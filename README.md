# gspatial - Neo4j Spatial Functions Plugin
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

- Download the neo4j-gspatial.jar file from /target/ or release.
- Copy the downloaded jar file to the plugins directory of your Neo4j installation.
- Restart the Neo4j server.

## Features
- SpatialProcedures Class: Provides main procedures to perform spatial operations.
- Various Functions for Spatial Operations: Perform different spatial operations that categorized TOPOLOGICAL Operations, SET Operations, Other Operations.
  - Topological Operations: Perform topological operations such as CONTAINS, COVERS, COVERED_BY, CROSSES, DISJOINT, EQUALS, INTERSECTS, OVERLAPS, TOUCHES, WITHIN. They require two geometries in the WKT format as arguments.
  - Distance Operations: Perform distance operations such as DISTANCE. They require two geometries in the WKT format as arguments.
  - Set Operations: Perform set operations such as DIFFERENCE, INTERSECTION, UNION. They also require two geometries in the WKT format as arguments.
  - Other Operations: Perform other operations such as AREA, BUFFER, BOUNDARY, CENTROID, CONVEX_HULL, DIMENSION, DISTANCE, ENVELOPE, LENGTH, SRID.
    - AREA, BOUNDARY, CENTROID, CONVEX_HULL, DIMENSION, ENVELOPE, LENGTH, SRID: These operations require a single geometry in the WKT format as an argument.
    - BUFFER: This operation requires two arguments. The first argument is a geometry in the WKT format and the second argument is a Double representing the buffer distance.
    - DISTANCE: This operation also requires two arguments. Both arguments are geometries in the WKT format. The operation calculates the distance between these two geometries.
- Spatial Data Transformation Utilities: Offers utilities to parse and transform spatial data in WKT format.

## Usage Examples
To perform spatial operations, you must adhere to the specified format.\
`"CALL gspatial.operation(operationName, [argList1, argList2], geomFormat) YIELD result"`
where operationName is the name of the operation to be performed,
argList1 and argList2 are the argument lists required for the operation,
geomFormat is the format of the geometry (WKT or WKB. WKT by default),
and result is the list consisted with `resultList` and `argList1` and `argList2`.
`resultList` consists of results of the operation.
use Several Neo4j's Cypher query language as follows:

Topological Operation Example:
``` Cypher
MATCH (n:NodeType1)
MATCH (m:NodeType2)

WITH COLLECT(n) as n_list, COLLECT(m) as m_list
CALL gspatial.operation('CONTAINS', [n_list, m_list], 'WKT') YIELD result

UNWIND result AS res
WITH res[1] AS n, res[2] AS m

WHERE n <> m
RETURN n.idx, m.idx
```

This query finds all pairs of nodes of type NodeType1 and NodeType2 that satisfy the CONTAINS condition.

Set Operation Example:
```Cypher
MATCH (n:NodeType1)
MATCH (m:NodeType2)

WITH COLLECT(n) AS n_list, COLLECT(m) AS m_list                      
CALL gspatial.operation('UNION', [n_list, m_list]) YIELD result   
               
UNWIND result AS res
WITH res[1] AS n, res[2] AS m, res[0] AS result
WHERE n <> m
         
RETURN n.idx, m.idx, results
```

This query finds all pairs of nodes of type NodeType1 and NodeType2 that satisfy the INTERSECTS condition and returns the UNION of their geometries.
arg1 and arg2 can be either a node if it has property key 'geometry' and its value is in the WKT format.

Other Operation Example:
```Cypher
MATCH (n:NodeType1)
WITH n, collect(n.geometry) AS geometries
CALL gspatial.operation('AREA', [geometries]) YIELD result

UNWIND result AS res
WITH n, res[0] AS result

RETURN n.idx, result
```
This query calculates the area of each node of type NodeType1.

```Cypher
CALL gspatial.operation('BUFFER', [['010100000000000000000024400000000000002440'], [2.0]], 'WKB') YIELD result
UNWIND result AS res
WITH res[0] AS result

RETURN result;
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
