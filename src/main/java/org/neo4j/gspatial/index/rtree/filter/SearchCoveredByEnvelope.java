package org.neo4j.gspatial.index.rtree.filter;

import org.neo4j.graphdb.Node;
import org.neo4j.gspatial.index.rtree.Envelope;
import org.neo4j.gspatial.index.rtree.EnvelopeDecoder;

/**
 * Find Envelopes covered by the given Envelope
 */
public class SearchCoveredByEnvelope extends AbstractSearchEnvelopeIntersection {

    public SearchCoveredByEnvelope(EnvelopeDecoder decoder, Envelope referenceEnvelope) {
        super(decoder, referenceEnvelope);
    }

    @Override
    protected boolean onEnvelopeIntersection(Node geomNode, Envelope geomEnvelope) {
        // check if every point of this Envelope is a point of the Reference Envelope
        return referenceEnvelope.contains(geomEnvelope);
    }

}