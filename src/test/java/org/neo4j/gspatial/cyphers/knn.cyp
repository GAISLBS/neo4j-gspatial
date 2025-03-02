PROFILE
WITH 'POINT(127.003822009184 37.48940647166388)' AS Query_Point
MATCH (n)
  WHERE n:Apartment OR n:AgendaArea OR n:LandCoverMap1m
WITH collect(n) AS nodes, Query_Point
CALL gspatial.operation('distance', [[Query_Point], nodes]) YIELD result
UNWIND result AS res
WITH res
  ORDER BY res[2] ASC
  LIMIT 5
RETURN res[1] AS n_idx, res[2] AS distance
