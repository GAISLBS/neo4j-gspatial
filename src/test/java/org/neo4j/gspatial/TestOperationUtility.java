package org.neo4j.gspatial;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.gspatial.utils.GeometryUtility;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestOperationUtility {

    public enum OperationCategory {
        TOPOLOGY, SET, DUAL_ARG, PARAM;

        public boolean containsOperation(String operation) {
            return switch (this) {
                case TOPOLOGY ->
                        Arrays.asList("contains", "covers", "covered_by", "crosses", "disjoint", "equals", "intersects", "overlaps", "touches", "within").contains(operation);
                case SET -> Arrays.asList("difference", "intersection", "union").contains(operation);
                case DUAL_ARG -> "distance".equals(operation);
                case PARAM -> "buffer".equals(operation);
            };
        }

        public String generateQuery(String nodeType1, String nodeType2, String operation) {
            return switch (this) {
                case TOPOLOGY ->
                        buildOperationQuery(nodeType1, nodeType2, operation, "n <> m AND result = true", "n.idx, m.idx");
                case SET -> buildSetOperationQuery(nodeType1, nodeType2, operation);
                case DUAL_ARG -> buildOperationQuery(nodeType1, nodeType2, operation, "n <> m", "n.idx, m.idx, result");
                case PARAM -> buildParamOperationQuery(nodeType1, nodeType2, operation);
            };
        }

        private String buildOperationQuery(String nodeType1, String nodeType2, String operation, String conditions, String returns) {
            return String.format(
                    """
                            MATCH (n:%s)
                            MATCH (m:%s)
                            CALL gspatial.operation('%s', [n, m]) YIELD result
                            WITH n, m, result
                            WHERE %s
                            RETURN %s
                            """,
                    nodeType1, nodeType2, operation, conditions, returns);
        }

        private String buildSetOperationQuery(String nodeType1, String nodeType2, String operation) {
            return String.format(
                    """
                                    MATCH (n:%s)
                                    MATCH (m:%s)
                                    CALL gspatial.operation('intersects', [n.geometry, m.geometry]) YIELD result as intersects_filter
                                    WITH n, m, intersects_filter
                                    WHERE n <> m and intersects_filter = true
                                    CALL gspatial.operation('%s', [n.geometry, m.geometry]) YIELD result
                                    RETURN n.idx, m.idx, result
                            """,
                    nodeType1, nodeType2, operation);
        }

        private String buildParamOperationQuery(String nodeType1, String nodeType2, String operation) {
            try {
                double param = Double.parseDouble(nodeType2);
                return String.format(
                        """
                                MATCH (n:%s)
                                CALL gspatial.operation('%s', [n.geometry, %s]) YIELD result
                                RETURN n.idx, result
                                """,
                        nodeType1, operation, param);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("nodeType2 must be convertible to Double for buffer operation");
            }
        }
    }

    public static void executeOperation(Driver driver, String inputArg1, String inputArg2, String operation, boolean singleInput) {
        List<Map<String, Object>> results = singleInput
                ? executeSingleInputSpatialQuery(driver, inputArg1, operation)
                : executeDualInputSpatialQuery(driver, inputArg1, inputArg2, operation);

//        CSVWriter csvWriter = new CSVWriter();
//        csvWriter.write("neo_" + operation + ".csv", results);

        verifyResults(results, operation);
    }

    private static List<Map<String, Object>> executeQuery(Driver driver, String cypherQuery) {
        try (Session session = driver.session()) {
            return session.run(cypherQuery).list().stream()
                    .map(TestOperationUtility::recordToMap)
                    .collect(Collectors.toList());
        }
    }

    private static List<Map<String, Object>> executeSingleInputSpatialQuery(Driver driver, String nodeType, String operation) {
        String cypherQuery = String.format("MATCH (n:%s) CALL gspatial.operation('%s', [n.geometry]) YIELD result RETURN n.idx, result", nodeType, operation);
        return executeQuery(driver, cypherQuery);
    }

    private static List<Map<String, Object>> executeDualInputSpatialQuery(Driver driver, String nodeType1, String nodeType2, String operation) {
        OperationCategory category = determineOperationCategory(operation);
        String query = category.generateQuery(nodeType1, nodeType2, operation);
        return executeQuery(driver, query);
    }

    private static OperationCategory determineOperationCategory(String operation) {
        for (OperationCategory category : OperationCategory.values()) {
            if (category.containsOperation(operation)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unsupported operation: " + operation);
    }

    private static Map<String, Object> recordToMap(Record record) {
        return record.asMap();
    }

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

    private static void sortAndNormalizeResults(List<Map<String, Object>> results) {
        results.sort(Comparator.comparing((Map<String, Object> map) -> (Long) map.getOrDefault("n.idx", Long.MIN_VALUE))
                .thenComparing(map -> (Long) map.getOrDefault("m.idx", Long.MIN_VALUE)));
        results.replaceAll(TestOperationUtility::normalizeResult);
    }

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
                Geometry geometry = GeometryUtility.parseWKT(normalizedString);
                normalizedResult.put("result", geometry);
            } catch (IllegalArgumentException e) {
                normalizedResult.put("result", normalizedString);
            }
        }
        return normalizedResult;
    }

    public static void executeHashTreeTest(Driver driver, String label) {
        try (Session session = driver.session()) {
            List<Map<String, Object>> dataList = fetchData(session, label);
            setHashTrees(session, dataList);
            verifyHashTrees(session, dataList);
        }
    }

    public static void executeHashTree(Driver driver, String label) {
        try (Session session = driver.session()) {
            List<Map<String, Object>> dataList = fetchData(session, label);
            setHashTrees(session, dataList);
        }
    }

    private static List<Map<String, Object>> fetchData(Session session, String label) {
        String fetchDataQuery = String.format("MATCH (n:%s) RETURN LABELS(n) as label, n.idx as geometryIdx, n.geometry as geom", label);
        List<Record> records = session.run(fetchDataQuery).list();

        return records.stream()
                .map(TestOperationUtility::convertRecordToMap)
                .toList();
    }

    private static Map<String, Object> convertRecordToMap(Record record) {
        Map<String, Object> map = new HashMap<>();
        map.put("label", record.get("label").asList().stream().map(Object::toString).collect(Collectors.joining(",")));
        map.put("geometryIdx", record.get("geometryIdx").asLong());
        map.put("geom", record.get("geom").asString());
        return map;
    }

    private static void setHashTrees(Session session, List<Map<String, Object>> dataList) {
        String setHashTreeQuery = "CALL gspatial.setHashTree($dataList) YIELD result RETURN result";
        session.run(setHashTreeQuery, Map.of("dataList", dataList));
    }

    private static void verifyHashTrees(Session session, List<Map<String, Object>> dataList) {
        for (Map<String, Object> data : dataList) {
            String label = data.get("label").toString();
            long geometryNodeId = (long) data.get("geometryIdx");
            String wkt = data.get("geom").toString();

            verifyHashTree(session, label, geometryNodeId, wkt);
        }
    }

    private static void verifyHashTree(Session session, String label, long geometryNodeId, String wkt) {
        String checkRelationQuery = String.format("MATCH (g:%s)-[:INDEX_OF]->(h:HashTree) WHERE g.idx = $geometryNodeId RETURN h.geohash as geohash", label);
        List<Record> records = session.run(checkRelationQuery, Map.of("geometryNodeId", geometryNodeId)).list();

        if (records.isEmpty()) {
            fail("Relationship between Hash node and Geometry node not found.");
        }

        String geohash = records.get(0).get("geohash").asString();
        assertNotNull(geohash, "Geohash should not be null for WKT: " + wkt);
    }
}
