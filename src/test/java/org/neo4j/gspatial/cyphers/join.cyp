PROFILE
MATCH (n:Shop)
WITH collect(n) AS nList
MATCH (m:Building)
WITH nList, collect(m) AS mList
CALL gspatial.operation('within', [nList, mList]) YIELD result
UNWIND result AS res
WITH res[0] AS n_idx, res[1] AS m_idx, res[2] AS result
  WHERE result = true
RETURN n_idx, m_idx