package org.neo4j.gspatial;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.gspatial.utils.GeometryUtility;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class provides utility methods for executing and verifying spatial operations in tests.
 */
public class TestOperationUtility {
    /**
     * Enum representing various categories of spatial operations.
     * Each category is associated with a set of operations and a method for generating a Cypher query for the operations.
     */
    public enum OperationCategory {
        TOPOLOGY, SET, DUAL_ARG, PARAM;

        /**
         * Checks if the given operation belongs to this category.
         *
         * @param operation the name of the operation
         * @return true if the operation belongs to this category, false otherwise
         */
        public boolean containsOperation(String operation) {
            return switch (this) {
                case TOPOLOGY ->
                        Arrays.asList("contains", "covers", "covered_by", "crosses", "disjoint", "equals", "intersects", "overlaps", "touches", "within").contains(operation);
                case SET -> Arrays.asList("difference", "intersection", "union").contains(operation);
                case DUAL_ARG -> "distance".equals(operation);
                case PARAM -> "buffer".equals(operation);
            };
        }

        /**
         * Generates a Cypher query for the given operation and arguments.
         *
         * @param nodeType1 the type of the first node
         * @param nodeType2 the type of the second node or a parameter for the operation
         * @param operation the name of the operation
         * @return the generated Cypher query
         */
        public String generateQuery(String nodeType1, String nodeType2, String operation) {
            return switch (this) {
                case TOPOLOGY ->
                        buildOperationsQuery(nodeType1, nodeType2, operation, "n <> m", "n.idx, m.idx");
                case SET -> buildSetOperationQuery(nodeType1, nodeType2, operation);
                case DUAL_ARG -> buildDistanceOperationQuery(nodeType1, nodeType2, operation, "n <> m", "n.idx, m.idx, result");
                case PARAM -> buildParamOperationQuery(nodeType1, nodeType2, operation);
            };
        }

        /**
         * Builds a Cypher query for a spatial operations.
         *
         * @param nodeType1  the type of the first node
         * @param nodeType2  the type of the second node
         * @param operation  the name of the operation
         * @param conditions the conditions for the WHERE clause
         * @param returns    the expressions for the RETURN clause
         * @return the generated Cypher query
         */
        private String buildOperationsQuery(String nodeType1, String nodeType2, String operation, String conditions, String returns) {
            return String.format(
                    """
                           MATCH (n:%s)
                           MATCH (m:%s)

                           WITH COLLECT(n) as n_list, COLLECT(m) as m_list
                           CALL gspatial.operation('%s', [n_list, m_list]) YIELD result
                           
                           UNWIND result[1] AS idx
                           WITH n_list[idx] AS n, m_list[idx] AS m

                           WHERE %s
                           RETURN %s;
                           """,
                    nodeType1, nodeType2, operation, conditions, returns);
        }


        /**
         * Builds a Cypher query for a distance operation.
         *
         * @param nodeType1  the type of the first node
         * @param nodeType2  the type of the second node
         * @param operation  the name of the operation
         * @param conditions the conditions for the WHERE clause
         * @param returns    the expressions for the RETURN clause
         * @return the generated Cypher query
         */
        private String buildDistanceOperationQuery(String nodeType1, String nodeType2, String operation, String conditions, String returns) {
            return String.format(
                    """
                            MATCH (n:%s)
                            MATCH (m:%s)
                                                        
                            WITH n, m, collect(n) AS n_list, collect(m) AS m_list
                            CALL gspatial.operation('%s', [n_list, m_list]) YIELD result
                                                        
                            UNWIND result[0] AS results
                            WITH n, m, ROUND(results, 4) AS result
                            
                            WHERE %s
                            RETURN %s
                            """,
                    nodeType1, nodeType2, operation, conditions, returns);
        }

        /**
         * Builds a Cypher query for a set operation.
         *
         * @param nodeType1 the type of the first node
         * @param nodeType2 the type of the second node
         * @param operation the name of the set operation
         * @return the generated Cypher query
         */
        private String buildSetOperationQuery(String nodeType1, String nodeType2, String operation) {
            return String.format(
                    """
                            MATCH (n:%s)
                            MATCH (m:%s)
                            WITH COLLECT(n) AS n_list, COLLECT(m) AS m_list
                                                        
                            CALL gspatial.operation('%s', [n_list, m_list]) YIELD result
                            
                            UNWIND result AS res
                            WITH res[0] AS n, res[1] AS m, res[2] AS result
                                                        
                            RETURN n.idx, m.idx, result
                            """,
                    nodeType1, nodeType2, operation);
        }

        /**
         * Builds a Cypher query for a spatial operation that requires a parameter.
         *
         * @param nodeType1 the type of the node
         * @param nodeType2 the parameter for the operation
         * @param operation the name of the operation
         * @return the generated Cypher query
         * @throws IllegalArgumentException if nodeType2 cannot be converted to a Double
         */
        private String buildParamOperationQuery(String nodeType1, String nodeType2, String operation) {
            try {
                double param = Double.parseDouble(nodeType2);
                return String.format(
                        """
                                MATCH (n:%s)
                                CALL gspatial.operation('%s', [[n.geometry], [%s]]) YIELD result AS results
                                UNWIND results[0] AS result

                                RETURN n.idx, result;
                                """,
                        nodeType1, operation, param);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("nodeType2 must be convertible to Double for buffer operation");
            }
        }
    }

