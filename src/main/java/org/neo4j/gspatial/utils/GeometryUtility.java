package org.neo4j.gspatial.utils;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class GeometryUtility {

    private static final WKTReader wktReader = new WKTReader();
    private static final int defaultSRID = 4326;

    public static Geometry parseWKT(String wkt) {
        try {
            Geometry geometry = wktReader.read(wkt);
            geometry.setSRID(defaultSRID);
            return geometry;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse WKT", e);
        }
    }

    public static int getDefaultSRID() {
        return defaultSRID;
    }

}
