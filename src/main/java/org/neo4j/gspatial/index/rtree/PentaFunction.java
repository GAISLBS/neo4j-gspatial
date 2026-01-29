package org.neo4j.gspatial.index.rtree;

@FunctionalInterface
public interface PentaFunction<T, U, V, W, X, R> {
    R apply(T t, U u, V v, W w, X x);
}