    /**
     * Executes the given spatial operation with the given arguments and verifies the results.
     *
     * @param driver      the Neo4j driver
     * @param inputArg1   the type of the first node or a WKT string
     * @param inputArg2   the type of the second node, a WKT string, or a parameter for the operation
     * @param operation   the name of the operation
     * @param singleInput true if the operation requires a single input, false otherwise
     */
    public static void executeOperation(Driver driver, String inputArg1, String inputArg2, String operation, boolean singleInput) {
        List<Map<String, Object>> results = singleInput
                ? executeSingleInputSpatialQuery(driver, inputArg1, operation)
                : executeDualInputSpatialQuery(driver, inputArg1, inputArg2, operation);

        CSVWriter csvWriter = new CSVWriter();
        csvWriter.write("neo_" + operation + ".csv", results);

        verifyResults(results, operation);
    }

    /**
     * Executes the provided Cypher query and returns the results as a list of maps.
     *
     * @param driver      the Neo4j driver
     * @param cypherQuery the Cypher query to be executed
     * @return a list of maps representing the results of the query
     */
    private static List<Map<String, Object>> executeQuery(Driver driver, String cypherQuery) {
        try (Session session = driver.session()) {
            return session.run(cypherQuery).list().stream()
                    .map(TestOperationUtility::recordToMap)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Executes a spatial operation that requires a single input and returns the results.
     *
     * @param driver    the Neo4j driver
     * @param nodeType  the type of the node
     * @param operation the name of the operation
     * @return a list of maps representing the results of the operation
     */
    private static List<Map<String, Object>> executeSingleInputSpatialQuery(Driver driver, String nodeType, String operation) {
        String cypherQuery = String.format("""
                MATCH (n:%s)
                WITH n, collect(n.geometry) AS geometries
                CALL gspatial.operation('%s', [geometries]) YIELD result AS results
                
                UNWIND results[0] AS result
                RETURN n.idx, result
                """,
                nodeType, operation);
        return executeQuery(driver, cypherQuery);
    }

    /**
     * Executes a spatial operation that requires two inputs and returns the results.
     *
     * @param driver    the Neo4j driver
     * @param nodeType1 the type of the first node
     * @param nodeType2 the type of the second node or a parameter for the operation
     * @param operation the name of the operation
     * @return a list of maps representing the results of the operation
     */
    private static List<Map<String, Object>> executeDualInputSpatialQuery(Driver driver, String nodeType1, String nodeType2, String operation) {
        OperationCategory category = determineOperationCategory(operation);
        String query = category.generateQuery(nodeType1, nodeType2, operation);
        return executeQuery(driver, query);
    }

    /**
     * Determines the category of the given operation.
     *
     * @param operation the name of the operation
     * @return the category of the operation
     * @throws IllegalArgumentException if the operation is not supported
     */
    private static OperationCategory determineOperationCategory(String operation) {
        for (OperationCategory category : OperationCategory.values()) {
            if (category.containsOperation(operation)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unsupported operation: " + operation);
    }

    /**
     * Converts a Record to a map.
     *
     * @param record the Record to convert
     * @return a map representing the Record
     */
    private static Map<String, Object> recordToMap(Record record) {
        return record.asMap();
    }

    /**
     * Verifies the results of a spatial operation against the expected results.
     *
     * @param results   the actual results of the operation
     * @param operation the name of the operation
     */
    private static void verifyResults(List<Map<String, Object>> results, String operation) {
        List<Map<String, Object>> expectedResults = TestIOUtility.loadDataForComparison("py_" + operation);
        sortAndNormalizeResults(results);
        sortAndNormalizeResults(expectedResults);
        double tolerance = 1e-6;
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> actual = results.get(i);
            Map<String, Object> expected = expectedResults.get(i);
            for (String key : expected.keySet()) {
                Object expectedValue = expected.get(key);
                Object actualValue = actual.get(key);
                if (expectedValue instanceof Geometry && actualValue instanceof Geometry) {
                    assertTrue(((Geometry) expectedValue).norm().equalsExact(((Geometry) actualValue).norm(), tolerance));
                } else {
                    assertEquals(expectedValue, actualValue);
                }
            }
        }
    }

    /**
     * Sorts and normalizes the results of a spatial operation.
     *
     * @param results the results of the operation
     */
    private static void sortAndNormalizeResults(List<Map<String, Object>> results) {
        results.sort(Comparator.comparing((Map<String, Object> map) -> (Long) map.getOrDefault("n.idx", Long.MIN_VALUE))
                .thenComparing(map -> (Long) map.getOrDefault("m.idx", Long.MIN_VALUE)));
        results.replaceAll(TestOperationUtility::normalizeResult);
    }

    /**
     * Normalizes a result of a spatial operation.
     *
     * @param result the result to normalize
     * @return a map representing the normalized result
     */
    private static Map<String, Object> normalizeResult(Map<String, Object> result) {
        Map<String, Object> normalizedResult = new HashMap<>(result);
        Object value = result.get("result");
        if (value instanceof Double) {
            double roundedValue = Math.round((Double) value * 10000.0) / 10000.0;
            normalizedResult.put("result", roundedValue);
        }
        if (value instanceof String) {
            String normalizedString = ((String) value).replace("\"", "");
            try {
                Geometry geometry = GeometryUtility.parseGeometry(normalizedString);
                normalizedResult.put("result", geometry);
            } catch (IllegalArgumentException e) {
                normalizedResult.put("result", normalizedString);
            }
        }
        return normalizedResult;
    }
}
