package org.neo4j.gspatial.index.rtree.filter;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.index.rtree.Envelope;
import org.neo4j.gspatial.index.rtree.EnvelopeDecoder;

public abstract class AbstractSearchEnvelopeIntersection implements SearchFilter {

    protected EnvelopeDecoder decoder;
    protected Envelope referenceEnvelope;

    public AbstractSearchEnvelopeIntersection(EnvelopeDecoder decoder, Envelope referenceEnvelope) {
        this.decoder = decoder;
        this.referenceEnvelope = referenceEnvelope;
    }

    public Envelope getReferenceEnvelope() {
        return referenceEnvelope;
    }

    @Override
    public boolean needsToVisit(Envelope indexNodeEnvelope) {
        return indexNodeEnvelope.intersects(referenceEnvelope);
    }

    @Override
    public final boolean geometryMatches(Transaction tx, Node geomNode) {
        Envelope geomEnvelope = decoder.decodeEnvelope(geomNode);
        if (geomEnvelope.intersects(referenceEnvelope)) {
            return onEnvelopeIntersection(geomNode, geomEnvelope);
        }

        return false;
    }

    @Override
    public String toString() {
        return "SearchEnvelopeIntersection[" + referenceEnvelope + "]";
    }

    protected abstract boolean onEnvelopeIntersection(Node geomNode, Envelope geomEnvelope);
}