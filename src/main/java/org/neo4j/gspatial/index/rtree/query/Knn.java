package org.neo4j.gspatial.index.rtree.query;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.constants.SpatialConstants;
import org.neo4j.gspatial.index.rtree.JtsGeometryDecoderFromNode;
import org.neo4j.gspatial.index.rtree.KnnVisitor;
import org.neo4j.gspatial.index.rtree.ProgressLoggingListener;
import org.neo4j.gspatial.index.rtree.RTreeRelationshipTypes;
import org.neo4j.gspatial.utils.RtreeUtility.KnnOutput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Stream;

import static org.neo4j.gspatial.index.rtree.query.QueryUtils.getIndexRoot;

public class Knn {
    private final Transaction tx;
    private final Geometry queryPoint;
    private final int k;
    private final PriorityQueue<KnnOutput> nearestNodes;
    private final List<Node> layers;
    private final ProgressLoggingListener progressListener;
    private final JtsGeometryDecoderFromNode geometryDecoder;

    public Knn(Transaction tx, List<Node> layers, List<Object> args, ProgressLoggingListener progressListener) {
        this.tx = tx;
        this.layers = layers;
        this.geometryDecoder = new JtsGeometryDecoderFromNode(SpatialConstants.GEOMETRYNAME.getValue());
        this.queryPoint = decodeQueryPoint(args.get(0));
        this.k = ((Long) args.get(1)).intValue();
        this.nearestNodes = new PriorityQueue<>(k, Comparator.comparingDouble(KnnOutput::getDistance).reversed());
        this.progressListener = progressListener;
    }

    private Geometry decodeQueryPoint(Object arg) {
        if (arg instanceof Node) {
            return geometryDecoder.decodeGeometry((Node) arg);
        } else if (arg instanceof ArrayList<?> && ((ArrayList<?>) arg).get(0) instanceof Double) {
            ArrayList<Double> list = (ArrayList<Double>) arg;
            Coordinate coord = new Coordinate(list.get(0), list.get(1));
            return new GeometryFactory().createPoint(coord);
        } else {
            throw new IllegalArgumentException("Invalid Query Point Format");
        }
    }

    public Stream<KnnOutput> query() {
        KnnVisitor visitor = new KnnVisitor(queryPoint, nearestNodes, k, geometryDecoder);

        List<KnnOutput> initialNodes = new ArrayList<>();
        for (Node layer : layers) {
            Node root = getIndexRoot(layer);
            double distance = QueryUtils.getIndexNodeEnvelope(root).distance(visitor.getQueryPointCoords());
            initialNodes.add(new KnnOutput(root, distance));
        }

        initialNodes.sort(Comparator.comparingDouble(KnnOutput::getDistance));
        for (KnnOutput knnOutput : initialNodes) {
            visitKnn(visitor, knnOutput.getNode());
        }

        return nearestNodes.stream();
    }

    public void visitKnn(KnnVisitor visitor, Node indexNode) {
        if (indexNode.hasRelationship(Direction.OUTGOING, RTreeRelationshipTypes.RTREE_CHILD)) {
            List<KnnOutput> children = new ArrayList<>();
            for (Relationship rel : indexNode.getRelationships(Direction.OUTGOING, RTreeRelationshipTypes.RTREE_CHILD)) {
                Node child = rel.getEndNode();
                double distance = QueryUtils.getIndexNodeEnvelope(child).distance(visitor.getQueryPointCoords());
                children.add(new KnnOutput(child, distance));
            }
            progressListener.updateVisitedIndexCount(children.size());
            children.sort(Comparator.comparingDouble(KnnOutput::getDistance));
            for (KnnOutput child : children) {
                if (child.getDistance() > visitor.getNnDistTemp()) {
                    return;
                }
                visitKnn(visitor, child.getNode());
            }
        } else if (indexNode.hasRelationship(Direction.OUTGOING, RTreeRelationshipTypes.RTREE_REFERENCE)) {
            int geometryCount = 0;
            for (Relationship rel : indexNode.getRelationships(Direction.OUTGOING, RTreeRelationshipTypes.RTREE_REFERENCE)) {
                visitor.onIndexReference(rel.getEndNode());
                geometryCount++;
            }
            progressListener.updateCandidateGeometryCount(geometryCount);
        }
    }
}
