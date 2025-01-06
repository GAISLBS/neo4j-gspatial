package org.neo4j.gspatial.utils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.neo4j.gspatial.constants.SpatialConstants;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GeometryUtility {

    private static final WKTReader WKT_READER = new WKTReader();
    private static final WKBReader WKB_READER = new WKBReader();
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final int srid = SpatialConstants.SRID.getIntValue();
    private static final String geometryFormat = SpatialConstants.GeometryFormat.getValue();
    private static final ConcurrentMap<String, Geometry> geometryCache = new ConcurrentHashMap<>();

    public static Geometry parseGeometry(String data) {
        return geometryCache.computeIfAbsent(data, key -> {
            try {
                Geometry geometry = switch (geometryFormat) {
                    case "WKB" -> {
                        byte[] bytes = WKBReader.hexToBytes(data);
                        yield WKB_READER.read(bytes);
                    }
                    case "WKT" -> WKT_READER.read(data);
                    default -> throw new IllegalArgumentException("Unsupported geometry format: " + geometryFormat);
                };
                geometry.setSRID(srid);
                return validateAndFixGeometry(geometry);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Failed to parse " + geometryFormat + ": " + e.getMessage(), e);
            }
        });
    }

    public static Geometry parseCoordinates(double[] coordinates) {
        validatePointCoordinates(coordinates);
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(coordinates[0], coordinates[1]));
        point.setSRID(srid);
        return validateAndFixGeometry(point);
    }

    public static Geometry parseCoordinates(double[][] coordinates) {
        validateCoordinates(coordinates);

        int numPoints = coordinates.length;
        if (numPoints == 1) {
            return parseCoordinates(coordinates[0]);
        } else {
            Coordinate[] coords = toCoordinateArray(coordinates);
            Geometry geometry = coords[0].equals2D(coords[coords.length - 1])
                    ? GEOMETRY_FACTORY.createPolygon(coords)
                    : GEOMETRY_FACTORY.createLineString(coords);
            geometry.setSRID(srid);
            return validateAndFixGeometry(geometry);
        }
    }

    private static void validatePointCoordinates(double[] coordinates) {
        if (coordinates == null || coordinates.length != 2) {
            throw new IllegalArgumentException("Point coordinates must contain exactly two values.");
        }
    }

    private static void validateCoordinates(double[][] coordinates) {
        if (coordinates == null || coordinates.length == 0) {
            throw new IllegalArgumentException("Coordinates array must not be null or empty.");
        }
    }

    private static Coordinate[] toCoordinateArray(double[][] coordinates) {
        Coordinate[] coords = new Coordinate[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            if (coordinates[i].length != 2) {
                throw new IllegalArgumentException("Each coordinate must contain exactly two values.");
            }
            coords[i] = new Coordinate(coordinates[i][0], coordinates[i][1]);
        }
        return coords;
    }

    private static Geometry validateAndFixGeometry(Geometry geometry) {
        IsValidOp validator = new IsValidOp(geometry);
        if (!validator.isValid()) {
            TopologyValidationError error = validator.getValidationError();
            System.out.println("Geometry is invalid: " + error.getMessage() + " - Attempting to fix.");
            geometry = GeometryFixer.fix(geometry);
            validator = new IsValidOp(geometry);
            if (!validator.isValid()) {
                throw new IllegalArgumentException("Failed to fix invalid geometry: " + validator.getValidationError().getMessage());
            }
        }
        return geometry;
    }
}
