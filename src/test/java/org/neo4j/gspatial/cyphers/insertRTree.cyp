PROFILE
MATCH (n:Building)
WITH collect(n) AS nList
CALL gspatial.rtree('insert', nList, 'Building') YIELD result
RETURN result