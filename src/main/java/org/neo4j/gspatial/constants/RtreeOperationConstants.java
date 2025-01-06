package org.neo4j.gspatial.constants;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.index.rtree.RTreeIndex;
import org.neo4j.gspatial.index.rtree.TriFunction;

import java.util.List;

public final class RtreeOperationConstants {

    public enum RtreeOperation {
        INSERT((index, tx, args) -> {
            index.add(tx, args);
            return String.format("build %d nodes", args.size());
        }),
        DELETE((index, tx, args) -> {
            for (Node node : args) {
                String geomNodeIdx = node.getProperty("idx").toString();
                index.remove(tx, geomNodeIdx, false, true);
            }
            return String.format("Deleted %d nodes", args.size());
        });

        private final TriFunction<RTreeIndex, Transaction, List<Node>, Object> executor;

        RtreeOperation(TriFunction<RTreeIndex, Transaction, List<Node>, Object> executor) {
            this.executor = executor;
        }

        public Object execute(RTreeIndex index, Transaction tx, List<Node> args) {
            return executor.apply(index, tx, args);
        }
    }
}
