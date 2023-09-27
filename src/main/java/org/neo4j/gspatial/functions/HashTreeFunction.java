package org.neo4j.gspatial.functions;

import org.neo4j.graphdb.*;
import org.neo4j.gspatial.utils.GeohashUtility;
import org.neo4j.gspatial.utils.GeometryUtility;

import java.util.*;


public class HashTreeFunction {

    private static final String HASH_TREE_LABEL = "HashTree";
    private static final String GEOHASH_PROPERTY = "geohash";
    private static final String INDEX_OF_REL_TYPE = "INDEX_OF";
    private static final String CHILD_OF_REL_TYPE = "CHILD_OF";
    private static final String GEOMETRY_PROPERTY = "geometry";
    private static final int MAX_GEOHASH_LENGTH = 12;
    private final Transaction tx;

    public HashTreeFunction(Transaction tx) {
        this.tx = tx;
    }

    public Node setHashTree(String label, long geometryIdx, String wkt) {
        Node geometryNode = findGeometryNode(label, geometryIdx);
        String geohash = GeohashUtility.generateGeohash(wkt);
        Node hashNode = findOrCreateHashNode(geohash);
        createOrSkipRelationship(geometryNode, hashNode, INDEX_OF_REL_TYPE);
        manageChildOfRelationships(hashNode, geohash);
        return hashNode;
    }

    private Node findGeometryNode(String label, long idx) {
        Node node = tx.findNode(Label.label(label), "idx", idx);
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        return node;
    }

    private Node findOrCreateHashNode(String geohash) {
        try (ResourceIterator<Node> nodes = tx.findNodes(Label.label(HASH_TREE_LABEL), GEOHASH_PROPERTY, geohash)) {
            return nodes.hasNext() ? nodes.next() : createHashNode(geohash);
        }
    }

    private Node createHashNode(String geohash) {
        Node hashNode = tx.createNode(Label.label(HASH_TREE_LABEL));
        hashNode.setProperty(GEOHASH_PROPERTY, geohash);
        return hashNode;
    }

    private void createOrSkipRelationship(Node fromNode, Node toNode, String relType) {
        if (!relationshipExists(fromNode, toNode, relType)) {
            fromNode.createRelationshipTo(toNode, RelationshipType.withName(relType));
        }
    }

    private static boolean relationshipExists(Node fromNode, Node toNode, String relType) {
        for (Relationship r : fromNode.getRelationships(RelationshipType.withName(relType))) {
            if (r.getOtherNode(fromNode).equals(toNode)) {
                return true;
            }
        }
        return false;
    }

    private void manageChildOfRelationships(Node hashNode, String geohash) {
        manageAncestorRelationship(hashNode, geohash);
        manageDescendantRelationship(hashNode, geohash);
    }

    private void manageAncestorRelationship(Node hashNode, String geohash) {
        String ancestorGeohash = geohash.substring(0, geohash.length() - 1);
        while (!ancestorGeohash.isEmpty()) {
            Node ancestorNode = findOrCreateHashNode(ancestorGeohash);
            if (ancestorNode != null) {
                createOrSkipRelationship(ancestorNode, hashNode, CHILD_OF_REL_TYPE);
            }
            hashNode = ancestorNode;
            ancestorGeohash = ancestorGeohash.substring(0, ancestorGeohash.length() - 1);
        }
    }

    private void manageDescendantRelationship(Node hashNode, String geohash) {
        String closestDescendantGeohash = findClosestDescendantGeohash(geohash);
        if (closestDescendantGeohash != null) {
            Node closestDescendantNode = findOrCreateHashNode(closestDescendantGeohash);
            createOrSkipRelationship(hashNode, closestDescendantNode, CHILD_OF_REL_TYPE);
        }
    }

