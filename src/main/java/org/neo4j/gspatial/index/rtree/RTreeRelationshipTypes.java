package org.neo4j.gspatial.index.rtree;

import org.neo4j.graphdb.RelationshipType;


public enum RTreeRelationshipTypes implements RelationshipType {

    RTREE_METADATA,
    RTREE_ROOT,
    RTREE_CHILD,
    RTREE_REFERENCE

}
