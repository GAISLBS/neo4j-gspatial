PROFILE
CALL gspatial.rtree.query.knn(['Apartment', 'AgendaArea', 'LandCoverMap1m'], [[127.003822009184, 37.48940647166388], 5])
YIELD node, distance
RETURN node.idx AS n_idx, distance
  ORDER BY distance ASC