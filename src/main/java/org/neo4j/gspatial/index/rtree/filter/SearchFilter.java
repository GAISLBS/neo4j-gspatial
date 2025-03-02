package org.neo4j.gspatial.index.rtree.filter;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.index.rtree.Envelope;

public interface SearchFilter {

    boolean needsToVisit(Envelope envelope);

    boolean geometryMatches(Transaction tx, Node geomNode);

}
