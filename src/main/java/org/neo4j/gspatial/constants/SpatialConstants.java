package org.neo4j.gspatial.constants;

public enum SpatialConstants {
    GeometryFormat("WKT"),
    UUIDNAME("idx"),
    GEOMETRYNAME("geometry"),
    SRID(4326),
    BBOX("bbox");

    private String stringValue;
    private int intValue;

    SpatialConstants(String value) {
        this.stringValue = value;
    }

    SpatialConstants(int value) {
        this.intValue = value;
    }

    SpatialConstants() {
    }

    public String getValue() {
        return stringValue;
    }

    public void setValue(String value) {
        this.stringValue = value;
    }

    public int getIntValue() {
        return intValue;
    }

    public void setValue(int value) {
        this.intValue = value;
    }
}
