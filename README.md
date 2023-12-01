# gspatial - Neo4j Spatial Functions
## Overview
'gspatial' is a plugin for the Neo4j database, specifically crafted for handling spatial data.
This plugin is adept at performing a range of spatial operations on geometric objects, which are represented in the Well-Known Text (WKT) format.
Unlike the existing neo4j-spatial plugin, which only supports up to Neo4j version 4.3.3, 'gspatial' is compatible with Neo4j versions 5.0 and above, offering a more user-friendly experience in conducting topological operations without the need for layers.
It harnesses the Java Topology Suite (JTS) to facilitate spatial functions through simplified coding, eliminating the dependency on layers.

However, the current version of 'gspatial' does not support spatial indexing, which may result in slower spatial operations.
Additionally, it only supports the WKT format for spatial objects and is limited to the WGS84 coordinate system.
Furthermore, this plugin has been tested exclusively on Neo4j Desktop version 5.10.0, and its functionality in other versions or environments cannot be guaranteed.

We look forward to the community's contributions in enhancing and evolving this plugin for broader applications and improved performance.

## Installation
To install this plugin into your Neo4j database, follow the steps below:

- Download the gspatial.jar file from /target/spatial-functions-1.0-SNAPSHOT.
- Copy the downloaded jar file to the plugins directory of your Neo4j installation.
- Restart the Neo4j server.

## Features
- SpatialProcedures Class: Provides main procedures to perform spatial operations.
- Various Functions for Spatial Operations: Perform different spatial operations that categorized TOPOLOGICAL Operations, SET Operations, Other Operations.
  - Topological Operations: Perform topological operations such as CONTAINS, COVERS, COVERED_BY, CROSSES, DISJOINT, EQUALS, INTERSECTS, OVERLAPS, TOUCHES, WITHIN. They require two geometries in the WKT format as arguments.
  - Set Operations: Perform set operations such as DIFFERENCE, INTERSECTION, UNION. They also require two geometries in the WKT format as arguments.
  - Other Operations: Perform other operations such as AREA, BUFFER, BOUNDARY, CENTROID, CONVEX_HULL, DIMENSION, DISTANCE, ENVELOPE, LENGTH, SRID.
    - AREA, BOUNDARY, CENTROID, CONVEX_HULL, DIMENSION, ENVELOPE, LENGTH, SRID: These operations require a single geometry in the WKT format as an argument.
    - BUFFER: This operation requires two arguments. The first argument is a geometry in the WKT format and the second argument is a Double representing the buffer distance.
    - DISTANCE: This operation also requires two arguments. Both arguments are geometries in the WKT format. The operation calculates the distance between these two geometries.
- Spatial Data Transformation Utilities: Offers utilities to parse and transform spatial data in WKT format.

## Usage Examples
To perform spatial operations, you must adhere to the specified format.\
"CALL gspatial.operation(operationName, [arg1, arg2]) YIELD result"
where operationName is the name of the operation to be performed,
arg1 and arg2 are the arguments required for the operation,
and result is the result of the operation.
use Several Neo4j's Cypher query language as follows:

Topological Operation Example:
``` Cypher
MATCH (n:NodeType1)
MATCH (m:NodeType2)
CALL gspatial.operation('CONTAINS', [n.geometry, m.geometry]) YIELD result
WITH n, m, result
WHERE n <> m AND result = true
RETURN n.idx, m.idx, result
```

This query finds all pairs of nodes of type NodeType1 and NodeType2 that satisfy the CONTAINS condition.

Set Operation Example:
```Cypher
MATCH (n:NodeType1)
MATCH (m:NodeType2)
CALL gspatial.operation('intersects', [n, m]) YIELD result as intersects_filter
WITH n, m, intersects_filter
WHERE n <> m and intersects_filter = true
CALL gspatial.operation('UNION', [n.geometry, m.geometry]) YIELD result
RETURN n.idx, m.idx, result
```

This query finds all pairs of nodes of type NodeType1 and NodeType2 that satisfy the INTERSECTS condition and returns the UNION of their geometries.
arg1 and arg2 can be either a node if it has property key 'geometry' and its value is in the WKT format.

Other Operation Example:
```Cypher
MATCH (n:NodeType1)
CALL gspatial.operation('AREA', [n.geometry]) YIELD result
RETURN n.idx, result
```
This query calculates the area of each node of type NodeType1.

```Cypher
CALL gspatial.operation('BUFFER', ['POINT (10 10)', 2.0]) YIELD result
RETURN result
```
This query creates a buffer of radius 2.0 around a given point.

## Developer Guide
This plugin is developed using Java 17 and managed via Maven. It can be built as follows:

```bash
mvn clean package
````

## Testing
The plugin includes several JUnit test cases to verify its functionality. These tests can be executed as follows:

```bash
mvn test
```

## License
This plugin is available under the MIT License. The full text of the license can be found in the [LICENSE](LICENSE.txt) file.
