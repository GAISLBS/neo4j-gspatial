WITH 'POINT(-73.985428 40.748817)' AS Query_Point
CALL gspatial.operation('buffer', [[Query_Point], [0.01]]) YIELD result
UNWIND result AS res
WITH res[1] AS Query_Circle
CALL gspatial.operation('envelope', [[Query_Circle]]) YIELD result
UNWIND result AS res
WITH res[1] AS Query_Polygon
RETURN Query_Polygon