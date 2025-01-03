package org.neo4j.gspatial.index.rtree.filter;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.index.rtree.Envelope;

public class SearchAll implements SearchFilter {

    @Override
    public boolean needsToVisit(Envelope indexNodeEnvelope) {
        return true;
    }

    @Override
    public boolean geometryMatches(Transaction tx, Node geomNode) {
        return true;
    }

}