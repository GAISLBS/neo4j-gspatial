package org.neo4j.gspatial.index.rtree;

import org.neo4j.graphdb.Node;

public class SpatialIndexRecordCounter implements SpatialIndexVisitor {

    private int geometryResult;
    private int indexResult;

    public void onIndexReference(Node geomNode) {
        geometryResult++;
    }

    public boolean needsToVisit(Envelope indexNodeEnvelope) {
        indexResult++;
        return true;
    }

    public int getGeometryResult() {
        return geometryResult;
    }

    public int getIndexResult() {
        return indexResult;
    }
}

