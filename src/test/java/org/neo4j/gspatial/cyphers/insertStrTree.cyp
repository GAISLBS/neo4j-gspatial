MATCH (n:Building)
WITH collect(n) AS nodes
CALL gspatial.strtree('insert', nodes, 'Building') YIELD result
RETURN result
