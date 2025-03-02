package org.neo4j.gspatial.index.rtree.filter;

import org.neo4j.graphdb.Node;
import org.neo4j.gspatial.index.rtree.Envelope;
import org.neo4j.gspatial.index.rtree.EnvelopeDecoder;

public class SearchEqualEnvelopes extends AbstractSearchEnvelopeIntersection {

    public SearchEqualEnvelopes(EnvelopeDecoder decoder, Envelope referenceEnvelope) {
        super(decoder, referenceEnvelope);
    }

    @Override
    protected boolean onEnvelopeIntersection(Node geomNode, Envelope geomEnvelope) {
        return referenceEnvelope.contains(geomEnvelope) && geomEnvelope.contains(referenceEnvelope);
    }

}