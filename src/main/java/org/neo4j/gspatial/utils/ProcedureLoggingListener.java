package org.neo4j.gspatial.utils;

import org.neo4j.logging.Level;
import org.neo4j.logging.Log;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class ProcedureLoggingListener {

    private final LogMethod logMethod;
    private long startTime;
    private MemoryUsage startHeapMemoryUsage;
    private MemoryUsage startNonHeapMemoryUsage;

    public interface LogMethod {
        void log(String message);
    }

    public ProcedureLoggingListener(PrintStream out) {
        this.logMethod = out::println;
    }

    public ProcedureLoggingListener(Log log, Level level) {
        this.logMethod = message -> {
            switch (level) {
                case DEBUG:
                    log.debug(message);
                    break;
                case ERROR:
                    log.error(message);
                    break;
                case INFO:
                    log.info(message);
                    break;
                case WARN:
                    log.warn(message);
                    break;
                default:
                    break;
            }
        };
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.startHeapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        this.startNonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        logMethod.log("Procedure started.");
    }

    public void end() {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage endHeapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage endNonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

        long heapMemoryUsed = endHeapMemoryUsage.getUsed() - startHeapMemoryUsage.getUsed();
        long nonHeapMemoryUsed = endNonHeapMemoryUsage.getUsed() - startNonHeapMemoryUsage.getUsed();
        long totalMemoryUsed = heapMemoryUsed + nonHeapMemoryUsed;

        String message = String.format("Procedure completed. Execution time: %d ms. Heap memory used: %d bytes. Non-heap memory used: %d bytes. Total memory used: %d bytes.",
                duration, heapMemoryUsed, nonHeapMemoryUsed, totalMemoryUsed);
        logMethod.log(message);
    }
}
