PROFILE
CALL gspatial.rtree.query.join(['Highway', 'Building'], ['intersects'])
YIELD node1, node2

MATCH (node2)-[:HAS_COLOUR]->(c:Colour)
  WHERE c.name = 'beige'
RETURN DISTINCT node1.idx AS n_idx, node2.idx AS m_idx