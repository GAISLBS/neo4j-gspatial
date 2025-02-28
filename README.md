# gspatial - Neo4j Advanced Spatial Plugin

## Overview
'gspatial' is a plugin for Neo4j that implements the [SGIR-Tree](https://www.mdpi.com/2220-9964/13/10/346) (Subgraph Integrated R-Tree) structure for efficient spatial data handling in graph databases. Unlike existing Neo4j spatial plugins that only support operations between whole layer node sets and specified node sets and their disconnected index approach, gspatial is specifically designed for offering improved spatial operation and query processing through direct integration with the graph structure.

Key features of the gspatial approach include:
- Support for spatial operations between any arbitary node sets
- Direct integration of R-Tree spatial indexing within the graph database structure
- Efficient spatial query processing through subgraph-based index organization
- Seamless combination of spatial searches and graph traversals
- Improved performance for complex spatial-graph queries
- Support for various spatial data types (points, polylines, polygons)

The plugin leverages the Java Topology Suite ([JTS](https://locationtech.github.io/jts/)) for spatial operations while maintaining minimal dependencies and simplified coding requirements.
So, you can check the Geometry(org.locationtech.jts.geom.Geometry) methods of JTS for more information about the spatial operations.

## Prerequisites
- Neo4j Desktop version 5.22.0 for using the plugin
- Java 17, Maven for building the plugin from source

## Installation
1. Download the neo4j-gspatial.jar file from release
2. Copy the downloaded jar file to the plugins directory of your Neo4j installation
   (you can find the plugins directory in the Neo4j Desktop settings
   or default path is .Neo4jDesktop/relate-data/dbmss/dbms-id/plugins/)
3. Restart your Neo4j server

## Configuration
Before using any spatial operations or indices, you must configure the gspatial plugin settings for your database. This step is mandatory and should match your data structure when restarting the Neo4j server:

```cypher
CALL gspatial.setConfig(
    "WKT",           // geometry format (required, default is WKT and can be changed to WKB)
    "idx",           // UUID property name for spatial objects (required, default is 'idx' and can be changed to any unique property name)
    "geometry",      // geometry property name (required, default is 'geometry' and can be changed to any property name)
    "4326"          // SRID (required, default is 4326 that corresponds to WGS84 coordinate system)
)
```

## Features
### Direct Spatial Operations
You can perform spatial operations directly on geometric objects without using indices. These operations are useful for simple queries or smaller datasets.

#### Topological Operations
```cypher
MATCH (n:NodeType1)
WITH n, COLLECT(n) AS n_list

MATCH (m:NodeType2)
WITH n, m, n_list, COLLECT(m) AS m_list

CALL gspatial.operation('CONTAINS', [n_list, m_list]) YIELD n, m, result

WHERE n <> m AND result = true
RETURN n.idx, m.idx, result;
```
This query finds all pairs of nodes of type NodeType1 and NodeType2 that satisfy the CONTAINS condition.

Available operations:
- CONTAINS, COVERS, COVERED_BY, CROSSES, DISJOINT
- EQUALS, INTERSECTS, OVERLAPS, TOUCHES, WITHIN

#### Set Operations
```cypher
MATCH (n:NodeType1)
WITH COLLECT(n) AS n_list

MATCH (m:NodeType2)
WITH n_list, COLLECT(m) AS m_list

CALL gspatial.operation('UNION', [n_list, m_list]) YIELD n, m, result

RETURN n.idx, m.idx, result
```

This query finds all pairs of nodes of type NodeType1 and NodeType2 that satisfy the INTERSECTS condition and returns the UNION of their geometries.
arg1 and arg2 can be either a node if it has property key 'geometry' and its value is in the WKT format.

Available operations:
- DIFFERENCE
- INTERSECTION
- UNION

#### Other Operations
```cypher
// Area calculation
MATCH (n:NodeType1)
WITH COLLECT(n) AS n_list
CALL gspatial.operation('AREA', [n_list]) YIELD n, result
RETURN n.idx, result
```
This query calculates the area of each node of type NodeType1.

```cypher
// Buffer creation
MATCH (n:NodeType1)
WITH n, COLLECT(n) AS n_list
CALL gspatial.operation('BUFFER', [n_list, [0.01]]) YIELD n, result
RETURN n.idx, result;
```
This query creates a buffer of radius 0.01 around a given point.

Available operations:
- AREA, BBOX, BUFFER, BOUNDARY, CENTROID
- CONVEX_HULL, DIMENSION, DISTANCE
- ENVELOPE, LENGTH, SRID

### Direct Label-based Spatial Operations
You can perform spatial operations directly on geometric objects without using indices. These operations are useful for simple queries or smaller datasets.

#### Topological Operations
```cypher
CALL gspatial.labelOperation('CONTAINS', 'NodeType1', 'NodeType2') YIELD n, m, result
WHERE n <> m AND result = true
RETURN n.idx, m.idx, result;
```
This query finds all pairs of nodes of type NodeType1 and NodeType2 that satisfy the CONTAINS condition.

Available operations:
- CONTAINS, COVERS, COVERED_BY, CROSSES, DISJOINT
- EQUALS, INTERSECTS, OVERLAPS, TOUCHES, WITHIN

#### Set Operations
```cypher
CALL gspatial.labelOperation('UNION', 'NodeType1', 'NodeType2') YIELD n, m, result
RETURN n.idx, m.idx, result
```

This query finds all pairs of nodes of type NodeType1 and NodeType2 that satisfy the INTERSECTS condition and returns the UNION of their geometries.
arg1 and arg2 can be either a node if it has property key 'geometry' and its value is in the WKT format.

Available operations:
- DIFFERENCE
- INTERSECTION
- UNION

#### Other Operations
```cypher
// Area calculation
CALL gspatial.labelOperation('AREA', 'NodeType1') YIELD n, result
RETURN n.idx, result
```
This query calculates the area of each node of type NodeType1.

```cypher
// Buffer creation
CALL gspatial.labelOperation('BUFFER', 'NodeType1', 0.01) YIELD n, result
RETURN n.idx, result;
```
This query creates a buffer of radius 0.01 around a given point.

Available operations:
- AREA, BBOX, BUFFER, BOUNDARY, CENTROID
- CONVEX_HULL, DIMENSION, DISTANCE
- ENVELOPE, LENGTH, SRID

### SGIR-Tree Spatial Index Operations
You can create a simple spatial index for a specific node type using the following operations:

#### Creating or Update Index
```cypher
MATCH(n:NodeType1)
WITH COLLECT(n) AS nodes
CALL gspatial.rtree("insert", nodes, "NodeType1") YIELD result
RETURN result
```

#### Delete Index
When your spatial data changes, you need to update the index:
```cypher
// Delete specific nodes from index
MATCH (n:NodeType1)
WHERE n.uuid IN ["uuid1", "uuid2"]
WITH COLLECT(n) AS delete_nodes
CALL gspatial.rtree("delete", delete_nodes, "NodeType1") YIELD result
RETURN result

// Delete entire index
CALL gspatial.rtree("deleteAll", [], "NodeType1") YIELD result
RETURN result
```

#### Spatial Range Queries
Two types of range queries are supported:

1. Bounding Box Query:
```cypher
CALL gspatial.rtree.query.range(
    ["NodeType1", "NodeType2"],
    [126.9776747745794, 127.003822009184, 37.48940647166388, 37.5139237487866]
)
YIELD node
RETURN node.uuid
```

2. Buffer Query (circular range):
```cypher
CALL gspatial.rtree.query.range(
    ["NodeType1", "NodeType2"],
    [[127.003822009184, 37.5139237487866], 0.01]  // center point and radius for buffer
)
YIELD node

MATCH (node)-[:HAS_COLOUR]->(c:Colour)
  WHERE c.name = 'beige'
RETURN node.uuid
```

#### KNN Query
```cypher
CALL gspatial.rtree.query.knn(
    ["NodeType1", "NodeType2"],
    [[127.003822009184, 37.48940647166388], 5]  // point and k value
)
YIELD node, distance
RETURN node.uuid AS n_uuid, distance
  ORDER BY distance ASC
```

#### Spatial Join Query
```cypher
CALL gspatial.rtree.query.join(
    ["NodeType1", "NodeType2"],
    ["intersects"]  // spatial topology operation
)
YIELD node1, node2

MATCH (node2)-[:HAS_COLOUR]->(c:Colour)
  WHERE c.name = 'beige'
RETURN DISTINCT node1.idx AS n_idx, node2.idx AS m_idx
```

## Building from Source
```bash
mvn clean package
```

## Testing
```bash
mvn test
```

The test suite includes comprehensive tests for:
- R-tree operations (insert, delete)
- Spatial queries (KNN, range, join)
- Index maintenance and optimization
- Direct spatial operations

## Performance
Based on experimental results, the SGIR-Tree implementation shows significant improvements over traditional approaches:
- Up to 24x faster query execution compared to external spatial indices
- Reduced memory usage for complex spatial-graph queries
- Efficient handling of large-scale spatial datasets

## Limitations
- Currently supports only WKT format for spatial objects
- Limited to WGS84 coordinate system
- Tested primarily on Neo4j Desktop version 5.22.0

## Contributing
Contributions are welcome! Please feel free to submit pull requests or create issues for bugs and feature requests.

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE.txt) file for details.

This project is a modification of [Neo4j Spatial](https://github.com/neo4j-contrib/spatial), which is licensed under the GNU General Public License v3.0.
