package org.neo4j.gspatial.index.rtree.query;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.gspatial.constants.SpatialConstants;
import org.neo4j.gspatial.index.rtree.Envelope;
import org.neo4j.gspatial.index.rtree.EnvelopeDecoderFromBbox;
import org.neo4j.gspatial.index.rtree.JtsGeometryDecoderFromNode;
import org.neo4j.gspatial.index.rtree.RTreeRelationshipTypes;
import org.neo4j.gspatial.utils.RtreeUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QueryUtils {
    private static final String bbox = SpatialConstants.BBOX.getValue();
    private static final String geometryName = SpatialConstants.GEOMETRYNAME.getValue();
    public static final EnvelopeDecoderFromBbox bboxDecoder = new EnvelopeDecoderFromBbox(bbox);
    public static final JtsGeometryDecoderFromNode geometryDecoder = new JtsGeometryDecoderFromNode(geometryName);

    public static Envelope getIndexNodeEnvelope(Node indexNode) {
        if (!indexNode.hasProperty(bbox)) {
            return null;
        }
        return bboxDecoder.decodeEnvelope(indexNode);
    }

    public static Envelope getRelationEnvelope(Relationship leafRelation) {
        return bboxDecoder.decodeEnvelopeEdge(leafRelation);
    }

    public static class NodeWithEnvelope {
        public Envelope envelope;
        public Node node;

        public NodeWithEnvelope(Node node, Envelope envelope) {
            this.node = node;
            this.envelope = envelope;
        }

        public static class Pair {
            public NodeWithEnvelope nwe1;
            public NodeWithEnvelope nwe2;

            public Pair(NodeWithEnvelope nwe1, NodeWithEnvelope nwe2) {
                this.nwe1 = nwe1;
                this.nwe2 = nwe2;
            }
        }
    }

    public static class NodeWithGeometry {
        public Geometry geometry;
        public Node node;
        public Envelope envelope;

        public NodeWithGeometry(Node node, Geometry geometry, Envelope envelope) {
            this.node = node;
            this.geometry = geometry;
            this.envelope = envelope;
        }

        public static class Pair {
            public NodeWithGeometry nwg1;
            public NodeWithGeometry nwg2;

            public Pair(NodeWithGeometry nwg1, NodeWithGeometry nwg2) {
                this.nwg1 = nwg1;
                this.nwg2 = nwg2;
            }
        }
    }

    public static Geometry getGeometry(Node geomtryNode) {
        return geometryDecoder.decodeGeometry(geomtryNode);
    }

    public static List<NodeWithEnvelope> getIndexChildren(Node rootNode) {
        List<NodeWithEnvelope> result = new ArrayList<>();
        for (Relationship r : rootNode.getRelationships(Direction.OUTGOING, RTreeRelationshipTypes.RTREE_CHILD)) {
            Node child = r.getEndNode();
            result.add(new NodeWithEnvelope(child, getIndexNodeEnvelope(child)));
        }
        return result;
    }

    public static List<NodeWithEnvelope> getChildrenWithEnvelope(Node node) {
        List<NodeWithEnvelope> result = new ArrayList<>();
        Iterable<Relationship> relationships = node.getRelationships(Direction.OUTGOING);
        for (Relationship relationship : relationships) {
            if (isRelevantRTreeRelationship(relationship)) {
                Node child = relationship.getEndNode();
                result.add(new NodeWithEnvelope(child, getRelationEnvelope(relationship)));
            }
        }
        return result;
    }

    public static List<Node> getLeafChildren(Node LeafNode) {
        List<Node> result = new ArrayList<>();
        for (Relationship r : LeafNode.getRelationships(Direction.OUTGOING, RTreeRelationshipTypes.RTREE_REFERENCE)) {
            result.add(r.getEndNode());
        }
        return result;
    }

    public static List<NodeWithGeometry> getGeomtryNodes(Node LeafNode) {
        List<NodeWithGeometry> result = new ArrayList<>();
        for (Relationship r : LeafNode.getRelationships(Direction.OUTGOING, RTreeRelationshipTypes.RTREE_REFERENCE)) {
            Node node = r.getEndNode();
            result.add(new NodeWithGeometry(node, getGeometry(node), getRelationEnvelope(r)));
        }
        return result;
    }

    public static Node getIndexRoot(Node layerNode) {
        return layerNode.getSingleRelationship(RTreeRelationshipTypes.RTREE_ROOT, Direction.OUTGOING).getEndNode();
    }

    public static List<Node> getIndexRoot(List<Node> layers) {
        return layers.stream()
                .map(QueryUtils::getIndexRoot)
                .collect(Collectors.toList());
    }

    public static boolean nodeIsLeaf(Node node) {
        return !node.hasRelationship(Direction.OUTGOING, RTreeRelationshipTypes.RTREE_CHILD);
    }

    public static boolean isDirectoryRelationship(Relationship relationship) {
        return relationship.isType(RTreeRelationshipTypes.RTREE_ROOT) ||
                relationship.isType(RTreeRelationshipTypes.RTREE_CHILD);
    }

    public static boolean isRelevantRTreeRelationship(Relationship relationship) {
        return relationship.isType(RTreeRelationshipTypes.RTREE_ROOT) ||
                relationship.isType(RTreeRelationshipTypes.RTREE_CHILD) ||
                relationship.isType(RTreeRelationshipTypes.RTREE_REFERENCE);
    }

    public static List<Node> getGeometryNodesFromIndex(Node indexNode) {
        List<Node> result = new ArrayList<>();
        Iterable<Relationship> relationships = indexNode.getRelationships(Direction.OUTGOING);
        for (Relationship relationship : relationships) {
            if (relationship.isType(RTreeRelationshipTypes.RTREE_REFERENCE)) {
                Node child = relationship.getEndNode();
                result.add(child);
            } else if (relationship.isType(RTreeRelationshipTypes.RTREE_CHILD)) {
                result.addAll(getGeometryNodesFromIndex(relationship.getEndNode()));
            }
        }
        return result;
    }

    public static List<RtreeUtility.JoinOutput> getJoinOutputFromIndexPair(Node index1, Node index2) {
        List<RtreeUtility.JoinOutput> results = new ArrayList<>();
        List<Node> geomNodes1 = getGeometryNodesFromIndex(index1);
        List<Node> geomNodes2 = getGeometryNodesFromIndex(index2);
        for (Node geomNode1 : geomNodes1) {
            for (Node geomNode2 : geomNodes2) {
                results.add(new RtreeUtility.JoinOutput(geomNode1, geomNode2));
            }
        }
        return results;
    }
}
