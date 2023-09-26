package org.neo4j.gspatial.functions;

import org.neo4j.graphdb.Node;
import org.neo4j.gspatial.utils.IOUtility.Output;

import java.util.List;
import java.util.Map;

public class HashTreeExecuter {
    private final HashTreeFunction hashTreeFunction;

    public HashTreeExecuter(HashTreeFunction hashTreeFunction) {
        this.hashTreeFunction = hashTreeFunction;
    }

    public Output handleSingleHashTreeOperation(Map<String, Object> data) {
        String label = (String) data.get("label");
        long geometryIdx = ((Number) data.get("geometryIdx")).longValue();
        String wkt = (String) data.get("geom");

        Node hashNode = hashTreeFunction.setHashTree(label, geometryIdx, wkt);

        return hashNode == null ?
                new Output("Geometry node not found for index: " + geometryIdx) :
                new Output("Successfully created or updated hash node and linked for index: " + geometryIdx);
    }

}
