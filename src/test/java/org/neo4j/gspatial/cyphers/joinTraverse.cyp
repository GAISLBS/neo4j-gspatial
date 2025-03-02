PROFILE
MATCH (a:Apartment)-[:HAS_TYPE]->()-[:TRADE]->(c:Contract)
  WHERE c.price < 70000
WITH collect(DISTINCT(a)) AS aList
MATCH (aa:AgendaArea)
WITH aList, collect(aa) AS aaList
CALL gspatial.operation('within', [aList, aaList]) YIELD result
UNWIND result AS res
WITH res[0] AS n_idx, res[1] AS m_idx, res[2] AS result
  WHERE result = true
RETURN n_idx, m_idx