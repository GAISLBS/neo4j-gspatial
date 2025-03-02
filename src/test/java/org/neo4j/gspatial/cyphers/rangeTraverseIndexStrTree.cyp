PROFILE
CALL gspatial.strtree.query.range(['Building', 'Highway', 'LandUse', 'Natural', 'Place', 'Shop'], [-73.995428,
  -73.975428, 40.738817000000004, 40.758817])
YIELD node

MATCH (node)-[:HAS_COLOUR]->(c:Colour)
  WHERE c.name = 'beige'
RETURN node.idx AS n_idx
// result : [252, 619322, 623455]