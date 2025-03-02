package org.neo4j.gspatial.index.rtree;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.graphdb.Node;
import org.neo4j.gspatial.utils.GeometryUtility;

public class JtsGeometryDecoderFromNode {
    private final String propertyName;

    public JtsGeometryDecoderFromNode(String propertyName) {
        this.propertyName = propertyName;
    }

    public Geometry decodeGeometry(Node node) {
        String propValue = (String) node.getProperty(propertyName);
        return GeometryUtility.parseGeometry(propValue);
    }
}
