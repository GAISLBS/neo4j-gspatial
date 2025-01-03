package org.neo4j.gspatial.index.rtree;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.graphdb.Node;
import org.neo4j.gspatial.utils.RtreeUtility.KnnOutput;

import java.util.PriorityQueue;

public class KnnVisitor implements SpatialIndexVisitor {
    private Geometry queryPoint;
    private double[] queryPointCoords;
    private final PriorityQueue<KnnOutput> nearestNodes;
    private int k;
    private double nnDistTemp = Double.MAX_VALUE;
    private JtsGeometryDecoderFromNode geometryDecoder;


    public KnnVisitor(Geometry queryPoint, PriorityQueue<KnnOutput> nearestNodes, int k, JtsGeometryDecoderFromNode geometryDecoder) {
        this.queryPoint = queryPoint;
        this.nearestNodes = nearestNodes;
        this.k = k;
        this.queryPointCoords = new double[]{queryPoint.getCentroid().getX(), queryPoint.getCentroid().getY()};
        this.geometryDecoder = geometryDecoder;
    }

    @Override
    public boolean needsToVisit(Envelope indexNodeEnvelope) {
        double minDist = indexNodeEnvelope.distance(queryPointCoords);
        return minDist < nnDistTemp;
    }

    @Override
    public void onIndexReference(Node geomNode) {
        Geometry targetGeometry = geometryDecoder.decodeGeometry(geomNode);
        double distance = queryPoint.distance(targetGeometry);
        if (nearestNodes.size() < k || distance < nnDistTemp) {
            if (nearestNodes.size() == k) {
                nearestNodes.poll();
            }
            nearestNodes.add(new KnnOutput(geomNode, distance));
            if (distance < nnDistTemp) {
                nnDistTemp = nearestNodes.peek().getDistance();
            }
        }
    }

    public double[] getQueryPointCoords() {
        return queryPointCoords;
    }

    public double getNnDistTemp() {
        return nnDistTemp;
    }

}
