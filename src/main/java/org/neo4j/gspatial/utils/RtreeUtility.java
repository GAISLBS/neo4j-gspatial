package org.neo4j.gspatial.utils;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.gspatial.constants.RtreeQueryConstants;
import org.neo4j.gspatial.constants.SpatialConstants;
import org.neo4j.gspatial.index.rtree.RTreeRelationshipTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RtreeUtility {
    private static final Map<Label, Boolean> uniqueIdxCache = new HashMap<>();
    private static final String uuid = SpatialConstants.UUIDNAME.getValue();
    private static final String geometry = SpatialConstants.GEOMETRYNAME.getValue();

    public static List<Node> checkArgs(Transaction tx, List<Object> rawArgs) {
        List<Node> checkedArgs = new ArrayList<>();
        for (Object arg : rawArgs) {
            checkedArgs.add(checkArg(tx, arg));
        }
        return checkedArgs;
    }

    public static Node checkArg(Transaction tx, Object arg) throws IllegalArgumentException {
        Node node = assertIsNode(arg);
        assertPropertyExists(node, uuid);
        assertPropertyUnique(tx, node);
        assertPropertyExists(node, geometry);
        validateGeometry(node);
        return node;
    }

    private static Node assertIsNode(Object obj) throws IllegalArgumentException {
        if (!(obj instanceof Node)) {
            throw new IllegalArgumentException("Expected a Neo4j Node");
        }
        return (Node) obj;
    }

    private static void assertPropertyExists(Node node, String propertyName) throws IllegalArgumentException {
        if (!node.hasProperty(propertyName)) {
            throw new IllegalArgumentException(String.format("Node lacks required %s", propertyName));
        }
    }

    private static void assertPropertyUnique(Transaction tx, Node node) {
        if (!isPropertyUnique(tx, node)) {
            throw new IllegalArgumentException(String.format("Node with property '%s' must be unique", node.getProperty(uuid)));
        }
    }

    private static void validateGeometry(Node node) {
        Object geomValue = node.getProperty(geometry);
        if (!(geomValue instanceof String)) {
            throw new IllegalArgumentException("Geometry must be a WKT string");
        }
        GeometryUtility.parseGeometry((String) geomValue);
    }

    private static boolean isPropertyUnique(Transaction tx, Node node) {
        Label label = node.getLabels().iterator().next();
        return uniqueIdxCache.computeIfAbsent(label, lbl -> isUniqueConstraintPresent(tx, lbl));
    }

    private static boolean isUniqueConstraintPresent(Transaction tx, Label label) {
        Schema schema = tx.schema();
        for (ConstraintDefinition constraint : schema.getConstraints(label)) {
            if (constraint.isConstraintType(ConstraintType.UNIQUENESS) && constraint.getPropertyKeys().iterator().next().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public static Node getLayer(Transaction tx, String RtreeLabel) {
        String queryString = String.format("MATCH (layer:%s)-[:%s]->(root) RETURN layer", RtreeLabel, RTreeRelationshipTypes.RTREE_ROOT);
        Result result = tx.execute(queryString);
        return result.hasNext() ? (Node) result.next().get("layer") : null;
    }

    public static int[] getMetas(List<Node> layers, RtreeQueryConstants.RtreeQuery queryType) {
        int[] metas = new int[]{0, 0}; // 기본값을 0으로 초기화
        boolean isMultiplication = queryType == RtreeQueryConstants.RtreeQuery.JOIN;

        if (isMultiplication) {
            metas[0] = 1;
            metas[1] = 1;
        }

        for (Node layer : layers) {
            Iterable<Relationship> relationships = layer.getRelationships(Direction.OUTGOING, RTreeRelationshipTypes.RTREE_METADATA);
            for (Relationship relationship : relationships) {
                Node meta = relationship.getEndNode();
                int totalIndexCount = (int) meta.getProperty("totalIndexCount");
                int totalGeometryCount = (int) meta.getProperty("totalGeometryCount");

                if (isMultiplication) {
                    metas[0] *= totalIndexCount;
                    metas[1] *= totalGeometryCount;
                } else {
                    metas[0] += totalIndexCount;
                    metas[1] += totalGeometryCount;
                }
            }
        }
        return metas;
    }

    public static List<Node> copyNodes(Transaction tx, List<Node> nodes, String RtreeLabel) {
        List<Node> copiedNodes = new ArrayList<>();
        Label newLabel = Label.label(RtreeLabel);
        for (Node node : nodes) {
            Node newNode = tx.createNode(newLabel);
            for (String key : node.getPropertyKeys()) {
                newNode.setProperty(key, node.getProperty(key));
            }
            copiedNodes.add(newNode);
        }
        return copiedNodes;
    }

    public static class Output {
        public Object result;

        public Output(Object result) {
            this.result = result;
        }
    }

    public static class RangeOutput {
        public Node node;

        public RangeOutput(Node node) {
            this.node = node;
        }
    }

    public static class KnnOutput {
        public Node node;
        public double distance;

        public KnnOutput(Node node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        public double getDistance() {
            return distance;
        }

        public Node getNode() {
            return node;
        }
    }

    public static class JoinOutput {
        public Node node1;
        public Node node2;

        public JoinOutput(Node node1, Node node2) {
            this.node1 = node1;
            this.node2 = node2;
        }
    }
}
