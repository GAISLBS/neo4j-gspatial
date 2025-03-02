PROFILE
CALL gspatial.strtree.query.range(['Apartment', 'AgendaArea', 'LandCoverMap1m'], [126.9776747745794, 127.003822009184,
  37.48940647166388, 37.5139237487866])
YIELD node
RETURN node.idx AS n_idx