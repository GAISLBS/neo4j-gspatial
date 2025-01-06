package org.neo4j.gspatial.utils;

import org.geotools.geojson.geom.GeometryJSON;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Geometry;
import org.neo4j.graphdb.Node;
import org.neo4j.gspatial.constants.SpatialConstants;
import org.neo4j.gspatial.index.rtree.EnvelopeDecoderFromBbox;
import org.neo4j.gspatial.index.rtree.JtsGeometryDecoderFromNode;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.neo4j.gspatial.utils.GeometryUtility.parseCoordinates;
import static org.neo4j.gspatial.utils.GeometryUtility.parseGeometry;

public class IOUtility {
    private static final String geometryName = SpatialConstants.GEOMETRYNAME.getValue();
    private static final String bbox = SpatialConstants.BBOX.getValue();
    private static final String uuid = SpatialConstants.UUIDNAME.getValue();
    public static final EnvelopeDecoderFromBbox bboxDecoder = new EnvelopeDecoderFromBbox(bbox);
    public static final JtsGeometryDecoderFromNode geometryDecoder = new JtsGeometryDecoderFromNode(geometryName);

    public static List<Geometry> argsConverter(List<Object> args) {
        List<Geometry> processedArgs = new ArrayList<>();
        for (Object arg : args) {
            Geometry convertedArg = convertArg(arg);
            if (convertedArg != null) {
                processedArgs.add(convertedArg);
            }
        }
        return processedArgs;
    }

    private static Geometry convertArg(Object arg) {
        if (arg instanceof Node) {
            return convertNode((Node) arg);
        } else if (arg instanceof String) {
            return parseGeometry((String) arg);
        } else if (arg instanceof double[]) {
            return parseCoordinates((double[]) arg);
        } else if (arg instanceof double[][]) {
            return parseCoordinates((double[][]) arg);
        }
        throw new IllegalArgumentException("Unsupported argument type: " + arg.getClass().getSimpleName());
    }

    public static Geometry convertNode(Node node) {
        if (node.hasProperty(geometryName)) {
            return geometryDecoder.decodeGeometry(node);
        } else if (node.hasProperty(bbox)) {
            return bboxDecoder.decodeEnvelope(node).toGeometry();
        }
        throw new IllegalArgumentException("Node with ID " + node.getElementId() + " does not have a 'geometry' property");
    }

    public static Object convertResult(Object result) {
        return result instanceof Geometry ? result.toString() : result;
    }

    public static class Output {
        public Object result;
        public Object n;
        public Object m;

        public Output(Object result, Object n, Object m) {
            this.result = result;
            this.n = n;
            this.m = m;
        }

        public Output(Object result, Object n) {
            this.result = result;
            this.n = n;
        }
    }

    public static JSONObject convertNodeToGeoJson(Node node) {
        JSONObject feature = new JSONObject();
        feature.put("type", "Feature");

        JSONObject properties = new JSONObject();
        if (node.hasProperty(uuid)) {
            properties.put("idx", node.getProperty(uuid).toString());
        }

        Geometry geom = convertNode(node);
        JSONObject geometry = new JSONObject();
        GeometryJSON gjson = new GeometryJSON();
        try (StringWriter writer = new StringWriter()) {
            gjson.write(geom, writer);
            geometry = (JSONObject) new JSONParser().parse(writer.toString());
        } catch (Exception e) {
            throw new RuntimeException("Error converting geometry to GeoJSON", e);
        }

        feature.put("geometry", geometry);
        feature.put("properties", properties);

        return feature;
    }

    public static void saveToGeoJson(List<Node> entryList, String fileName) {
        JSONObject featureCollection = new JSONObject();
        featureCollection.put("type", "FeatureCollection");

        List<JSONObject> features = new ArrayList<>();
        for (Node entry : entryList) {
            features.add(convertNodeToGeoJson(entry));
        }
        featureCollection.put("features", features);

        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(featureCollection.toJSONString());
        } catch (IOException e) {
            throw new RuntimeException("Error writing GeoJSON to file", e);
        }
    }

    public static void setSpatialConstants(String geomFormat, String uuidName, String geomName, String srid) {
        SpatialConstants.GeometryFormat.setValue(geomFormat.toUpperCase());
        SpatialConstants.UUIDNAME.setValue(uuidName);
        SpatialConstants.GEOMETRYNAME.setValue(geomName);
        SpatialConstants.SRID.setValue(Integer.parseInt(srid));
    }
}
