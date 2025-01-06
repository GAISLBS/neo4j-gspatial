PROFILE
CALL gspatial.rtree.query.join(['Highway', 'Building'], ['within'])
YIELD node1, node2
RETURN node1.idx AS n_idx, node2.idx AS m_idx