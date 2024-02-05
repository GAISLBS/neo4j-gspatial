package org.neo4j.gspatial.utils;

import org.locationtech.jts.geom.Geometry;
import org.neo4j.graphdb.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides utility methods to convert input and output for spatial operations.
 * It includes methods for converting arguments and results to the appropriate format.
 */
public class IOUtility {
    private static final String BUFFER_OPERATION = "BUFFER";

    /**
     * Converts the given arguments to the appropriate format for spatial operations.
     * Each argument is converted using the convertArg method.
     *
     * @param args          the arguments for the operation
     * @return the converted arguments
     */
    public static List<Object> argsConverter(List<Object> args, String geomFormat) {
        List<Object> processedArgs = new ArrayList<>();
        for (Object arg : args) {
            processedArgs.add(convertArg(arg, geomFormat));
        }
        return processedArgs;
    }

    /**
     * Converts the given argument for spatial operations based on its type.
     * If the argument is a Node, it is converted using the convertNode method.
     * If the argument is a String, it is converted to a Geometry object using the GeometryUtility.parseWKT method.
     * If the argument is a Double and the operation is BUFFER, the argument is returned as is.
     *
     * @param arg           the argument to convert
     * @return the converted argument
     */
    private static Object convertArg(Object arg, String geomFormat) {
        if (arg instanceof Node) {
            return convertNode((Node) arg, geomFormat);
        }
        if (arg instanceof String) {
            if (geomFormat.equals("WKB")) {
                return GeometryUtility.parseWKB((String) arg);
            }
            if (geomFormat.equals("WKT")){
                return GeometryUtility.parseWKT((String) arg);
            }
        }
        return arg;
    }

    /**
     * Converts the given Node to a Geometry object.
     * If the Node has a 'geometry' property, the property value is converted to a Geometry object using the GeometryUtility.parseWKT method.
     *
     * @param node the Node to convert
     * @return the converted Geometry object
     * @throws IllegalArgumentException if the Node does not have a 'geometry' property
     */
    private static Object convertNode(Node node, String geomFormat) {
        if (node.hasProperty("geometry")) {
            if (geomFormat.equals("WKB"))
                return GeometryUtility.parseWKB(node.getProperty("geometry").toString());
            else if (geomFormat.equals("WKT"))
                return GeometryUtility.parseWKT(node.getProperty("geometry").toString());
        }
        throw new IllegalArgumentException("Node does not have a 'geometry' property");
    }

    /**
     * Converts the result of a spatial operation to the appropriate format.
     * If the result is a Geometry object, the result is converted to a String using the toString method.
     *
     * @param result the result to convert
     * @return the converted result
     */
    public static Object convertResult(Object result) {
        return result instanceof Geometry ? result.toString() : result;
    }

    /**
     * This class represents the output of a spatial operation.
     * It includes a single field 'result' that contains the result of the operation.
     */
    public static class Output {
        public List<List<Object>> result;
        public List<Object> resultList;
        public List<Object> indexList;

        public Output(List<Object> resultList, List<Object> indexList) {
            this.resultList = resultList;
            this.indexList = indexList;
            this.result = List.of(resultList, indexList);
        }
    }
}
