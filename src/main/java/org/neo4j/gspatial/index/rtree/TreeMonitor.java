package org.neo4j.gspatial.index.rtree;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.index.rtree.RTreeIndex.NodeWithEnvelope;

import java.util.List;
import java.util.Map;

public interface TreeMonitor {
    void setHeight(int height);

    int getHeight();

    void addNbrRebuilt(RTreeIndex rtree, Transaction tx);

    int getNbrRebuilt();

    void addSplit(Node indexNode);

    void beforeMergeTree(Node indexNode, List<NodeWithEnvelope> right);

    void afterMergeTree(Node indexNode);

    int getNbrSplit();

    void addCase(String key);

    Map<String, Integer> getCaseCounts();

    void reset();

    void matchedTreeNode(int level, Node node);

    List<Node> getMatchedTreeNodes(int level);
}
