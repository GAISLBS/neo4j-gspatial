package org.neo4j.gspatial.index.rtree;

import org.neo4j.graphdb.Entity;

public interface EnvelopeDecoder {
    Envelope decodeEnvelope(Entity container);
}
