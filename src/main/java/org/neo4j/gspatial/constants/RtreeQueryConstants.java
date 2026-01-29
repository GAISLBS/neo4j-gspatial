package org.neo4j.gspatial.constants;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.index.rtree.PentaFunction;
import org.neo4j.gspatial.index.rtree.ProgressLoggingListener;
import org.neo4j.gspatial.index.rtree.query.Join;
import org.neo4j.gspatial.index.rtree.query.Knn;
import org.neo4j.gspatial.index.rtree.query.Range;
import org.neo4j.logging.Log;

import java.util.List;
import java.util.stream.Stream;

public final class RtreeQueryConstants {
    public enum RtreeQuery {
        JOIN((tx, layers, args, log, progressListener) -> new Join(tx, layers, args, log, progressListener).query()),
        RANGE((tx, layers, args, log, progressListener) -> new Range(tx, layers, args, progressListener).query()),
        KNN((tx, layers, args, log, progressListener) -> new Knn(tx, layers, args, progressListener).query());

        private final PentaFunction<Transaction, List<Node>, List<Object>, Log, ProgressLoggingListener, Stream<?>> executor;

        RtreeQuery(PentaFunction<Transaction, List<Node>, List<Object>, Log, ProgressLoggingListener, Stream<?>> executor) {
            this.executor = executor;
        }

        public Stream<?> execute(Transaction tx, List<Node> layers, List<Object> args, Log log, ProgressLoggingListener progressListener) {
            return executor.apply(tx, layers, args, log, progressListener);
        }
    }
}