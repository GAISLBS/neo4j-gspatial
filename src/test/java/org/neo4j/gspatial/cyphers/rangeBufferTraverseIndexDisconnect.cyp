PROFILE
CALL gspatial.rtree.query.range(['Building', 'Highway', 'LandUse', 'Natural', 'Place', 'Shop'], [
  [-73.985428, 40.748817], 0.1], true)
YIELD node

MATCH (n:Building)
  WHERE n.idx = node.idx
WITH DISTINCT n
MATCH (n)-[:HAS_COLOUR]->(c:Colour)
  WHERE c.name = 'beige'
RETURN n.idx AS n_idx