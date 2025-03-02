package org.neo4j.gspatial;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.types.MapAccessor;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class RTreeTestUtils {
    private static final String MATCH_QUERY = "MATCH (n:%s) WITH collect(n) as nodes";
    private static final String MATCH_WHERE_QUERY = "MATCH (n:%s) WHERE %s WITH collect(n) as nodes";
    private static final String DELETE_QUERY = "CALL gspatial.rtree(\"delete\", %s) YIELD result";
    private static final String INSERT_QUERY = "CALL gspatial.rtree(\"insert\", nodes, \"%s\") YIELD result";
    private static final String STRTREE_INSERT_QUERY = "CALL gspatial.strtree(\"insert\", nodes, \"%s\") YIELD result";
    private static final String STRTREE_RANGE_QUERY_BOX = "CALL gspatial.strtree.query.range([\"%s\"], [%f, %f, %f, %f])\n";
    private static final String STRTREE_RANGE_QUERY = "CALL gspatial.strtree.query.range([\"%s\"], [[%f, %f], %f])\n";
    private static final String RETURN_RESULT = "RETURN result";
    private static final String RANGE_QUERY_BOX = "CALL gspatial.rtree.query.range([\"%s\"], [%f, %f, %f, %f])\n";
    private static final String RANGE_QUERY = "CALL gspatial.rtree.query.range([\"%s\"], [[%f, %f], %f])\n";
    private static final String JOIN_QUERY = "CALL gspatial.rtree.query.join([\"%s\", \"%s\"], [\"%s\"])\n";
    private static final String KNN_QUERY = "CALL gspatial.rtree.query.knn([\"%s\"], [[%f, %f], %d])\n";
    private static final String JOIN_YIELD = "YIELD node1, node2\n";
    private static final String RANGE_YIELD = "YIELD node\n";
    private static final String KNN_YIELD = "YIELD node, distance\n";
    private static final String JOIN_RESULT = " RETURN node1.idx as n_idx, node2.idx as m_idx";
    private static final String RANGE_RESULT = " RETURN node.idx as n_idx ";
    private static final String KNN_RESULT =
            """
                    RETURN node.idx as n_idx, distance
                    ORDER BY distance ASC
                    """;
    private static final String JOIN_TRAVERSE_RESULT = "RETURN DISTINCT node1.idx as n_idx, node2.idx as m_idx";
    private static final String TRAVERSE_MATCH_QUERY =
            """
                    MATCH (%s)-[:HAS_COLOUR]->(c:Colour)
                      WHERE c.name = 'beige'
                    """;
    private static final String DISCONNECT_QUERY =
            """
                    MATCH (%s:%s) WHERE %s.idx = %s.idx
                    WITH %s
                    """;


    public static List<Map<String, Object>> executeQuery(Driver driver, String cypherQuery) {
        try (Session session = driver.session()) {
            List<Map<String, Object>> result = session.run(cypherQuery).list().stream().map(MapAccessor::asMap).collect(Collectors.toList());
            return result;
        }
    }

    public static void executeDeleteN(Driver driver, String label, String condition) {
        String createQuery = String.format(MATCH_QUERY, label) + " " + INSERT_QUERY;
        String matchQuery = String.format(MATCH_WHERE_QUERY, label, condition);
        String deleteQuery = String.format(DELETE_QUERY, "nodes");
        executeQuery(driver, createQuery + " " + matchQuery + " " + deleteQuery + " " + RETURN_RESULT);
    }

    public static List<Map<String, Object>> executeDeleteAll(Driver driver, String label) {
        String createQuery = String.format(MATCH_QUERY, label) + " " + INSERT_QUERY;
        String matchQuery = String.format(MATCH_QUERY, label);
        String deleteQuery = String.format(DELETE_QUERY, "[]");
        return executeQuery(driver, createQuery + " as rootNode " + matchQuery + " " + deleteQuery + " " + RETURN_RESULT);
    }

    public static void executeInsert(Driver driver, String... labels) {
        for (String label : labels) {
            String matchQuery = String.format(MATCH_QUERY, label);
            String insertQuery = String.format(INSERT_QUERY, label);
            executeQuery(driver, matchQuery + " " + insertQuery + " " + RETURN_RESULT);
        }
    }

    public static void executeStrTreeInsert(Driver driver, String... labels) {
        for (String label : labels) {
            String matchQuery = String.format(MATCH_QUERY, label);
            String insertQuery = String.format(STRTREE_INSERT_QUERY, label);
            executeQuery(driver, matchQuery + " " + insertQuery + " " + RETURN_RESULT);
        }
    }

    public static void executeStrTreeRangeQuery(Driver driver, String[] labels, double[] bbox) {
        String labelsString = String.join("\", \"", labels);
        String query = String.format(STRTREE_RANGE_QUERY_BOX, labelsString, bbox[0], bbox[1], bbox[2], bbox[3]);
        List<Map<String, Object>> results = executeQuery(driver, query + " " + RANGE_RESULT);
        verifyResults(results, "range_query_", String.join("_", labels), "Range");
    }

    public static void executeStrTreeRangeQuery(Driver driver, String[] labels, double[] point, double distance) {
        String labelsString = String.join("\", \"", labels);
        String query = String.format(STRTREE_RANGE_QUERY, labelsString, point[0], point[1], distance);
        String distanceFormatted = String.format(MessageFormat.format("%.{0}f", getDecimalPlaces(distance)), distance);
        List<Map<String, Object>> results = executeQuery(driver, query + " " + RANGE_RESULT);
        verifyResults(results, String.format("range_query_buffer_%s_", distanceFormatted), String.join("_", labels), "Range");
    }

    public static void executeStrTreeRangeTraverseQuery(Driver driver, String[] labels, double[] point, double distance) {
        String labelsString = String.join("\", \"", labels);
        String query = String.format(STRTREE_RANGE_QUERY, labelsString, point[0], point[1], distance);
        String traverse_query = String.format(TRAVERSE_MATCH_QUERY, "node");
        List<Map<String, Object>> results = executeQuery(driver, query + RANGE_YIELD + traverse_query + RANGE_RESULT);
    }

    public static void executeDeleteALeafNode(Driver driver, String label) {
        String query = String.format("MATCH(a:%s)" +
                "WHERE a.idx = 62\n" +
                "    OR a.idx = 58\n" +
                "    OR a.idx = 47\n" +
                "    OR a.idx = 20\n" +
                "    OR a.idx = 14\n" +
                "    OR a.idx = 7\n" +
                "    OR a.idx = 2\n" +
                "WITH COLLECT(a) AS nodes\n" +
                "CALL gspatial.rtree('delete', nodes, '%s') YIELD result", label, label);
        executeQuery(driver, query + " " + RETURN_RESULT);
    }

    public static void verifyRootBoundingBoxValidity(Driver driver, String label) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        Map<String, Object> root = null;

        String query = String.format("MATCH(a:%sRTree)<-[:RTREE_ROOT]-()\n" + "RETURN a", label);
        List<Map<String, Object>> rootResult = executeQuery(driver, query);
        if (rootResult.size() != 1) {
            fail("There should be only one root node");
        }
        root = rootResult.get(0);

        String query2 = String.format("MATCH ()-[:RTREE_ROOT]->(a:%sRTree)-[:RTREE_CHILD]->(b)\n" +
                "RETURN b", label);
        List<Map<String, Object>> childResult = executeQuery(driver, query2);

        for (Map<String, Object> node : childResult) {
            InternalNode b = (InternalNode) node.get("b");
            Map<String, Object> properties = b.asMap();
            List<Double> bbox = (List<Double>) properties.get("bbox");

            double minX2 = bbox.get(0);
            double minY2 = bbox.get(1);
            double maxX2 = bbox.get(2);
            double maxY2 = bbox.get(3);

            if (minX2 < minX) {
                minX = minX2;
            }
            if (minY2 < minY) {
                minY = minY2;
            }
            if (maxX2 > maxX) {
                maxX = maxX2;
            }
            if (maxY2 > maxY) {
                maxY = maxY2;
            }
        }

        InternalNode a = (InternalNode) root.get("a");
        Map<String, Object> properties = a.asMap();
        List<Double> bbox = (List<Double>) properties.get("bbox");

        double rootMinX = bbox.get(0);
        double rootMinY = bbox.get(1);
        double rootMaxX = bbox.get(2);
        double rootMaxY = bbox.get(3);
        if (minX < rootMinX || minY < rootMinY || maxX > rootMaxX || maxY > rootMaxY) {
            fail("Root's bbox is invalid");
        }
    }

    public static void executeRangeQuery(Driver driver, String[] labels, double[] bbox) {
        String labelsString = String.join("\", \"", labels);
        String query = String.format(RANGE_QUERY_BOX, labelsString, bbox[0], bbox[1], bbox[2], bbox[3]);
        List<Map<String, Object>> results = executeQuery(driver, query + " " + RANGE_YIELD + RANGE_RESULT);
//        verifyResults(results, "range_query_", String.join("_", labels), "Range");
    }

    public static void executeRangeQuery(Driver driver, String[] labels, double[] point, double distance) {
        String labelsString = String.join("\", \"", labels);
        String query = String.format(RANGE_QUERY, labelsString, point[0], point[1], distance);
//        String distanceFormatted = String.format(MessageFormat.format("%.{0}f", getDecimalPlaces(distance)), distance);
        List<Map<String, Object>> results = executeQuery(driver, query + " " + RANGE_YIELD + RANGE_RESULT);
//        verifyResults(results, String.format("range_query_buffer_%s_", distanceFormatted), String.join("_", labels), "Range");
    }

    public static void executeJoinQuery(Driver driver, String label1, String label2, String operationName) {
        String query = String.format(JOIN_QUERY, label1, label2, operationName);
        List<Map<String, Object>> results = executeQuery(driver, query + " " + JOIN_YIELD + JOIN_RESULT);
//        verifyResults(results, "join_query_" + operationName + "_", label1 + "_" + label2, "Join");
    }

    public static void executeKnnQuery(Driver driver, String[] labels, double[] point, int k) {
        String labelsString = String.join("\", \"", labels);
        String query = String.format(KNN_QUERY, labelsString, point[0], point[1], k);
        List<Map<String, Object>> results = executeQuery(driver, query + " " + KNN_YIELD + KNN_RESULT);
//        verifyResults(results, String.format("knn_query_%d_", k), String.join("_", labels), "knn");
    }

    public static void executeRangeTraverseQuery(Driver driver, String[] labels, double[] point, double distance) {
        String labelsString = String.join("\", \"", labels);
        String query = String.format(RANGE_QUERY, labelsString, point[0], point[1], distance);
        String traverse_query = String.format(TRAVERSE_MATCH_QUERY, "node");
        List<Map<String, Object>> results = executeQuery(driver, query + " " + RANGE_YIELD + traverse_query + RANGE_RESULT);
    }

    public static void executeJoinTraverseQuery(Driver driver, String label1, String label2, String operationName) {
        String query = String.format(JOIN_QUERY, label1, label2, operationName);
        String traverse_query = String.format(TRAVERSE_MATCH_QUERY, "node2");
        List<Map<String, Object>> results = executeQuery(driver, query + " " + JOIN_YIELD + traverse_query + JOIN_TRAVERSE_RESULT);
    }

    public static void executeKnnTraverseQuery(Driver driver, String[] labels, double[] point, int k) {
        String labelsString = String.join("\", \"", labels);
        String query = String.format(KNN_QUERY, labelsString, point[0], point[1], k);
        String traverse_query = String.format(TRAVERSE_MATCH_QUERY, "node");
        List<Map<String, Object>> results = executeQuery(driver, query + " " + KNN_YIELD + traverse_query + KNN_RESULT);
    }

    public static void executeRangeTraverseDisconnectQuery(Driver driver, String[] labels, double[] point, double distance) {
        String labelsString = String.join("\", \"", labels);
        String query = String.format(RANGE_QUERY, labelsString, point[0], point[1], distance);
        String disconnect_query = String.format(DISCONNECT_QUERY, "n", "Building", "n", "node", " DISTINCT n");
        String traverse_query = String.format(TRAVERSE_MATCH_QUERY, "n");
        List<Map<String, Object>> results = executeQuery(driver, query + " " + RANGE_YIELD + disconnect_query + traverse_query + RANGE_RESULT);
    }

    public static void executeJoinTraverseDisconnectQuery(Driver driver, String label1, String label2, String operationName) {
        String query = String.format(JOIN_QUERY, label1, label2, operationName);
        String disconnect_query = String.format(DISCONNECT_QUERY, "m", "Building", "m", "node2", "node1, m");
        String traverse_query = String.format(TRAVERSE_MATCH_QUERY, "m");
        List<Map<String, Object>> results = executeQuery(driver, query + " " + JOIN_YIELD + disconnect_query + traverse_query + JOIN_TRAVERSE_RESULT);
    }

    public static void executeKnnTraverseDisconnectQuery(Driver driver, String[] labels, double[] point, int k) {
        String labelsString = String.join("\", \"", labels);
        String query = String.format(KNN_QUERY, labelsString, point[0], point[1], k);
        String disconnect_query = String.format(DISCONNECT_QUERY, "n", "Building", "n", "node", "n, distance");
        String traverse_query = String.format(TRAVERSE_MATCH_QUERY, "n");
        List<Map<String, Object>> results = executeQuery(driver, query + " " + KNN_YIELD + disconnect_query + traverse_query + KNN_RESULT);
    }

    private static int getDecimalPlaces(double value) {
        String text = Double.toString(value);
        int index = text.indexOf(".");
        if (index < 0) {
            return 0;
        } else {
            return text.length() - index - 1;
        }
    }

    public static void verifyResults(List<Map<String, Object>> results, String operation, String labels, String queryType) {
        List<Map<String, Object>> expectedResults = TestIOUtility.loadDataForComparison(operation + labels);
        assertEquals(expectedResults.size(), results.size());
        if ("Knn".equalsIgnoreCase(queryType)) {
            sortAndOptionallyNormalizeResults(results, "distance", true);
            sortAndOptionallyNormalizeResults(expectedResults, "distance", true);
        } else {
            sortAndOptionallyNormalizeResults(results, "n_idx", false);
            sortAndOptionallyNormalizeResults(expectedResults, "n_idx", false);
        }
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

    private static void sortAndOptionallyNormalizeResults(List<Map<String, Object>> results, String sortKey, boolean normalize) {
        if ("distance".equals(sortKey)) {
            results.sort(Comparator.comparing((Map<String, Object> map) -> (Double) map.getOrDefault(sortKey, Double.MIN_VALUE))
                    .thenComparing(map -> (Long) map.getOrDefault("n_idx", Long.MIN_VALUE)));
        } else {
            results.sort(Comparator.comparing((Map<String, Object> map) -> (Long) map.getOrDefault(sortKey, Long.MIN_VALUE))
                    .thenComparing(map -> (Long) map.getOrDefault("m_idx", Long.MIN_VALUE)));
        }
        if (normalize) {
            results.replaceAll(RTreeTestUtils::normalizeResult);
        }
    }

    /**
     * Normalizes a result of a spatial operation.
     *
     * @param result the result to normalize
     * @return a map representing the normalized result
     */
    private static Map<String, Object> normalizeResult(Map<String, Object> result) {
        Map<String, Object> normalizedResult = new HashMap<>(result);
        Object value = result.get("distance");
        if (value instanceof Double) {
            double roundedValue = Math.round((Double) value * 1000000.0) / 1000000.0;
            normalizedResult.put("distance", roundedValue);
        }
        return normalizedResult;
    }
}
