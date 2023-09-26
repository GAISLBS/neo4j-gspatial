package org.neo4j.gspatial.functions;

import org.neo4j.graphdb.*;
import org.neo4j.gspatial.utils.GeohashUtility;
import org.neo4j.gspatial.utils.GeometryUtility;

import java.util.Arrays;
import java.util.List;

public class HashTreeFunction {

    private static final String HASH_TREE_LABEL = "HashTree";
    private static final String GEOHASH_PROPERTY = "geohash";
    private static final String INDEX_OF_REL_TYPE = "INDEX_OF";
    private static final String CHILD_OF_REL_TYPE = "CHILD_OF";
    private static final int MAX_GEOHASH_LENGTH = 12;
    private final Transaction tx;

    public HashTreeFunction(Transaction tx) {
        this.tx = tx;
    }

    public Node setHashTree(String label, long geometryIdx, String wkt) {
        Node geometryNode = findGeometryNode(label, geometryIdx);
        if (geometryNode == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        String geohash = GeohashUtility.generateGeohash(wkt);
        Node hashNode = findOrCreateHashNode(geohash);
        createOrSkipRelationship(geometryNode, hashNode, INDEX_OF_REL_TYPE);
        manageChildOfRelationships(hashNode, geohash);

        return hashNode;
    }

    private Node findGeometryNode(String label, long geometryIdx) {
        return tx.findNode(Label.label(label), "idx", geometryIdx);
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
                break;
            }
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
        List<String> downRelOperations = Arrays.asList("CONTAINS", "COVERS");

        Node node1 = (Node) args.get(0);
        Node node2 = (Node) args.get(1);

        args.set(0, GeometryUtility.parseWKT((String) node1.getProperty("geometry")));
        args.set(1, GeometryUtility.parseWKT((String)node2.getProperty("geometry")));

        Node geohashNode1 = findGeohashNode(node1);
        Node geohashNode2 = findGeohashNode(node2);

        if (geohashNode1 == null || geohashNode2 == null) {
            return args;
        }
        boolean isRelated;
        if (geohashNode1.equals(geohashNode2)) {
            isRelated = true;
        } else {
            if (operationName.equals("EQUALS")){
                isRelated = false;
            }



            else if (downRelOperations.contains(operationName)) {
                isRelated = checkChildOfRelationship(geohashNode1, geohashNode2);
            } else {
                isRelated = checkChildOfRelationship(geohashNode2, geohashNode1);
            }
        }
        if (!isRelated) {
            args.add(2, false);
        }
        return args;
    }

    private static boolean checkNear(Node geohashNode1, Node geohashNode2){
        return false;
    }

    private static Node findGeohashNode(Node node) {
        for (Relationship r : node.getRelationships(RelationshipType.withName("INDEX_OF"))) {
            return r.getOtherNode(node);
        }
        return null;
    }

    private static boolean checkChildOfRelationship(Node parentNode, Node childNode) {
        return relationshipExists(parentNode, childNode, CHILD_OF_REL_TYPE);
    }
}
