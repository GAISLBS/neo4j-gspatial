package org.neo4j.gspatial.functions;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.constants.RtreeOperationConstants.RtreeOperation;
import org.neo4j.gspatial.index.rtree.EnvelopeDecoderFromJtsGeometry;
import org.neo4j.gspatial.index.rtree.RTreeIndex;
import org.neo4j.gspatial.index.rtree.RTreeMonitor;
import org.neo4j.gspatial.utils.RtreeUtility;
import org.neo4j.logging.Log;

import java.util.List;
import java.util.stream.Stream;

public class RtreeOperationExecutor {
    private final Transaction tx;
    private final Log log;
    private final RTreeIndex index;
    private final String RtreeLabel;
    private final Boolean disconnect;

    public RtreeOperationExecutor(Log log, Transaction tx, String spatialSetLabel, Boolean disconnect) {
        this.log = log;
        this.tx = tx;
        this.disconnect = disconnect;
        this.RtreeLabel = spatialSetLabel + (disconnect ? "ComparisonRTree" : "RTree");
        Node layerNode = RtreeUtility.getLayer(tx, RtreeLabel);
        if (layerNode == null) {
            System.out.printf("Creating %s RTree layer node%n", spatialSetLabel);
            layerNode = tx.createNode(Label.label(RtreeLabel));
        }
        System.out.printf("Use %s RTree index%n", spatialSetLabel);
        this.index = new RTreeIndex(tx, layerNode, new EnvelopeDecoderFromJtsGeometry("geometry"), 10, new RTreeMonitor(), RtreeLabel);
    }

    public Stream<RtreeUtility.Output> executeOperation(String operationName, List<Object> rawArgs) {
        log.info(String.format("Running gspatial.%s with arguments: %s", operationName, rawArgs));
        List<Node> checkedArgs = RtreeUtility.checkArgs(tx, rawArgs);

        if (disconnect) {
            log.info("Disconnect flag is true, copying nodes to new layer.");
            checkedArgs = RtreeUtility.copyNodes(tx, checkedArgs, RtreeLabel);
        }

        RtreeOperation operation = RtreeOperation.valueOf(operationName.toUpperCase());
        Object result = operation.execute(index, tx, checkedArgs);
        index.saveCount(tx);
        return Stream.of(new RtreeUtility.Output(result));
    }
}
