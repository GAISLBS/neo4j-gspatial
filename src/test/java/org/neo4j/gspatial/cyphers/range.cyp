PROFILE
WITH
  'POLYGON ((-73.995428 40.738817000000004, -73.995428 40.758817, -73.975428 40.758817, -73.975428 40.738817000000004, -73.995428 40.738817000000004))'
  AS Query_Polygon
MATCH (n)
  WHERE n:Building OR n:Highway OR n:LandUse OR n:Natural OR n:Place OR n:Shop
WITH collect(n) AS nodes, Query_Polygon
CALL gspatial.operation('within', [nodes, [Query_Polygon]]) YIELD result
UNWIND result AS res
WITH res[0] AS n_idx, res[2] AS result
  WHERE result = true
RETURN n_idx