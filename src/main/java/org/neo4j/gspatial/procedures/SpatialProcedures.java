package org.neo4j.gspatial.procedures;

import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.functions.SpatialOperationExecutor;
import org.neo4j.gspatial.utils.IOUtility;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.List;
import java.util.stream.Stream;

/**
 * This class is responsible for executing spatial procedures.
 * It uses the SpatialOperationExecutor to perform the operation.
 */
public class SpatialProcedures {
    @Context
    public Log log;
    @Context
    public Transaction tx;
    private static SpatialOperationExecutor operationExecutor;

    /**
     * Executes the given spatial operation with the given arguments.
     * The operation is performed using the SpatialOperationExecutor.
     * The result of the operation is returned as a stream.
     *
     * @param operationName the name of the operation to perform
     * @param argsList      the arguments for the operation
     * @return a stream containing the result of the operation
     */
    @Procedure(value = "gspatial.operation")
    @Description("Generic method for spatial operations")
    public Stream<IOUtility.Output> operation(@Name("operation") String operationName, @Name("argsList") List<List<Object>> argsList, @Name(value = "geomType", defaultValue = "WKT") String geomType) {
        geomType = geomType.toUpperCase();
        if (!geomType.equals("WKB") && !geomType.equals("WKT")) {
            throw new IllegalArgumentException("Invalid geomType. Must be either 'WKB' or 'WKT'");
        }
        if (operationExecutor == null) {
            operationExecutor = new SpatialOperationExecutor(log);
        }
        return operationExecutor.executeOperation(operationName, argsList, geomType);
    }
}
