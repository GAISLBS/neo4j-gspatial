PROFILE
CALL gspatial.rtree.query.join(['Highway', 'Building'], ['intersects'], true)
YIELD node1, node2

MATCH (m:Building)
  WHERE m.idx = node2.idx
WITH node1, m
MATCH (m)-[:HAS_COLOUR]->(c:Colour)
  WHERE c.name = 'beige'
RETURN DISTINCT node1.idx AS n_idx, m.idx AS m_idx