    private String findClosestDescendantGeohash(String parentGeohash) {
        if (parentGeohash.length() >= MAX_GEOHASH_LENGTH) {
            return null;
        }
        try (ResourceIterator<Node> nodes = tx.findNodes(Label.label(HASH_TREE_LABEL))) {
            while (nodes.hasNext()) {
                Node node = nodes.next();
                String nodeGeohash = (String) node.getProperty(GEOHASH_PROPERTY);
                if (nodeGeohash.startsWith(parentGeohash) && nodeGeohash.length() == parentGeohash.length() + 1) {
                    return nodeGeohash;
                }
            }
        }
        return null;
    }

    public static List<Object> findHashRelation(List<Object> args, String operationName) {
        if (args.size() < 2) {
            throw new IllegalArgumentException("At least two nodes are required for relationship checks.");
        }

        Node node1 = (Node) args.get(0);
        Node node2 = (Node) args.get(1);

        args.set(0, GeometryUtility.parseWKT((String) node1.getProperty(GEOMETRY_PROPERTY)));
        args.set(1, GeometryUtility.parseWKT((String) node2.getProperty(GEOMETRY_PROPERTY)));

        Node geohashNode1 = findGeohashNode(node1);
        Node geohashNode2 = findGeohashNode(node2);

        if (geohashNode1 == null || geohashNode2 == null) {
            return args;
        }
        boolean isRelated = checkGeoHashRelation(geohashNode1, geohashNode2, operationName.toUpperCase());
        if (!isRelated) {
            args.add(2, false);
        }
        return args;
    }

    private static boolean checkGeoHashRelation(Node node1, Node node2, String operationName) {
        return switch (operationName) {
            case "EQUALS" -> node1.equals(node2);
            case "CONTAINS", "COVERS" -> node1.equals(node2) || checkDescendant(node1, node2);
            case "WITHIN", "COVERED_BY" -> node1.equals(node2) || checkDescendant(node2, node1);
            case "CROSSES", "OVERLAPS", "TOUCHES", "INTERSECTS", "DISJOINT" ->
                    node1.equals(node2) || checkNear(node1, node2) || checkDescendant(node1, node2) || checkDescendant(node2, node1);
            default -> throw new IllegalArgumentException("Invalid operation name");
        };
    }

    private static boolean checkNear(Node geohashNode1, Node geohashNode2) {
        String geohash1 = (String) geohashNode1.getProperty(GEOHASH_PROPERTY);
        String geohash2 = (String) geohashNode2.getProperty(GEOHASH_PROPERTY);
        if (geohash1.length() != geohash2.length()) {
            return false;
        }
        String truncatedGeohash1 = geohash1.substring(0, geohash1.length() - 1);
        String truncatedGeohash2 = geohash2.substring(0, geohash2.length() - 1);
        if (!truncatedGeohash1.equals(truncatedGeohash2)) {
            return false;
        }
        String[] possibleLastCharacters = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "b", "c", "d", "e", "f", "g", "h", "j", "k", "m", "n", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
        String lastCharOfGeohash2 = geohash2.substring(geohash2.length() - 1);
        for (String lastChar : possibleLastCharacters) {
            if (lastChar.equals(lastCharOfGeohash2)) {
                return true;
            }
        }
        return false;
    }

    private static Node findGeohashNode(Node node) {
        for (Relationship r : node.getRelationships(RelationshipType.withName(INDEX_OF_REL_TYPE))) {
            return r.getOtherNode(node);
        }
        return null;
    }

    private static boolean checkDescendant(Node parentNode, Node childNode) {
        Set<Node> visitedNodes = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();
        queue.add(parentNode);
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (!visitedNodes.add(current)) {
                continue;
            }
            for (Relationship r : current.getRelationships(RelationshipType.withName(CHILD_OF_REL_TYPE))) {
                Node possibleChild = r.getOtherNode(current);
                if (possibleChild.equals(childNode)) {
                    return true;
                }
                queue.add(possibleChild);
            }
        }
        return false;
    }
}
