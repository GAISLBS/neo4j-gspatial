PROFILE
CALL gspatial.rtree.query.knn(['Building', 'Highway', 'LandUse', 'Natural', 'Place', 'Shop'], [[-73.985428, 40.748817],
  5])
YIELD node, distance
RETURN node.idx AS n_idx, distance
  ORDER BY distance ASC