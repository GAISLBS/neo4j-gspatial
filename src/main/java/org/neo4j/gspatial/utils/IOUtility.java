package org.neo4j.gspatial.utils;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.graphdb.Node;
import org.neo4j.gspatial.constants.SpatialOperationConstants;
import org.neo4j.gspatial.functions.HashTreeFunction;

import java.util.ArrayList;
import java.util.List;

public class IOUtility {
    private static final String BUFFER_OPERATION = "BUFFER";

    public static List<Object> argsConverter(String operationName, List<Object> args) {
        List<Object> processedArgs = new ArrayList<>();
        boolean topologyOperation = SpatialOperationConstants.isTopologyOperation(operationName);
        for (Object arg : args) {
            processedArgs.add(convertArg(arg, operationName));
        }
        return topologyOperation ?
                HashTreeFunction.findHashRelation(processedArgs, operationName) :
                processedArgs;
    }

    private static Object convertArg(Object arg, String operationName) {
        if (arg instanceof Node) {
            return convertNode((Node) arg, operationName);
        } else if (arg instanceof String) {
            return GeometryUtility.parseWKT((String) arg);
        } else if (arg instanceof Double && BUFFER_OPERATION.equals(operationName)) {
            return arg;
        }
        return arg;
    }

    private static Object convertNode(Node node, String operationName) {
        boolean topologyOperation = SpatialOperationConstants.isTopologyOperation(operationName);
        if (topologyOperation) {
            return node;
        } else if (node.hasProperty("geometry")) {
            return GeometryUtility.parseWKT(node.getProperty("geometry").toString());
        }
        throw new IllegalArgumentException("Node does not have a 'geometry' property");
    }

    public static Object convertResult(Object result) {
        return result instanceof Geometry ? result.toString() : result;
    }

    public static class Output {
        public Object result;
        public Output(Object result) {
            this.result = result;
        }
    }
}
