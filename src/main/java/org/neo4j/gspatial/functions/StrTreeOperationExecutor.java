package org.neo4j.gspatial.functions;

import org.locationtech.jts.index.strtree.STRtree;
import org.neo4j.graphdb.Node;
import org.neo4j.gspatial.constants.SpatialConstants;
import org.neo4j.gspatial.utils.IOUtility;

import java.util.List;
import java.util.stream.Stream;

import static org.neo4j.gspatial.utils.StrTreeUtils.*;

public class StrTreeOperationExecutor {
    private final String INDEX_FILE_PATH;
    private final STRtree spatialIndex;
    private static final String uuid = SpatialConstants.UUIDNAME.getValue();

    public StrTreeOperationExecutor(String spatialSetLabel) {
        this.INDEX_FILE_PATH = spatialSetLabel + "_StrTree.bin";
        this.spatialIndex = loadOrCreateIndex(INDEX_FILE_PATH);
        System.out.printf("Use %s StrTree index%n", spatialSetLabel);
    }

    public Stream<IOUtility.Output> executeOperation(String operationName, List<Node> args) {
        if ("insert".equalsIgnoreCase(operationName)) {
            insertNodes(args);
            spatialIndex.build();
            saveIndex(spatialIndex, INDEX_FILE_PATH);
            return Stream.of(new IOUtility.Output(List.of("Index created and saved successfully.")));
        } else {
            boolean success = removeNodes(args);
            if (success) {
                saveIndex(spatialIndex, INDEX_FILE_PATH);
                return Stream.of(new IOUtility.Output(List.of("Nodes removed from index successfully.")));
            } else {
                return Stream.of(new IOUtility.Output(List.of("Failed to remove some nodes from the index.")));
            }
        }
    }

    private void insertNodes(List<Node> nodes) {
        for (Node node : nodes) {
            spatialIndex.insert(getEnvelope(node), node.getProperty(uuid));
        }
    }

    private boolean removeNodes(List<Node> nodes) {
        for (Node node : nodes) {
            boolean removed = spatialIndex.remove(getEnvelope(node), node.getProperty(uuid));
            if (!removed) {
                return false;
            }
        }
        return true;
    }
}
