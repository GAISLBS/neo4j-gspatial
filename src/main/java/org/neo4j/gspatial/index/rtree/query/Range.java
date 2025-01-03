package org.neo4j.gspatial.index.rtree.query;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.constants.SpatialConstants;
import org.neo4j.gspatial.index.rtree.*;
import org.neo4j.gspatial.utils.IOUtility;
import org.neo4j.gspatial.utils.RtreeUtility.RangeOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Range {
    private final Transaction tx;
    private final Envelope searchEnvelope;
    private final Geometry searchGeometry;
    private final List<Node> layers;
    private final ProgressLoggingListener progressListener;
    private final EnvelopeDecoderFromBbox bboxDecoder = new EnvelopeDecoderFromBbox("bbox");
    private final EnvelopeDecoderFromJtsGeometry geometryDecoder = new EnvelopeDecoderFromJtsGeometry(SpatialConstants.GEOMETRYNAME.getValue());
    private final String cypherQuery;

    public Range(Transaction tx, List<Node> layers, List<Object> args, String cypherQuery, ProgressLoggingListener progressListener) {
        this.tx = tx;
        this.layers = layers;
        this.searchEnvelope = decodeReferenceEnvelope(args);
        this.searchGeometry = searchEnvelope.toGeometry();
        this.progressListener = progressListener;
        this.cypherQuery = cypherQuery;
    }

    private Envelope decodeReferenceEnvelope(List<Object> args) {
        if (args.size() == 2) {
            if (args.get(0) instanceof Node) {
                return geometryDecoder.bufferEnvelope((Node) args.get(0), (Double) args.get(1));
            } else if (args.get(0) instanceof ArrayList<?> && ((ArrayList<?>) args.get(0)).get(0) instanceof Double) {
                ArrayList<Double> list = (ArrayList<Double>) args.get(0);
                return geometryDecoder.bufferEnvelope(list, (Double) args.get(1));
            } else {
                throw new IllegalArgumentException("Invalid argument");
            }
        } else if (args.size() == 4) {
            return new Envelope((Double) args.get(0), (Double) args.get(1), (Double) args.get(2), (Double) args.get(3));
        } else {
            throw new IllegalArgumentException("Invalid argument");
        }
    }

    public Stream<RangeOutput> query() {
        List<Node> candidateNodes = searchTree(layers, new ArrayList<>());
        progressListener.worked(1, "Done searching index");
        candidateNodes = filterCandidatesWithCypher(candidateNodes);
        progressListener.updateCandidateGeometryCount(candidateNodes.size());
        return candidateNodes.parallelStream()
                .filter(this::isWithinSearchEnvelope)
                .map(RangeOutput::new);
    }

    private List<Node> filterCandidatesWithCypher(List<Node> candidateNodes) {
        if (cypherQuery == null || cypherQuery.isEmpty()) {
            return candidateNodes;
        }

        List<Node> filteredNodes = new ArrayList<>();
        for (Node node : candidateNodes) {
            String query = String.format("WITH $node AS n %s", cypherQuery);
            Map<String, Object> params = Map.of("node", node);
            Result result = tx.execute(query, params);
            if (result.hasNext()) {
                filteredNodes.add(node);
            }
        }
        return filteredNodes;
    }

    private boolean isWithinSearchEnvelope(Node node) {
        Geometry geom = IOUtility.convertNode(node);
        return geom.within(searchGeometry);
    }

    public List<Node> searchTree(List<Node> indexes, List<Node> candidateNodes) {
        progressListener.updateVisitedIndexCount(indexes.size());

        List<Node> nextLevelNodes = indexes.stream()
                .flatMap(index -> StreamSupport.stream(index.getRelationships(Direction.OUTGOING).spliterator(), false))
                .filter(rel -> QueryUtils.isDirectoryRelationship(rel) || rel.isType(RTreeRelationshipTypes.RTREE_REFERENCE))
                .map(rel -> {
                    Envelope envelope = bboxDecoder.decodeEnvelopeEdge(rel);
                    if (searchEnvelope.intersects(envelope)) {
                        if (QueryUtils.isDirectoryRelationship(rel)) {
                            return rel.getEndNode();
                        } else {
                            candidateNodes.add(rel.getEndNode());
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return nextLevelNodes.isEmpty() ? candidateNodes : searchTree(nextLevelNodes, candidateNodes);
    }
}
