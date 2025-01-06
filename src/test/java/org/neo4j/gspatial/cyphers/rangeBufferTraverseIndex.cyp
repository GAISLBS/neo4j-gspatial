PROFILE
CALL gspatial.rtree.query.range(['Building', 'Highway', 'LandUse', 'Natural', 'Place', 'Shop'], [
  [-73.985428, 40.748817], 0.1])
YIELD node

MATCH (node)-[:HAS_COLOUR]->(c:Colour)
  WHERE c.name = 'beige'
RETURN node.idx AS n_idx
