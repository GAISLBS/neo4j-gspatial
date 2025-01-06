PROFILE
CALL gspatial.rtree.query.range(['Building', 'Highway', 'LandUse', 'Natural', 'Place', 'Shop'], [-73.995428, -73.975428,
  40.738817000000004, 40.758817])
YIELD node
RETURN node.idx AS n_idx