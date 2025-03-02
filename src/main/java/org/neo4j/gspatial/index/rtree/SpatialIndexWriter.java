package org.neo4j.gspatial.index.rtree;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.List;

public interface SpatialIndexWriter extends SpatialIndexReader {

    void add(Transaction tx, Node geomNode);

    void add(Transaction tx, List<Node> geomNodes);

    void remove(Transaction tx, String geomNodeId, boolean deleteGeomNode, boolean throwExceptionIfNotFound);

    void removeAll(Transaction tx, boolean deleteGeomNodes, Listener monitor);

    void clear(Transaction tx, Listener monitor);

}
