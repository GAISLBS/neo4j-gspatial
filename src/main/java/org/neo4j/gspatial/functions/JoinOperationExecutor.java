package org.neo4j.gspatial.functions;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.gspatial.index.rtree.query.QueryUtils.NodeWithGeometry;
import org.neo4j.gspatial.utils.RtreeUtility.JoinOutput;
import org.neo4j.logging.Log;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class JoinOperationExecutor {
    private final Log log;
    private final JoinOperation operation;

    public JoinOperationExecutor(Log log, String operationName) {
        this.log = log;
        if (JoinOperation.isTopologyOperation(operationName)) {
            this.operation = JoinOperation.valueOf(operationName);
        } else {
            throw new IllegalArgumentException(String.format("Operation %s is not supported", operationName));
        }
    }

    public enum JoinOperation {
        CONTAINS(Geometry::contains),
        COVERS(Geometry::covers),
        COVERED_BY(Geometry::coveredBy),
        CROSSES(Geometry::crosses),
        DISJOINT(Geometry::disjoint),
        EQUALS(Geometry::equals),
        INTERSECTS(Geometry::intersects),
        OVERLAPS(Geometry::overlaps),
        TOUCHES(Geometry::touches),
        WITHIN(Geometry::within);

        private final BiFunction<Geometry, Geometry, Boolean> executor;

        JoinOperation(BiFunction<Geometry, Geometry, Boolean> executor) {
            this.executor = executor;
        }

        public Boolean execute(Geometry geom1, Geometry geom2) {
            return executor.apply(geom1, geom2);
        }

        public static boolean isTopologyOperation(String operationName) {
            return Arrays.stream(JoinOperation.values())
                    .anyMatch(op -> op.name().equals(operationName.toUpperCase()));
        }
    }

    public Stream<JoinOutput> executeOperation(List<NodeWithGeometry.Pair> pairs) {
        return pairs.parallelStream()
                .filter(pair -> operation.execute(pair.nwg1.geometry, pair.nwg2.geometry))
                .map(pair -> new JoinOutput(pair.nwg1.node, pair.nwg2.node));
    }
}
