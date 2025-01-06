PROFILE
CALL gspatial.rtree.query.knn(['Building', 'Highway', 'LandUse', 'Natural', 'Place', 'Shop'], [[-73.985428, 40.748817],
  125], true)
YIELD node, distance

MATCH (n:Building)
  WHERE n.idx = node.idx
WITH n, distance
MATCH (n)-[:HAS_COLOUR]->(c:Colour)
  WHERE c.name = 'beige'
RETURN n.idx AS n_idx, distance
  ORDER BY distance ASC