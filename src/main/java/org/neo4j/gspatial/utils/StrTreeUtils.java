package org.neo4j.gspatial.utils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.neo4j.graphdb.Node;
import org.neo4j.gspatial.constants.SpatialConstants;
import org.neo4j.gspatial.index.rtree.JtsGeometryDecoderFromNode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class StrTreeUtils {
    public static final String GEOMETRY_NAME = SpatialConstants.GEOMETRYNAME.getValue();
    public static final JtsGeometryDecoderFromNode GEOM_DECODER = new JtsGeometryDecoderFromNode(GEOMETRY_NAME);

    public static STRtree loadIndex(String path) {
        try (FileInputStream fileIn = new FileInputStream(path);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            return (STRtree) in.readObject();
        } catch (Exception e) {
            throw new RuntimeException(String.format("there is no StrTree index in %s", path), e);
        }
    }

    public static void saveIndex(STRtree index, String path) {
        try (FileOutputStream fileOut = new FileOutputStream(path);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(index);
        } catch (Exception e) {
            throw new RuntimeException(String.format("failed to save StrTree index to %s", path), e);
        }
    }

    public static Envelope getEnvelope(Node node) {
        return GEOM_DECODER.decodeGeometry(node).getEnvelopeInternal();
    }

    public static STRtree loadOrCreateIndex(String filePath) {
        return Files.exists(Paths.get(filePath)) ? loadIndex(filePath) : new STRtree();
    }

    public static Envelope decodeReferenceEnvelope(List<Object> args) {
        if (args.size() == 2) {
            if (args.get(0) instanceof Node) {
                Node node = (Node) args.get(0);
                Double distance = (Double) args.get(1);
                Object propValue = node.getProperty(GEOMETRY_NAME);
                Geometry geometry = GeometryUtility.parseGeometry((String) propValue);
                Geometry buffer = geometry.buffer(distance);
                return new Envelope(buffer.getEnvelopeInternal());
            } else if (args.get(0) instanceof ArrayList<?> && ((ArrayList<?>) args.get(0)).get(0) instanceof Double) {
                ArrayList<Double> coord = (ArrayList<Double>) args.get(0);
                Double distance = (Double) args.get(1);
                Geometry geometry = new GeometryFactory().createPoint(new Coordinate(coord.get(0), coord.get(1)));
                Geometry buffer = geometry.buffer(distance);
                return new Envelope(buffer.getEnvelopeInternal());
            } else {
                throw new IllegalArgumentException("Invalid argument");
            }
        } else if (args.size() == 4) {
            return new Envelope((Double) args.get(0), (Double) args.get(1), (Double) args.get(2), (Double) args.get(3));
        } else {
            throw new IllegalArgumentException("Invalid argument");
        }
    }

    public static Geometry envelopeToGeometry(Envelope env) {
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[]{
                new Coordinate(env.getMinX(), env.getMinY()),
                new Coordinate(env.getMinX(), env.getMaxY()),
                new Coordinate(env.getMaxX(), env.getMaxY()),
                new Coordinate(env.getMaxX(), env.getMinY()),
                new Coordinate(env.getMinX(), env.getMinY())
        };
        return factory.createPolygon(coordinates);
    }
}
