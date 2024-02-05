package org.neo4j.gspatial.utils;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;

/**
 * This class provides utility methods for working with geometries.
 * It includes methods for parsing WKT strings into Geometry objects and getting the default SRID.
 */
public class GeometryUtility {

    private static final WKTReader wktReader = new WKTReader();
    private static final WKBReader wkbReader = new WKBReader();
    private static final int defaultSRID = 4326;

    /**
     * Parses the given WKT string into a Geometry object.
     * The SRID of the Geometry object is set to the default SRID.
     *
     * @param wkt the WKT string to parse
     * @return the parsed Geometry object
     * @throws IllegalArgumentException if the WKT string cannot be parsed
     */
    private static Geometry parseWKT(String wkt) {
        try {
            Geometry geometry = wktReader.read(wkt);
            geometry.setSRID(defaultSRID);
            return geometry;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse WKT", e);
        }
    }

    private static Geometry parseWKB(String wkb) {
        try {
            byte[] bytes = WKBReader.hexToBytes(wkb);
            Geometry geometry = wkbReader.read(bytes);
            geometry.setSRID(defaultSRID);
            return geometry;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse WKB", e);
        }
    }

    public static Geometry parseGeometry(String data) {
        try {
            return parseWKT(data);
        } catch (IllegalArgumentException e) {
            return parseWKB(data);
        }
    }

    /**
     * Returns the default SRID.
     *
     * @return the default SRID
     */
    public static int getDefaultSRID() {
        return defaultSRID;
    }

}
