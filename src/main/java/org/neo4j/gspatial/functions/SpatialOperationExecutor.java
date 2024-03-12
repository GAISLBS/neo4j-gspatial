package org.neo4j.gspatial.functions;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.gspatial.constants.SpatialOperationConstants.SpatialOperation;
import org.neo4j.gspatial.utils.IOUtility;
import org.neo4j.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.apache.commons.lang3.math.NumberUtils.max;
import static org.neo4j.gspatial.constants.SpatialOperationConstants.isSetOperation;

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
     * @param rawArgList    the raw arguments for the operation
     * @return a stream containing the result of the operation
     */
    public Stream<IOUtility.Output> executeOperation(String operationName, List<List<Object>> rawArgList, String geomFormat) {
        return executeDualOperation(operationName, rawArgList, geomFormat);
//        if (rawArgList.size() == 1) {
//            return executeSingleOperation(operationName, rawArgList.get(0), geomFormat);
//        }
//        else {
//            return executeDualOperation(operationName, rawArgList, geomFormat);
//        }
    }

//    private Stream<IOUtility.Output> executeSingleOperation(String operationName, List<Object> rawArgs, String geomFormat) {
//        List<Object> resultList = new ArrayList<>();
//        List<Object> indexList = new ArrayList<>();
//
//        for (int i = 0; i < rawArgs.size(); i++) {
//            List<Object> rawArg = List.of(rawArgs.get(i));
//            log.info(String.format("Running gspatial.%s with arguments: %s", operationName, rawArg));
//            List<Object> convertedArgs = IOUtility.argsConverter(rawArg, geomFormat);
//            SpatialOperation operation = SpatialOperation.valueOf(operationName.toUpperCase());
//            Object result = operation.execute(convertedArgs);
//
//            if (result instanceof Geometry && ((Geometry) result).isEmpty()) {
//                continue;
//            }
//
//            resultList.add(IOUtility.convertResult(result));
//
//            if (result instanceof Boolean && (Boolean) result) {
//                indexList.add(i);
//            }
//        }
//        return Stream.of(new IOUtility.Output(resultList, indexList));
//    }

//    private Stream<IOUtility.Output> executeDualOperation(String operationName, List<List<Object>> rawArgList, String geomFormat) {
//        boolean hasIndexList = false;
//        List<Object> resultList = new ArrayList<>();
//        List<Object> indexList = new ArrayList<>();
//        String operationNameUpper = operationName.toUpperCase();
//
//        if (isSetOperation(operationNameUpper)) {
//            rawArgList = executeIntersectsOperation(rawArgList, geomFormat);
//            indexList = rawArgList.get(2);
//            hasIndexList = true;
//        }
//
//        List<Object> nList = rawArgList.get(0);
//        List<Object> mList = rawArgList.get(1);
//
//        for (int i = 0; i < nList.size(); i++) {
//            List<Object> rawArgs = List.of(nList.get(i), mList.get(i));
//            log.info(String.format("Running gspatial.%s with arguments: %s", operationNameUpper, rawArgs));
//            List<Object> convertedArgs = IOUtility.argsConverter(rawArgs, geomFormat);
//            SpatialOperation operation = SpatialOperation.valueOf(operationNameUpper);
//            Object result = operation.execute(convertedArgs);
//
//            if (result instanceof Geometry && ((Geometry) result).isEmpty()) {
//                continue;
//            }
//
//            resultList.add(IOUtility.convertResult(result));
//
//            if (result instanceof Boolean && (Boolean) result && !hasIndexList) {
//                indexList.add(i);
//            }
//        }
//
//        return Stream.of(new IOUtility.Output(resultList, indexList));
//    }

    private Stream<IOUtility.Output> executeDualOperation(String operationName, List<List<Object>> rawArgList, String geomFormat) {
        List<List<Object>> resultList = new ArrayList<>();
        String operationNameUpper = operationName.toUpperCase();

        if (isSetOperation(operationNameUpper)) {
            rawArgList = executeIntersectsOperation(rawArgList, geomFormat);
        }

        List<Object> nList = rawArgList.get(0);
        List<Object> mList = rawArgList.get(1);

        for (int i = 0; i < nList.size(); i++) {
            List<Object> rawArgs = List.of(nList.get(i), mList.get(i));
            log.info(String.format("Running gspatial.%s with arguments: %s", operationNameUpper, rawArgs));
            List<Object> convertedArgs = IOUtility.argsConverter(rawArgs, geomFormat);
            SpatialOperation operation = SpatialOperation.valueOf(operationNameUpper);
            Object result = operation.execute(convertedArgs);

            if (result instanceof Geometry && ((Geometry) result).isEmpty()) {
                continue;
            }

            resultList.add(List.of(nList.get(i), mList.get(i), IOUtility.convertResult(result)));

            //true인 경우에만 하는 작업은 뺐음.. 뭘 했더라?
        }

        return Stream.of(new IOUtility.Output(resultList));
    }

    private List<List<Object>> executeIntersectsOperation(List<List<Object>> rawArgList, String geomFormat) {
        List<Object> newNList = new ArrayList<>();
        List<Object> newMList = new ArrayList<>();
        List<Object> indexList = new ArrayList<>();

        List<Object> nList = rawArgList.get(0);
        List<Object> mList = rawArgList.get(1);

        for (int i = 0; i < nList.size(); i++) {
            Object n = nList.get(i);
            Object m = mList.get(i);
            if (n.equals(m)) {
                continue;
            }
            List<Object> rawArgs = List.of(n, m);
            log.info(String.format("Running gspatial.INTERSECTS with arguments: %s", rawArgs));
            List<Object> convertedArgs = IOUtility.argsConverter(rawArgs, geomFormat);
            SpatialOperation operation = SpatialOperation.INTERSECTS;
            Object result = operation.execute(convertedArgs);

            if (result instanceof Boolean && (Boolean) result) {
                newNList.add(n);
                newMList.add(m);
                indexList.add(i);
            }
        }

        return List.of(newNList, newMList, indexList);
    }
}
