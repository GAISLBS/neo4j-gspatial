package org.neo4j.gspatial.functions;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.gspatial.constants.SpatialOperationConstants.SpatialOperation;
import org.neo4j.gspatial.utils.IOUtility;
import org.neo4j.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.apache.commons.lang3.math.NumberUtils.max;

/**
 * This class is responsible for executing spatial operations.
 * It uses the SpatialOperation enum to perform the operation and convert the result to the appropriate format.
 */
public class SpatialOperationExecutor {
    private final Log log;

    /**
     * Constructs a new SpatialOperationExecutor with the given log.
     *
     * @param log the log to use for logging information about the operations
     */
    public SpatialOperationExecutor(Log log) {
        this.log = log;
    }

    /**
     * Executes the given spatial operation with the given arguments.
     * The arguments are first converted to the appropriate format using the IOUtility.argsConverter method.
     * The operation is then performed using the SpatialOperation enum.
     * If the result of the operation is an empty Geometry, an empty stream is returned.
     * Otherwise, the result is converted to the appropriate format using the IOUtility.convertResult method and returned as a stream.
     *
     * @param operationName the name of the operation to perform
     * @param rawArgs       the raw arguments for the operation
     * @return a stream containing the result of the operation
     */
    public Stream<IOUtility.Output> executeOperation(String operationName, List<Object> rawArgs) {
        log.info(String.format("Running gspatial.%s with arguments: %s", operationName, rawArgs));
        List<Object> convertedArgs = IOUtility.argsConverter(operationName, rawArgs);
        SpatialOperation operation = SpatialOperation.valueOf(operationName.toUpperCase());
        Object result = operation.execute(convertedArgs);
        if (result instanceof Geometry && ((Geometry) result).isEmpty()) {
            return Stream.empty();
        }
        return Stream.of(new IOUtility.Output(IOUtility.convertResult(result)));
    }

    public void executeOperations(String operationName, List<List<Object>> rawArgsList) {
        Stream.Builder<IOUtility.Output> outputBuilder = Stream.builder();

        List<Object> nList = rawArgsList.get(0);
        List<Object> mList = rawArgsList.get(1);

        int len = max(nList.size(), mList.size());

        for (int i = 0; i < len; i++) {
            Object nowN = null;
            Object nowM = null;

            try {
                nowN = nList.get(i);
            } catch (IndexOutOfBoundsException e) {
                continue;
            }

            try {
                nowM = mList.get(i);
            } catch (IndexOutOfBoundsException e) {
                continue;
            }

            List<Object>rawArgs = List.of(nowN, nowM);
            executeOperation(operationName, rawArgs);
        }
    }
}
