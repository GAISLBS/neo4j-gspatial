package org.neo4j.gspatial.index.rtree;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.index.rtree.RTreeIndex.NodeWithEnvelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RTreeMonitor implements TreeMonitor {
    private int nbrSplit;
    private int height;
    private int nbrRebuilt;
    private HashMap<String, Integer> cases = new HashMap<>();
    private ArrayList<ArrayList<Node>> matchedTreeNodes = new ArrayList<>();

    public RTreeMonitor() {
        reset();
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void addNbrRebuilt(RTreeIndex rtree, Transaction tx) {
        nbrRebuilt++;
    }

    @Override
    public int getNbrRebuilt() {
        return nbrRebuilt;
    }

    @Override
    public void addSplit(Node indexNode) {
        nbrSplit++;
    }

    @Override
    public void beforeMergeTree(Node indexNode, List<NodeWithEnvelope> right) {

    }

    @Override
    public void afterMergeTree(Node indexNode) {

    }

    @Override
    public int getNbrSplit() {
        return nbrSplit;
    }

    @Override
    public void addCase(String key) {
        Integer n = cases.get(key);
        if (n != null) {
            n++;
        } else {
            n = 1;
        }
        cases.put(key, n);
    }

    @Override
    public Map<String, Integer> getCaseCounts() {
        return cases;
    }

    @Override
    public void reset() {
        cases.clear();
        height = 0;
        nbrRebuilt = 0;
        nbrSplit = 0;
        matchedTreeNodes.clear();
    }

    @Override
    public void matchedTreeNode(int level, Node node) {
        ensureMatchedTreeNodeLevel(level);
        matchedTreeNodes.get(level).add(node);
    }

    private void ensureMatchedTreeNodeLevel(int level) {
        while (matchedTreeNodes.size() <= level) {
            matchedTreeNodes.add(new ArrayList<Node>());
        }
    }

    @Override
    public List<Node> getMatchedTreeNodes(int level) {
        ensureMatchedTreeNodeLevel(level);
        return matchedTreeNodes.get(level).stream().collect(Collectors.toList());
    }
}