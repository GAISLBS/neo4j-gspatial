package org.neo4j.gspatial.index.rtree.query;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.functions.JoinOperationExecutor;
import org.neo4j.gspatial.index.rtree.Envelope;
import org.neo4j.gspatial.index.rtree.ProgressLoggingListener;
import org.neo4j.gspatial.index.rtree.query.QueryUtils.NodeWithEnvelope;
import org.neo4j.gspatial.index.rtree.query.QueryUtils.NodeWithGeometry;
import org.neo4j.gspatial.utils.RtreeUtility.JoinOutput;
import org.neo4j.logging.Log;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.neo4j.gspatial.index.rtree.query.QueryUtils.getJoinOutputFromIndexPair;

public class Join {
    private Transaction tx;
    private Node root1;
    private Node root2;
    private String operationString;
    private Log log;
    private ProgressLoggingListener progressListener;
    private final JoinOperationExecutor executor;
    private final boolean isDisjoint;

    public Join(Transaction tx, List<Node> layers, List<Object> args, Log log, ProgressLoggingListener progressListener) {
        this.tx = tx;
        this.log = log;
        this.root1 = QueryUtils.getIndexRoot(layers.get(0));
        this.root2 = QueryUtils.getIndexRoot(layers.get(1));
        this.operationString = args.get(0).toString().toUpperCase();
        this.progressListener = progressListener;
        this.isDisjoint = operationString.equalsIgnoreCase("DISJOINT");
        this.executor = new JoinOperationExecutor(log, isDisjoint ? "INTERSECTS" : operationString);
    }

    public Stream<JoinOutput> query() {
        Envelope env1 = QueryUtils.getIndexNodeEnvelope(root1);
        Envelope env2 = QueryUtils.getIndexNodeEnvelope(root2);
        if (env1.intersects(env2)) {
            progressListener.worked(1, "Start");
            List<NodeWithEnvelope.Pair> leafPairs = spatialJoin4(root1, root2, env1.intersection(env2));
            progressListener.worked(1, "Done exploring index nodes");
            List<JoinOutput> results = executeSpatialOperations(leafPairs);
            return results.stream();
        } else if (isDisjoint) {
            return getJoinOutputFromIndexPair(root1, root2).stream();
        } else {
            return Stream.empty();
        }
    }

    private List<NodeWithEnvelope.Pair> spatialJoin4(Node node1, Node node2, Envelope intersectionRect) {
        List<NodeWithEnvelope> rtreeNodes1 = QueryUtils.getIndexChildren(node1).stream()
                .filter(nwe -> nwe.envelope.intersects(intersectionRect))
                .collect(Collectors.toList());
        List<NodeWithEnvelope> rtreeNodes2 = QueryUtils.getIndexChildren(node2).stream()
                .filter(nwe -> nwe.envelope.intersects(intersectionRect))
                .collect(Collectors.toList());
        progressListener.updateVisitedIndexCount(rtreeNodes1.size() + rtreeNodes2.size());
        // Sort nodes by the minimum X value of their envelopes
        rtreeNodes1.sort(Comparator.comparingDouble(nwe -> nwe.envelope.getMinX()));
        rtreeNodes2.sort(Comparator.comparingDouble(nwe -> nwe.envelope.getMinX()));

        List<NodeWithEnvelope.Pair> pairs = sortedIntersectionTest(rtreeNodes1, rtreeNodes2);

        List<NodeWithEnvelope.Pair> leafPairs = new ArrayList<>();
        Map<Node, Integer> pinCounts = new HashMap<>();
        for (NodeWithEnvelope.Pair pair : pairs) {
            Node child1 = pair.nwe1.node;
            Node child2 = pair.nwe2.node;
            Envelope childIntersection = pair.nwe1.envelope.intersection(pair.nwe2.envelope);

            if (QueryUtils.nodeIsLeaf(child1) && QueryUtils.nodeIsLeaf(child2)) {
                leafPairs.add(pair);
            } else if (QueryUtils.nodeIsLeaf(child1)) {
                leafPairs.addAll(windowQuery(pair.nwe1, child2, childIntersection, true));
            } else if (QueryUtils.nodeIsLeaf(child2)) {
                leafPairs.addAll(windowQuery(pair.nwe2, child1, childIntersection, false));
            } else {
                incrementPinCount(pinCounts, child1);
                incrementPinCount(pinCounts, child2);
                leafPairs.addAll(spatialJoin4(child1, child2, childIntersection));
                decrementPinCount(pinCounts, child1);
                decrementPinCount(pinCounts, child2);
            }
        }
        return leafPairs;
    }

