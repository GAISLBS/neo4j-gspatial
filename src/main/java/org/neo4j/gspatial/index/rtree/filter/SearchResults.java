package org.neo4j.gspatial.index.rtree.filter;

import org.neo4j.graphdb.Node;

import java.util.Iterator;

public class SearchResults implements Iterable<Node> {
    private final Iterable<Node> traverser;
    private int count = -1;

    public SearchResults(Iterable<Node> traverser) {
        this.traverser = traverser;
    }

    @Override
    public Iterator<Node> iterator() {
        return traverser.iterator();
    }

    public int count() {
        if (count < 0) {
            count = 0;
            for (@SuppressWarnings("unused")
            Node node : this) {
                count++;
            }
        }
        return count;
    }
}
