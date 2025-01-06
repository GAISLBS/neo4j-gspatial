WITH
  'POLYGON ((126.9776747745794 37.48940647166388, 127.003822009184 37.48940647166388, 127.003822009184 37.5139237487866, 126.9776747745794 37.5139237487866, 126.9776747745794 37.48940647166388))'
  AS Query_Polygon,
  [126.9776747745794, 127.003822009184] AS Qx, [37.48940647166388, 37.5139237487866] AS Qy
MATCH (:ApartmentRTree)-[:RTREE_ROOT]->()-[r:RTREE_CHILD*]->()-[:RTREE_REFERENCE]->(n:Apartment)
  WHERE ALL(rel IN r
    WHERE
    NOT(rel.max_x < Qx[0] OR rel.min_x > Qx[1]) AND
    NOT(rel.max_y < Qy[0] OR rel.min_y > Qy[1])
  )
WITH collect(DISTINCT(n)) AS nodes, Query_Polygon, Qx, Qy, n
CALL gspatial.operation('within', [nodes, [Query_Polygon]]) YIELD result
UNWIND result AS res
WITH res[0] AS n_idx, res[2] AS result, n
  WHERE result = true
WITH n
MATCH (n)-[:HAS_TYPE]->()-[:TRADE]->(c:Contract)
  WHERE c.price < 70000
RETURN DISTINCT (n).idx