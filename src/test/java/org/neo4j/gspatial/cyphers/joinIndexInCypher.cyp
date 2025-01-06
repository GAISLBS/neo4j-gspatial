PROFILE
MATCH (layer1:LandCoverMap1mRTree)-[:RTREE_ROOT]->(root1),
      (layer2:AgendaAreaRTree)-[:RTREE_ROOT]->(root2)
CALL {
WITH root1, root2
MATCH p1 = (root1)-[:RTREE_CHILD*]->(index1),
      p2 = (root2)-[:RTREE_CHILD*]->(index2)
  WHERE ALL(rel1 IN relationships(p1)
    WHERE
    ALL(rel2 IN relationships(p2)
      WHERE
      NOT(rel1.max_x < rel2.min_x OR rel1.min_x > rel2.max_x OR
      rel1.max_y < rel2.min_y OR rel1.min_y > rel2.max_y)
    )
  )
RETURN index1, index2
}
MATCH (index1)-[:RTREE_REFERENCE]->(lcm),
      (index2)-[:RTREE_REFERENCE]->(aa)
WITH collect(DISTINCT lcm) AS aList, collect(DISTINCT aa) AS aaList
CALL gspatial.operation('within', [aList, aaList]) YIELD result
UNWIND result AS res
WITH res[0] AS landcovermap_index, res[1] AS agendaarea_index, res[2] AS result
  WHERE res[2] = true
RETURN landcovermap_index, agendaarea_index