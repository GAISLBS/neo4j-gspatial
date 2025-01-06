PROFILE
WITH
  'POLYGON ((126.9776747745794 37.48940647166388, 127.003822009184 37.48940647166388, 127.003822009184 37.5139237487866, 126.9776747745794 37.5139237487866, 126.9776747745794 37.48940647166388))'
  AS Query_Polygon
MATCH (n)-[:HAS_TYPE]->()-[:TRADE]->(c:Contract)
  WHERE c.price < 70000
WITH collect(n) AS nodes, Query_Polygon
CALL gspatial.operation('within', [nodes, [Query_Polygon]]) YIELD result
UNWIND result AS res
WITH res[0] AS n_idx, res[2] AS result
  WHERE result = true
RETURN n_idx