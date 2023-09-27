package org.neo4j.gspatial.utils;

import com.github.davidmoten.geo.Coverage;
import com.github.davidmoten.geo.GeoHash;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

public class GeohashUtility {
    public static String generateGeohash(String wkt) {
        Geometry geometry = GeometryUtility.parseWKT(wkt);
        if (geometry.getGeometryType().equals("Point")) {
            var lat = geometry.getCoordinate().y;
            var lon = geometry.getCoordinate().x;
            return GeoHash.encodeHash(lat, lon, 9);
        } else {
            Envelope envelope = geometry.getEnvelopeInternal();
            double topLeftLat = envelope.getMaxY();
            double bottomRightLat = envelope.getMinY();
            double topLeftLon = envelope.getMinX();
            double bottomRightLon = envelope.getMaxX();

            Coverage coverage = GeoHash.coverBoundingBoxMaxHashes(topLeftLat, topLeftLon, bottomRightLat, bottomRightLon, 1);
            return String.join("", coverage.getHashes());
        }
    }
}
