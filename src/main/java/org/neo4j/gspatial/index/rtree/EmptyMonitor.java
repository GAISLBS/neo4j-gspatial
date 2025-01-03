package org.neo4j.gspatial.index.rtree;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.index.rtree.RTreeIndex.NodeWithEnvelope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmptyMonitor implements TreeMonitor {
    @Override
    public void setHeight(int height) {
    }

    public int getHeight() {
        return -1;
    }

    @Override
    public void addNbrRebuilt(RTreeIndex rtree, Transaction tx) {
    }

    @Override
    public int getNbrRebuilt() {
        return -1;
    }

    @Override
    public void addSplit(Node indexNode) {

    }

    @Override
    public void beforeMergeTree(Node indexNode, List<NodeWithEnvelope> right) {

    }

    @Override
    public void afterMergeTree(Node indexNode) {

    }

    @Override
    public int getNbrSplit() {
        return -1;
    }

    @Override
    public void addCase(String key) {

    }

    @Override
    public Map<String, Integer> getCaseCounts() {
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public void matchedTreeNode(int level, Node node) {

    }

    @Override
    public List<Node> getMatchedTreeNodes(int level) {
        return new ArrayList();
    }
}
