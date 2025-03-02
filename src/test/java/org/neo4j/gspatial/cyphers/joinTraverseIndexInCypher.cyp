MATCH (:ApartmentRTree)-[:RTREE_ROOT]->()-[r1:RTREE_CHILD*]->()-[:RTREE_REFERENCE]->(a:Apartment)-[:HAS_TYPE]->()
        -[:TRADE]->(c:Contract)
MATCH (:AgendaAreaRTree)-[:RTREE_ROOT]->()-[r2:RTREE_CHILD*]->()-[:RTREE_REFERENCE]->(aa:AgendaArea)
  WHERE ALL(rel1 IN r1
    WHERE ALL(rel2 IN r2
      WHERE
      NOT(rel1.max_x < rel2.min_x OR rel1.min_x > rel2.max_x) AND
      NOT(rel1.max_y < rel2.min_y OR rel1.min_y > rel2.max_y)
    )) AND c.price < 70000
WITH collect(DISTINCT(a)) AS aList, collect(DISTINCT(aa)) AS aaList
CALL gspatial.operation('within', [aList, aaList]) YIELD result
UNWIND result AS res
WITH res[0] AS apt_index, res[1] AS agendaarea_index, res[2] AS result
  WHERE result = true
RETURN apt_index, agendaarea_index