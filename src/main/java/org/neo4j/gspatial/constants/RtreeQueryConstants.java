package org.neo4j.gspatial.constants;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.index.rtree.HexaFunction;
import org.neo4j.gspatial.index.rtree.ProgressLoggingListener;
import org.neo4j.gspatial.index.rtree.query.Join;
import org.neo4j.gspatial.index.rtree.query.Knn;
import org.neo4j.gspatial.index.rtree.query.Range;
import org.neo4j.logging.Log;

import java.util.List;
import java.util.stream.Stream;

public final class RtreeQueryConstants {
    public enum RtreeQuery {
        JOIN((tx, layers, args, cypherQuery, log, progressListener) -> new Join(tx, layers, args, log, progressListener).query()),
        RANGE((tx, layers, args, cypherQuery, log, progressListener) -> new Range(tx, layers, args, cypherQuery, progressListener).query()),
        KNN((tx, layers, args, cypherQuery, log, progressListener) -> new Knn(tx, layers, args, progressListener).query());

        private final HexaFunction<Transaction, List<Node>, List<Object>, String, Log, ProgressLoggingListener, Stream<?>> executor;

        RtreeQuery(HexaFunction<Transaction, List<Node>, List<Object>, String, Log, ProgressLoggingListener, Stream<?>> executor) {
            this.executor = executor;
        }

        public Stream<?> execute(Transaction tx, List<Node> layers, List<Object> args, String cypherQuery, Log log, ProgressLoggingListener progressListener) {
            return executor.apply(tx, layers, args, cypherQuery, log, progressListener);
        }
    }
}