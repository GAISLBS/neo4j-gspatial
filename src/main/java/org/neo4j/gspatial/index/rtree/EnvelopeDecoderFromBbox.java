package org.neo4j.gspatial.index.rtree;

import org.neo4j.graphdb.Entity;

public class EnvelopeDecoderFromBbox implements EnvelopeDecoder {
    private final String propertyName;
    private final String minx = "min_x";
    private final String miny = "min_y";
    private final String maxx = "max_x";
    private final String maxy = "max_y";

    public EnvelopeDecoderFromBbox(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Envelope decodeEnvelope(Entity container) {
        double[] bbox = (double[]) container.getProperty(propertyName);
        return new Envelope(bbox[0], bbox[2], bbox[1], bbox[3]);
    }

    public Envelope decodeEnvelopeEdge(Entity container) {
        return new Envelope((Double) container.getProperty(minx),
                (Double) container.getProperty(maxx),
                (Double) container.getProperty(miny),
                (Double) container.getProperty(maxy));
    }
}
