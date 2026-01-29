package org.neo4j.gspatial.functions;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.constants.SpatialConstants;
import org.neo4j.gspatial.utils.RtreeUtility.RangeOutput;
import org.neo4j.gspatial.index.rtree.ProgressLoggingListener;
import org.neo4j.logging.Level;
import org.neo4j.logging.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.neo4j.gspatial.utils.StrTreeUtils.*;

public class StrTreeQueryExecutor {
    private final Transaction tx;
    private final HashMap<String, STRtree> spatialIndexes = new HashMap<>();
    public static final String UUID = SpatialConstants.UUIDNAME.getValue();
    private Geometry searchGeometry;
    private Envelope searchEnvelope;
    ProgressLoggingListener progressListener;

    public StrTreeQueryExecutor(Log log, Transaction tx, List<String> spatialSetLabels) {
        this.tx = tx;
        progressListener = new ProgressLoggingListener("StrTreeQueryExecutor", log, Level.INFO, new int[]{0, 0});
        progressListener.begin(spatialSetLabels.size() * 3 + 1);
        spatialSetLabels.forEach(label -> {
            String indexPath = label + "_StrTree.bin";
            this.spatialIndexes.put(label, loadOrCreateIndex(indexPath));
        });
        progressListener.worked(1, "Done loading spatial indexes");
    }

    public Stream<RangeOutput> executeQuery(List<Object> args) {
        this.searchEnvelope = decodeReferenceEnvelope(args);
        this.searchGeometry = envelopeToGeometry(searchEnvelope);
        progressListener.worked(1, "Done decoding search envelope");
        return queryRange();
    }

    private Stream<RangeOutput> queryRange() {
        return spatialIndexes.entrySet().stream()
                .flatMap(entry -> queryLabelRange(entry.getKey(), entry.getValue()))
                .peek(result -> progressListener.worked(1, "Done Filtering"))
                .filter(result -> result.geometry.within(searchGeometry))
                .peek(result -> progressListener.worked(1, "Done Refinement"))
                .map(result -> new RangeOutput(result.node))
                .onClose(() -> progressListener.done());
    }

    private Stream<QueryResult> queryLabelRange(String label, STRtree spatialIndex) {
        List<Object> resultsFromIndex = spatialIndex.query(searchEnvelope);
        progressListener.worked(1, "Done querying index");
        return resultsFromIndex.stream()
                .map(result -> findNodeByLabelAndUuid(label, result))
                .filter(Objects::nonNull)
                .map(node -> new QueryResult(node, GEOM_DECODER.decodeGeometry(node)));
    }

    private Node findNodeByLabelAndUuid(String label, Object result) {
        Object uuid = (result instanceof Long) ? result : result.toString();
        return tx.findNode(Label.label(label), UUID, uuid);
    }

    private record QueryResult(Node node, Geometry geometry) {
    }
}
