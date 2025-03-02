package org.neo4j.gspatial.index.rtree;

import org.neo4j.graphdb.Node;

public interface SpatialIndexVisitor {

    boolean needsToVisit(Envelope indexNodeEnvelope);

    void onIndexReference(Node geomNode);

}