    private List<NodeWithEnvelope.Pair> windowQuery(NodeWithEnvelope leafNwe, Node dirNode, Envelope intersectionRect, boolean isLeft) {
        return QueryUtils.getIndexChildren(dirNode).stream()
                .filter(nwe -> nwe.envelope.intersects(intersectionRect))
                .filter(dirEntry -> leafNwe.envelope.intersects(dirEntry.envelope))
                .flatMap(dirEntry -> {
                    if (QueryUtils.nodeIsLeaf(dirEntry.node)) {
                        return Stream.of(new NodeWithEnvelope.Pair(isLeft ? leafNwe : dirEntry, isLeft ? dirEntry : leafNwe));
                    } else {
                        return windowQuery(leafNwe, dirEntry.node, dirEntry.envelope.intersection(intersectionRect), isLeft).stream();
                    }
                })
                .collect(Collectors.toList());
    }

    private void incrementPinCount(Map<Node, Integer> pinCounts, Node node) {
        pinCounts.put(node, pinCounts.getOrDefault(node, 0) + 1);
    }

    private void decrementPinCount(Map<Node, Integer> pinCounts, Node node) {
        int count = pinCounts.getOrDefault(node, 0) - 1;
        if (count == 0) {
            pinCounts.remove(node);
        } else {
            pinCounts.put(node, count);
        }
    }

    private List<NodeWithEnvelope.Pair> sortedIntersectionTest(List<NodeWithEnvelope> rtreeNodes1, List<NodeWithEnvelope> rtreeNodes2) {
        List<NodeWithEnvelope.Pair> output = new ArrayList<>();
        int i = 0, j = 0;

        while (i < rtreeNodes1.size() && j < rtreeNodes2.size()) {
            NodeWithEnvelope nwe1 = rtreeNodes1.get(i);
            NodeWithEnvelope nwe2 = rtreeNodes2.get(j);

            if (nwe1.envelope.getMinX() <= nwe2.envelope.getMaxX()) {
                internalLoop(nwe1, j, rtreeNodes2, output);
                i++;
            } else {
                internalLoop(nwe2, i, rtreeNodes1, output);
                j++;
            }
        }
        return output;
    }

    private void internalLoop(NodeWithEnvelope t, int unmarked, List<NodeWithEnvelope> Sseq, List<NodeWithEnvelope.Pair> output) {
        int k = unmarked;
        while (k < Sseq.size() && Sseq.get(k).envelope.getMinX() <= t.envelope.getMaxX()) {
            if (t.envelope.intersects(Sseq.get(k).envelope)) {
                output.add(new NodeWithEnvelope.Pair(t, Sseq.get(k)));
            }
            k++;
        }
    }

    private List<JoinOutput> executeSpatialOperations(List<NodeWithEnvelope.Pair> leafPairs) {
        List<NodeWithGeometry.Pair> pairs = new ArrayList<>();
        for (NodeWithEnvelope.Pair pair : leafPairs) {
            List<NodeWithGeometry> geomNodes1 = QueryUtils.getGeomtryNodes(pair.nwe1.node);
            List<NodeWithGeometry> geomNodes2 = QueryUtils.getGeomtryNodes(pair.nwe2.node);
            List<NodeWithGeometry.Pair> newPairs = geomNodes1.parallelStream()
                    .flatMap(geomNode1 -> geomNodes2.parallelStream()
                            .filter(geomNode2 -> geomNode1.envelope.intersects(geomNode2.envelope))
                            .map(geomNode2 -> new NodeWithGeometry.Pair(geomNode1, geomNode2)))
                    .toList();
            pairs.addAll(newPairs);
        }
        progressListener.worked(1, "Done Setting Candidate Pairs");
        progressListener.updateCandidateGeometryCount(pairs.size());

        List<JoinOutput> results = Collections.synchronizedList(new ArrayList<>());
        executor.executeOperation(pairs.parallelStream().toList()).forEach(results::add);
        progressListener.worked(1, "Done Spatial Operation");

        if (isDisjoint) {
            List<JoinOutput> allResults = getJoinOutputFromIndexPair(root1, root2);
            progressListener.worked(1, "Done get All Pairs");
            Set<String> intersectsResultsSet = results.parallelStream()
                    .map(intersectsOutput -> intersectsOutput.node1.getElementId() + "_" + intersectsOutput.node2.getElementId())
                    .collect(Collectors.toSet());
            results = allResults.parallelStream()
                    .filter(joinOutput -> !intersectsResultsSet.contains(joinOutput.node1.getElementId() + "_" + joinOutput.node2.getElementId()))
                    .collect(Collectors.toList());
        }

        return results;
    }
}
