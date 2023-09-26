package org.neo4j.gspatial.procedures;

import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.functions.HashTreeExecuter;
import org.neo4j.gspatial.functions.HashTreeFunction;
import org.neo4j.gspatial.functions.SpatialOperationExecutor;
import org.neo4j.gspatial.utils.IOUtility.Output;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SpatialProcedures {
    @Context
    public Log log;
    @Context
    public Transaction tx;
    private static SpatialOperationExecutor operationExecutor;

    @Procedure(value = "gspatial.operation")
    @Description("Generic method for spatial operations")
    public Stream<Output> operation(@Name("operation") String operationName, @Name("args") List<Object> args) {
        if (operationExecutor == null) {
            operationExecutor = new SpatialOperationExecutor(log);
        }
        return operationExecutor.executeOperation(operationName, args);
    }

    @Procedure(value = "gspatial.setHashTree", mode = Mode.WRITE)
    @Description("Create new nodes with geohashes and link them to the geometry nodes")
    public Stream<Output> setHashTrees(@Name("dataList") List<Map<String, Object>> dataList) {
        HashTreeFunction hashTreeManager = new HashTreeFunction(tx);
        HashTreeExecuter handler = new HashTreeExecuter(hashTreeManager);
        return dataList.stream().map(handler::handleSingleHashTreeOperation).toList().stream();
    }
}
