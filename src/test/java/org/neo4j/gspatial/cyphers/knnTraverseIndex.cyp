PROFILE
CALL gspatial.rtree.query.knn(['Building', 'Highway', 'LandUse', 'Natural', 'Place', 'Shop'], [[-73.985428, 40.748817],
  125])
YIELD node, distance

MATCH (node)-[:HAS_COLOUR]->(c:Colour)
  WHERE c.name = 'beige'
RETURN node.idx AS n_idx, distance
  ORDER BY distance ASC