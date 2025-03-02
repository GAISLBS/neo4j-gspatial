package org.neo4j.gspatial.index.rtree.query;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.gspatial.index.rtree.*;
import org.neo4j.gspatial.index.rtree.filter.SearchCoveredByEnvelope;
import org.neo4j.gspatial.index.rtree.filter.SearchFilter;
import org.neo4j.gspatial.utils.RtreeUtility.RangeOutput;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RangeOld {
    private final Transaction tx;
    private final Envelope searchEnvelope;
    private final List<Node> roots;
    private final RTreeMonitor monitor;
    private final ProgressLoggingListener progressListener;

    public RangeOld(Transaction tx, List<Node> layers, List<Object> args, String cypherQuery, ProgressLoggingListener progressListener) {
        this.tx = tx;
        this.roots = QueryUtils.getIndexRoot(layers);
        this.searchEnvelope = decodeReferenceEnvelope(args);
        this.monitor = new RTreeMonitor();
        this.progressListener = progressListener;
    }

    private Envelope decodeReferenceEnvelope(List<Object> args) {
        if (args.size() == 2) {
            if (args.get(0) instanceof Node) {
                return new EnvelopeDecoderFromJtsGeometry("geometry").bufferEnvelope((Node) args.get(0), (Double) args.get(1));
            } else if (args.get(0) instanceof ArrayList<?> && ((ArrayList<?>) args.get(0)).get(0) instanceof Double) {
                ArrayList<Double> list = (ArrayList<Double>) args.get(0);
                return new EnvelopeDecoderFromJtsGeometry("geometry").bufferEnvelope(list, (Double) args.get(1));
            } else {
                throw new IllegalArgumentException("Invalid argument");
            }
        } else if (args.size() == 4) {
            return new Envelope((Double) args.get(0), (Double) args.get(1), (Double) args.get(2), (Double) args.get(3));
        } else {
            throw new IllegalArgumentException("Invalid argument");
        }
    }

    public Stream<RangeOutput> query() {
        SearchFilter indexSearchFilter = new SearchCoveredByEnvelope(new EnvelopeDecoderFromBbox("bbox"), searchEnvelope);
        SearchFilter geomSearchFilter = new SearchCoveredByEnvelope(new EnvelopeDecoderFromJtsGeometry("geometry"), searchEnvelope);
        return searchIndex(tx, indexSearchFilter, geomSearchFilter);

    }

    private class SearchEvaluator implements Evaluator {
        private final SearchFilter indexFilter;
        private final SearchFilter geomFilter;
        private final Transaction tx;

        public SearchEvaluator(Transaction tx, SearchFilter indexFilter, SearchFilter geomFilter) {
            this.tx = tx;
            this.indexFilter = indexFilter;
            this.geomFilter = geomFilter;
        }

        @Override
        public Evaluation evaluate(Path path) {
            Relationship rel = path.lastRelationship();
            Node node = path.endNode();
            if (rel == null) {
                return Evaluation.EXCLUDE_AND_CONTINUE;
            } else if (rel.isType(RTreeRelationshipTypes.RTREE_CHILD)) {
                boolean shouldContinue = indexFilter.needsToVisit(QueryUtils.getIndexNodeEnvelope(node));
                if (shouldContinue) monitor.matchedTreeNode(path.length(), node);
                monitor.addCase(shouldContinue ? "Index Matches" : "Index Does NOT Match");
                progressListener.updateVisitedIndexCount(1);
                return shouldContinue ?
                        Evaluation.EXCLUDE_AND_CONTINUE :
                        Evaluation.EXCLUDE_AND_PRUNE;
            } else if (rel.isType(RTreeRelationshipTypes.RTREE_REFERENCE)) {
                boolean found = geomFilter.geometryMatches(tx, node);
                monitor.addCase(found ? "Geometry Matches" : "Geometry Does NOT Match");
                if (found) monitor.setHeight(path.length());
                progressListener.updateCandidateGeometryCount(1);
                return found ?
                        Evaluation.INCLUDE_AND_PRUNE :
                        Evaluation.EXCLUDE_AND_PRUNE;
            }
            return null;
        }
    }

    public Stream<RangeOutput> searchIndex(Transaction tx, SearchFilter indexFilter, SearchFilter geomFilter) {
        SearchEvaluator searchEvaluator = new SearchEvaluator(tx, indexFilter, geomFilter);
        MonoDirectionalTraversalDescription traversal = new MonoDirectionalTraversalDescription();
        TraversalDescription td = traversal
                .depthFirst()
                .relationships(RTreeRelationshipTypes.RTREE_CHILD, Direction.OUTGOING)
                .relationships(RTreeRelationshipTypes.RTREE_REFERENCE, Direction.OUTGOING)
                .evaluator(searchEvaluator);
        Traverser traverser = td.traverse(roots);
        return StreamSupport.stream(traverser.nodes().spliterator(), false).map(RangeOutput::new);
    }
}
