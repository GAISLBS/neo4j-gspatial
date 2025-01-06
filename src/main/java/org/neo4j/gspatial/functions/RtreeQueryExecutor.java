package org.neo4j.gspatial.functions;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.constants.RtreeQueryConstants;
import org.neo4j.gspatial.index.rtree.ProgressLoggingListener;
import org.neo4j.gspatial.utils.RtreeUtility;
import org.neo4j.logging.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class RtreeQueryExecutor {
    private final Transaction tx;
    private final Log log;
    private final List<Node> layers = new LinkedList<>();
    private final String cypherQuery;

    public RtreeQueryExecutor(Log log, Transaction tx, List<String> spatialSetLabels, String cypherQuery, Boolean disconnect) {
        this.tx = tx;
        this.log = log;
        this.cypherQuery = cypherQuery;
        spatialSetLabels.forEach(label -> {
            String RtreeLabel = label + (disconnect ? "ComparisonRTree" : "RTree");
            layers.add(RtreeUtility.getLayer(tx, RtreeLabel));
        });
    }

    public Stream<?> executeOperation(String queryType, List<Object> args) {
//        ProgressLoggingListener progressListener = new ProgressLoggingListener(queryType, log, Level.INFO, RtreeUtility.getMetas(layers));
        log.info(String.format("Running gspatial.%s with arguments: %s", queryType, args));
        try {
            RtreeQueryConstants.RtreeQuery query = RtreeQueryConstants.RtreeQuery.valueOf(queryType.toUpperCase());
            ProgressLoggingListener progressListener = new ProgressLoggingListener(queryType, System.out, RtreeUtility.getMetas(layers, query));
            progressListener.setTimeWait(1);
            progressListener.begin(6);
            Stream<?> results = query.execute(tx, layers, args, cypherQuery, log, progressListener);
            progressListener.worked(1);
            progressListener.done();
            return results;

        } catch (IllegalArgumentException e) {
            log.error("Invalid query type provided: " + queryType, e);
            return null;
        }
    }
}
