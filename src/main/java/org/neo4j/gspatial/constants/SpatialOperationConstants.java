package org.neo4j.gspatial.constants;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public final class SpatialOperationConstants {
    public enum SpatialOperation {
        AREA(args -> ((Geometry) args.get(0)).getArea(), 1),
        BBOX(args -> {
            Envelope envelope = ((Geometry) args.get(0)).getEnvelopeInternal();
            return List.of(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
        }, 1),
        BUFFER(args -> {
            Geometry geometry = (Geometry) args.get(0);
            Double distance = (Double) args.get(1);
            if (SpatialConstants.SRID.getIntValue() == 4326) {  // WGS84
                distance = distance / 99300;                    // meters to degrees
            }
            return geometry.buffer(distance);
        }, 2),
        BOUNDARY(args -> ((Geometry) args.get(0)).getBoundary(), 1),
        CENTROID(args -> ((Geometry) args.get(0)).getCentroid(), 1),
        CONTAINS(args -> ((Geometry) args.get(0)).contains((Geometry) args.get(1)), 2),
        CONVEX_HULL(args -> ((Geometry) args.get(0)).convexHull(), 1),
        COVERS(args -> ((Geometry) args.get(0)).covers((Geometry) args.get(1)), 2),
        COVERED_BY(args -> ((Geometry) args.get(0)).coveredBy((Geometry) args.get(1)), 2),
        CROSSES(args -> ((Geometry) args.get(0)).crosses((Geometry) args.get(1)), 2),
        DIFFERENCE(args -> ((Geometry) args.get(0)).difference((Geometry) args.get(1)), 2),
        DIMENSION(args -> ((Geometry) args.get(0)).getDimension(), 1),
        DISJOINT(args -> ((Geometry) args.get(0)).disjoint((Geometry) args.get(1)), 2),
        DISTANCE(args -> {
            Double distance = ((Geometry) args.get(0)).distance((Geometry) args.get(1));
            if (SpatialConstants.SRID.getIntValue() == 4326) {  // WGS84
                distance = distance * 99300;                    // degrees to meters
            }
            return distance;
        }, 2),
        ENVELOPE(args -> ((Geometry) args.get(0)).getEnvelope(), 1),
        EQUALS(args -> ((Geometry) args.get(0)).equals((Geometry) args.get(1)), 2),
        INTERSECTION(args -> ((Geometry) args.get(0)).intersection((Geometry) args.get(1)), 2),
        INTERSECTS(args -> ((Geometry) args.get(0)).intersects((Geometry) args.get(1)), 2),
        LENGTH(args -> ((Geometry) args.get(0)).getLength(), 1),
        OVERLAPS(args -> ((Geometry) args.get(0)).overlaps((Geometry) args.get(1)), 2),
        SRID(args -> ((Geometry) args.get(0)).getSRID(), 1),
        TOUCHES(args -> ((Geometry) args.get(0)).touches((Geometry) args.get(1)), 2),
        UNION(args -> ((Geometry) args.get(0)).union((Geometry) args.get(1)), 2),
        WITHIN(args -> ((Geometry) args.get(0)).within((Geometry) args.get(1)), 2);

        private final Function<List<Object>, Object> executor;
        private final int argCount;

        SpatialOperation(Function<List<Object>, Object> executor, int argCount) {
            this.executor = executor;
            this.argCount = argCount;
        }

        public Object execute(List<Object> args) {
            return executor.apply(args);
        }

        public int getArgCount() {
            return argCount;
        }
    }

    public static boolean isTopologyOperation(String operationName) {
        return Arrays.asList("CONTAINS", "COVERS", "COVERED_BY", "CROSSES", "DISJOINT", "EQUALS", "INTERSECTS", "OVERLAPS", "TOUCHES", "WITHIN")
                .contains(operationName.toUpperCase());
    }

    public static boolean isSetOperation(String operationName) {
        return Arrays.asList("DIFFERENCE", "INTERSECTION", "SYMDIFFERENCE", "UNION")
                .contains(operationName.toUpperCase());
    }

    public static boolean isSingParameterOperation(String operationName) {
        return Arrays.asList("BUFFER").contains(operationName.toUpperCase());
    }

    public static boolean isDualParameterOperation(String operationName) {
        return Arrays.asList("DISTANCE").contains(operationName.toUpperCase());
    }

    public static boolean isSingOperation(String operationName) {
        return Arrays.asList("AREA", "BBOX", "BOUNDARY", "CENTROID", "CONVEX_HULL", "DIMENSION", "ENVELOPE", "LENGTH", "SRID").contains(operationName.toUpperCase());
    }
}
