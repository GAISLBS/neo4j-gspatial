package org.neo4j.gspatial.functions;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.graphdb.Node;
import org.neo4j.gspatial.constants.SpatialConstants;
import org.neo4j.gspatial.constants.SpatialOperationConstants;
import org.neo4j.gspatial.constants.SpatialOperationConstants.SpatialOperation;
import org.neo4j.gspatial.utils.IOUtility;
import org.neo4j.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SpatialOperationExecutor {
    private final Log log;

    public SpatialOperationExecutor(Log log) {
        this.log = log;
    }

    public Stream<IOUtility.Output> executeOperation(String operationName, List<List<Object>> rawArgList) {
        String operationNameUpper = operationName.toUpperCase();
//        log.info(String.format("Running gspatial.%s", operationNameUpper));

        if (SpatialOperationConstants.isSingOperation(operationNameUpper)) {
            return executeSingleOperation(operationNameUpper, rawArgList.get(0));
        } else if (SpatialOperationConstants.isSingParameterOperation(operationNameUpper)) {
            return executeSingleParamOperation(operationNameUpper, rawArgList.get(0), rawArgList.get(1));
        } else if (SpatialOperationConstants.isDualParameterOperation(operationNameUpper) || SpatialOperationConstants.isTopologyOperation(operationNameUpper)) {
            return executeDualOperation(operationNameUpper, rawArgList);
        } else if (SpatialOperationConstants.isSetOperation(operationNameUpper)) {
            return executeSetOperation(operationNameUpper, rawArgList);
        } else {
            log.error(String.format("Operation %s is not supported", operationNameUpper));
            return Stream.empty();
        }
    }

    private Stream<IOUtility.Output> executeSingleOperation(String operationName, List<Object> rawArgs) {
        return executeOperation(operationName, rawArgs, (operation, convertedArg) -> operation.execute(List.of(convertedArg)));
    }

    private Stream<IOUtility.Output> executeSingleParamOperation(String operationName, List<Object> rawArgs, List<Object> param) {
        Double paramValue = (Double) param.get(0);
        return executeOperation(operationName, rawArgs, (operation, convertedArg) -> operation.execute(List.of(convertedArg, paramValue)));
    }

    private Stream<IOUtility.Output> executeDualOperation(String operationName, List<List<Object>> rawArgList) {
        return executeOperation(operationName, rawArgList.get(0), rawArgList.get(1), (operation, convertedNArg, convertedMArg) -> operation.execute(List.of(convertedNArg, convertedMArg)), false);
    }

    private Stream<IOUtility.Output> executeSetOperation(String operationName, List<List<Object>> rawArgList) {
        return executeOperation(operationName, rawArgList.get(0), rawArgList.get(1), (operation, convertedNArg, convertedMArg) -> {
            Boolean intersectionResult = (Boolean) SpatialOperation.INTERSECTS.execute(List.of(convertedNArg, convertedMArg));
            if (!intersectionResult) {
                return null;
            }
            return operation.execute(List.of(convertedNArg, convertedMArg));
        }, true);
    }

    private Stream<IOUtility.Output> executeOperation(String operationName, List<Object> rawArgs, OperationExecutor executor) {
        List<Object> resultList = new ArrayList<>();
        List<Geometry> convertedArgs = IOUtility.argsConverter(rawArgs);
        SpatialOperation operation = SpatialOperation.valueOf(operationName.toUpperCase());

        for (int i = 0; i < convertedArgs.size(); i++) {
            Object convertedArg = convertedArgs.get(i);
            Object result = executor.execute(operation, convertedArg);

            if (isValidResult(result)) {
                resultList.add(createOutputEntry(rawArgs.get(i), result, i));
            }
        }
        return Stream.of(new IOUtility.Output(resultList));
    }

    private Stream<IOUtility.Output> executeOperation(String operationName, List<Object> nList, List<Object> mList, DualOperationExecutor executor, boolean isSetOperation) {
        List<Object> resultList = new ArrayList<>();
        List<Geometry> convertedNList = IOUtility.argsConverter(nList);
        List<Geometry> convertedMList = IOUtility.argsConverter(mList);
        SpatialOperation operation = SpatialOperation.valueOf(operationName);

        for (int i = 0; i < convertedNList.size(); i++) {
            Object nArg = nList.get(i);
            for (int j = 0; j < convertedMList.size(); j++) {
                Object mArg = mList.get(j);
                Object result = executor.execute(operation, convertedNList.get(i), convertedMList.get(j));

                if (isValidResult(result)) {
                    resultList.add(createOutputEntry(nArg, mArg, result, i, j));
                }
            }
        }
        return Stream.of(new IOUtility.Output(resultList));
    }

    private boolean isValidResult(Object result) {
        return result != null && !(result instanceof Geometry && ((Geometry) result).isEmpty());
    }

    private Object determineIndex(Object arg, int index) {
        if (arg instanceof Node) {
            return ((Node) arg).getProperty(SpatialConstants.UUIDNAME.getValue());
        } else {
            return index;
        }
    }

    private List<Object> createOutputEntry(Object arg, Object result, int index) {
        Object idx = determineIndex(arg, index);
        return List.of(idx, IOUtility.convertResult(result));
    }

    private List<Object> createOutputEntry(Object nArg, Object mArg, Object result, int nIndex, int mIndex) {
        Object n_idx = determineIndex(nArg, nIndex);
        Object m_idx = determineIndex(mArg, mIndex);
        return List.of(n_idx, m_idx, IOUtility.convertResult(result));
    }

    @FunctionalInterface
    private interface OperationExecutor {
        Object execute(SpatialOperation operation, Object convertedArg);
    }

    @FunctionalInterface
    public interface DualOperationExecutor {
        Object execute(SpatialOperation operation, Geometry convertedNArg, Geometry convertedMArg);
    }
}
