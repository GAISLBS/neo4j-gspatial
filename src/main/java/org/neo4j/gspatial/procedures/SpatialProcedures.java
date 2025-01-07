package org.neo4j.gspatial.procedures;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.gspatial.functions.*;
import org.neo4j.gspatial.utils.IOUtility;
import org.neo4j.gspatial.utils.IOUtility.Output;
import org.neo4j.gspatial.utils.ProcedureLoggingListener;
import org.neo4j.gspatial.utils.RtreeUtility;
import org.neo4j.logging.Level;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.neo4j.gspatial.utils.IOUtility.setSpatialConstants;

public class SpatialProcedures {
    @Context
    public Log log;
    @Context
    public Transaction tx;

    @Procedure(value = "gspatial.setConfig")
    @Description("Set the spatial configuration")
    public void setConfig(@Name(value = "geomFormat", defaultValue = "WKT") String geomFormat,
                          @Name(value = "uuidName", defaultValue = "idx") String uuidName,
                          @Name(value = "geomName", defaultValue = "geometry") String geomName,
                          @Name(value = "srid", defaultValue = "4326") String srid) {
        setSpatialConstants(geomFormat, uuidName, geomName, srid);
    }

    @Procedure(value = "gspatial.operation")
    @Description("Generic method for spatial operations")
    public Stream<Output> operation(@Name("operation") String operationName,
                                    @Name("argsList") List<List<Object>> argsList) {
        return executeWithLogging(() -> {
            SpatialOperationExecutor operationExecutor = new SpatialOperationExecutor(log, tx);
            return operationExecutor.executeOperation(operationName, argsList);
        });
    }

    @Procedure(value = "gspatial.simpleOperation")
    @Description("Generic method for spatial operations")
    public Stream<Output> simpleOperation(@Name("operation") String operationName,
                                    @Name("argNameA") String argNameA,
                                    @Name(value = "argNameB", defaultValue = "") Object argNameB) {
        return executeWithLogging(() -> {
            SpatialOperationExecutor operationExecutor = new SpatialOperationExecutor(log, tx);
            if (argNameB.toString().isBlank()) {
                return operationExecutor.executeSimpleOperation(operationName, argNameA, null);
            }
            return operationExecutor.executeSimpleOperation(operationName, argNameA, argNameB);
        });
    }


    @Procedure(value = "gspatial.rtree", mode = Mode.WRITE)
    @Description("CRUD method for R-Tree operations")
    public Stream<RtreeUtility.Output> rtree(@Name("rtree") String operationName,
                                             @Name("args") List<Object> args,
                                             @Name("spatialSetLabel") String spatialSetLabel,
                                             @Name(value = "disconnect", defaultValue = "false") Boolean disconnect) {
        return executeWithLogging(() -> {
            RtreeOperationExecutor indexOperationExecutor = new RtreeOperationExecutor(log, tx, spatialSetLabel, disconnect);
            return indexOperationExecutor.executeOperation(operationName, args);
        });
    }

    @Procedure(value = "gspatial.rtree.query.knn", mode = Mode.READ)
    @Description("Query method for Knn operations")
    public Stream<RtreeUtility.KnnOutput> knnQuery(@Name(value = "SpatialSetLabels", defaultValue = "[]") List<String> spatialSetLabels,
                                                   @Name(value = "args", defaultValue = "[]") List<Object> args,
                                                   @Name(value = "disconnect", defaultValue = "false") Boolean disconnect,
                                                   @Name(value = "cypherQuery", defaultValue = "") String cypherQuery) {
        return executeWithLogging(() -> {
            RtreeQueryExecutor indexQueryExecutor = new RtreeQueryExecutor(log, tx, spatialSetLabels, cypherQuery, disconnect);
            return indexQueryExecutor.executeOperation("knn", args).map(RtreeUtility.KnnOutput.class::cast);
        });
    }

    @Procedure(value = "gspatial.rtree.query.range", mode = Mode.READ)
    @Description("Query method for Range operations")
    public Stream<RtreeUtility.RangeOutput> rangeQuery(@Name(value = "SpatialSetLabels", defaultValue = "[]") List<String> spatialSetLabels,
                                                       @Name(value = "args", defaultValue = "[]") List<Object> args,
                                                       @Name(value = "disconnect", defaultValue = "false") Boolean disconnect,
                                                       @Name(value = "cypherQuery", defaultValue = "") String cypherQuery) {
        return executeWithLogging(() -> {
            RtreeQueryExecutor indexQueryExecutor = new RtreeQueryExecutor(log, tx, spatialSetLabels, cypherQuery, disconnect);
            return indexQueryExecutor.executeOperation("range", args).map(RtreeUtility.RangeOutput.class::cast);
        });
    }

    @Procedure(value = "gspatial.rtree.query.join", mode = Mode.READ)
    @Description("Query method for Join operations")
    public Stream<RtreeUtility.JoinOutput> joinQuery(@Name(value = "SpatialSetLabels", defaultValue = "[]") List<String> spatialSetLabels,
                                                     @Name(value = "args", defaultValue = "[]") List<Object> args,
                                                     @Name(value = "disconnect", defaultValue = "false") Boolean disconnect,
                                                     @Name(value = "cypherQuery", defaultValue = "") String cypherQuery) {
        return executeWithLogging(() -> {
            RtreeQueryExecutor indexQueryExecutor = new RtreeQueryExecutor(log, tx, spatialSetLabels, cypherQuery, disconnect);
            return indexQueryExecutor.executeOperation("join", args).map(RtreeUtility.JoinOutput.class::cast);
        });
    }

    @Procedure(value = "gspatial.strtree", mode = Mode.READ)
    @Description("CRUD method for STR-Tree(JTS) operations")
    public Stream<IOUtility.Output> StrTree(@Name("rtree") String operationName,
                                            @Name("args") List<Node> args,
                                            @Name("spatialSetLabel") String spatialSetLabel) {
        return executeWithLogging(() -> {
            StrTreeOperationExecutor executor = new StrTreeOperationExecutor(spatialSetLabel);
            return executor.executeOperation(operationName, args);
        });
    }

    @Procedure(value = "gspatial.strtree.query.range", mode = Mode.READ)
    @Description("STR-Tree(JTS) Query method for range operations")
    public Stream<RtreeUtility.RangeOutput> StrTreeRangeQuery(@Name("spatialSetLabel") List<String> spatialSetLabels,
                                                              @Name("args") List<Object> args) {
        return executeWithLogging(() -> {
            StrTreeQueryExecutor executor = new StrTreeQueryExecutor(tx, spatialSetLabels);
            return executor.executeQuery(args);
        });
    }

    private <T> Stream<T> executeWithLogging(Supplier<Stream<T>> action) {
        ProcedureLoggingListener loggingListener = new ProcedureLoggingListener(log, Level.INFO);
//        ProcedureLoggingListener loggingListener = new ProcedureLoggingListener(System.out);
        loggingListener.start();
        Stream<T> result = action.get();
        loggingListener.end();
        return result;
    }
}
