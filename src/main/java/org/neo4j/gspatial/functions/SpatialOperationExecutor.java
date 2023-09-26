package org.neo4j.gspatial.functions;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.gspatial.constants.SpatialOperationConstants.SpatialOperation;
import org.neo4j.gspatial.utils.IOUtility;
import org.neo4j.logging.Log;

import java.util.List;
import java.util.stream.Stream;


public class SpatialOperationExecutor {
    private final Log log;

    public SpatialOperationExecutor(Log log) {
        this.log = log;
    }

    public Stream<IOUtility.Output> executeOperation(String operationName, List<Object> rawArgs) {
        log.info(String.format("Running gspatial.%s with arguments: %s", operationName, rawArgs));
        List<Object> convertedArgs = IOUtility.argsConverter(operationName, rawArgs);
        if (convertedArgs.size() > 2 && Boolean.FALSE.equals(convertedArgs.get(2))) {
            return Stream.of(new IOUtility.Output(false));
        }
        SpatialOperation operation = SpatialOperation.valueOf(operationName.toUpperCase());
        Object result = operation.execute(convertedArgs);
        if (result instanceof Geometry && ((Geometry) result).isEmpty()) {
            return Stream.empty();
        }
        return Stream.of(new IOUtility.Output(IOUtility.convertResult(result)));
    }
}
