package org.neo4j.gspatial.index.rtree;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.index.rtree.filter.SearchFilter;
import org.neo4j.gspatial.index.rtree.filter.SearchResults;

import java.util.Map;

public interface SpatialIndexReader {

    EnvelopeDecoder getEnvelopeDecoder();

    boolean isEmpty(Transaction tx);

    int count(Transaction tx);

    Envelope getBoundingBox(Transaction tx);

    boolean isNodeIndexed(Transaction tx, String nodeId);

    Iterable<Node> getAllIndexedNodes(Transaction tx);

    SearchResults searchIndex(Transaction tx, SearchFilter filter);

    void addMonitor(TreeMonitor monitor);

    void configure(Map<String, Object> config);
}

