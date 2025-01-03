package org.neo4j.gspatial.index.rtree.query;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.functions.JoinOperationExecutor;
import org.neo4j.gspatial.index.rtree.Envelope;
import org.neo4j.gspatial.index.rtree.ProgressLoggingListener;
import org.neo4j.gspatial.index.rtree.query.QueryUtils.NodeWithEnvelope;
import org.neo4j.gspatial.index.rtree.query.QueryUtils.NodeWithGeometry;
import org.neo4j.gspatial.utils.RtreeUtility;
import org.neo4j.logging.Log;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JoinOld {
    private Transaction tx;
    private Node root1;
    private Node root2;
    private String operationString;
    private Log log;
    private ProgressLoggingListener progressListener;

    public JoinOld(Transaction tx, List<Node> layers, List<Object> args, Log log, ProgressLoggingListener progressListener) {
        this.tx = tx;
        this.log = log;
        this.root1 = QueryUtils.getIndexRoot(layers.get(0));
        this.root2 = QueryUtils.getIndexRoot(layers.get(1));
        this.operationString = (String) args.get(0);
        this.progressListener = progressListener;
    }

    public Stream<RtreeUtility.JoinOutput> query() {
        Envelope env1 = QueryUtils.getIndexNodeEnvelope(root1);
        Envelope env2 = QueryUtils.getIndexNodeEnvelope(root2);
        if (env1.intersects(env2)) {
            progressListener.worked(1, "Start");
            Map<Node, ArrayList<Node>> leafPairs = oldRecursiveSpatialJoin(QueryUtils.getIndexChildren(root1), QueryUtils.getIndexChildren(root2));
            progressListener.worked(1, "Done exploring index nodes");
//            List<Object> results = executeSpatialOperation(leafPairs, new SpatialOperationExecutor(log));

            return executeOperation(leafPairs, new JoinOperationExecutor(log, operationString));
        } else {
            return Stream.empty();
        }
    }

    private Map<Node, ArrayList<Node>> oldRecursiveSpatialJoin(List<NodeWithEnvelope> rtreeNodes1, List<NodeWithEnvelope> rtreeNodes2) {
        Map<Node, ArrayList<Node>> indexPairs = new HashMap<>();
        progressListener.updateVisitedIndexCount(rtreeNodes1.size() + rtreeNodes2.size());

        for (NodeWithEnvelope nwe1 : rtreeNodes1) {
            Envelope env1 = nwe1.envelope;
            for (NodeWithEnvelope nwe2 : rtreeNodes2) {
                Envelope env2 = nwe2.envelope;
                if (env1.intersects(env2)) {
                    if (QueryUtils.nodeIsLeaf(nwe1.node) && QueryUtils.nodeIsLeaf(nwe2.node)) {
                        ArrayList<Node> overlapLeafs = indexPairs.computeIfAbsent(nwe1.node, k -> new ArrayList<>());
                        overlapLeafs.add(nwe2.node);
                    } else if (!QueryUtils.nodeIsLeaf(nwe1.node) && !QueryUtils.nodeIsLeaf(nwe2.node)) {
                        Map<Node, ArrayList<Node>> childResults = oldRecursiveSpatialJoin(
                                QueryUtils.getIndexChildren(nwe1.node), QueryUtils.getIndexChildren(nwe2.node)
                        );
                        childResults.forEach((key, value) -> indexPairs.merge(key, value, (list1, list2) -> {
                            list1.addAll(list2);
                            return list1;
                        }));
                    } else {
                        if (QueryUtils.nodeIsLeaf(nwe1.node)) {
                            Map<Node, ArrayList<Node>> childResults = oldRecursiveSpatialJoin(
                                    Collections.singletonList(nwe1), QueryUtils.getIndexChildren(nwe2.node)
                            );
                            childResults.forEach((key, value) -> indexPairs.merge(key, value, (list1, list2) -> {
                                list1.addAll(list2);
                                return list1;
                            }));
                        } else {
                            Map<Node, ArrayList<Node>> childResults = oldRecursiveSpatialJoin(
                                    QueryUtils.getIndexChildren(nwe1.node), Collections.singletonList(nwe2)
                            );
                            childResults.forEach((key, value) -> indexPairs.merge(key, value, (list1, list2) -> {
                                list1.addAll(list2);
                                return list1;
                            }));
                        }
                    }
                }
            }
        }
        return indexPairs;
    }

//    private List<Object> executeSpatialOperation(Map<Node, ArrayList<Node>> leafPairs, SpatialOperationExecutor executor) {
//        List<Object> results = new ArrayList<>();
//
//        for (Map.Entry<Node, ArrayList<Node>> entry : leafPairs.entrySet()) {
//            Node leaf1 = entry.getKey();
//            ArrayList<Node> leafList2 = entry.getValue();
//
//            List<Object> geomList1 = QueryUtils.getLeafChildren(leaf1);
//            List<Object> geomList2 = new ArrayList<>();
//            for (Node leaf2 : leafList2) {
//                geomList2.addAll(QueryUtils.getLeafChildren(leaf2));
//            }
//            progressListener.updateCandidateGeometryCount(geomList1.size() * geomList2.size());
//            List<List<Object>> rawArgList = Arrays.asList(geomList1, geomList2);
//            Stream<IOUtility.Output> outputStream = executor.executeOperation(operationString, rawArgList);
//            results.addAll(outputStream.flatMap(output -> output.getResult().stream()).toList());
//        }
//        return results;
//    }

    private Stream<RtreeUtility.JoinOutput> executeOperation(Map<Node, ArrayList<Node>> leafPairs, JoinOperationExecutor executor) {
        List<NodeWithGeometry.Pair> pair = leafPairs.entrySet().stream()
                .flatMap(entry -> {
                    Node leaf1 = entry.getKey();
                    List<NodeWithGeometry> geomList2 = entry.getValue().stream()
                            .flatMap(leaf2 -> QueryUtils.getGeomtryNodes(leaf2).stream())
                            .toList();
                    return QueryUtils.getGeomtryNodes(leaf1).stream()
                            .flatMap(geomNode1 -> geomList2.stream()
                                    .map(geomNode2 -> new NodeWithGeometry.Pair(geomNode1, geomNode2)));
                })
                .collect(Collectors.toList());

        progressListener.worked(1, "Done Setting Candidate Pairs");
        progressListener.updateCandidateGeometryCount(pair.size());
        return executor.executeOperation(pair);
    }

}
