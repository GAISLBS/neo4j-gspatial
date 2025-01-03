package org.neo4j.gspatial.index.rtree;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.neo4j.graphdb.Entity;
import org.neo4j.gspatial.utils.GeometryUtility;

import java.util.ArrayList;

public class EnvelopeDecoderFromJtsGeometry implements EnvelopeDecoder {
    private final String propertyName;

    public EnvelopeDecoderFromJtsGeometry(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Envelope decodeEnvelope(Entity container) {
        Object propValue = container.getProperty(propertyName);
        Geometry geometry = GeometryUtility.parseGeometry((String) propValue);
        return new Envelope(geometry.getEnvelopeInternal());
    }

    public Envelope bufferEnvelope(Entity container, double distance) {
        Object propValue = container.getProperty(propertyName);
        Geometry geometry = GeometryUtility.parseGeometry((String) propValue);
        Geometry buffer = geometry.buffer(distance);
        return new Envelope(buffer.getEnvelopeInternal());
    }

    public Envelope bufferEnvelope(ArrayList<Double> coord, double distance) {
        Geometry geometry = new GeometryFactory().createPoint(new Coordinate(coord.get(0), coord.get(1)));
        Geometry buffer = geometry.buffer(distance);
        return new Envelope(buffer.getEnvelopeInternal());
    }
}
