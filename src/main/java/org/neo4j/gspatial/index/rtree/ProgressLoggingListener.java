package org.neo4j.gspatial.index.rtree;

import org.neo4j.logging.Level;
import org.neo4j.logging.Log;

import java.io.PrintStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * This listener logs percentage progress to the specified PrintStream or Logger based on a timer,
 * never logging more frequently than the specified number of ms.
 */
public class ProgressLoggingListener implements Listener {

    private final ProgressLog out;
    private final String name;
    private long lastLogTime = 0L;
    private int totalUnits = 0;
    private int workedSoFar = 0;
    private boolean enabled = false;
    private long timeWait = 1000;
    private long startTime;
    private int totalGeometryCombination;
    private int totalIndexCombination;
    private int visitedIndexCount = 0;
    private int candidateGeometryCount = 0;

    public interface ProgressLog {
        void log(String line);
    }

    public ProgressLoggingListener(String name, final PrintStream out, int[] counts) {
        this.name = name;
        this.out = out::println;
        this.totalIndexCombination = counts[0];
        this.totalGeometryCombination = counts[1];
    }

    public ProgressLoggingListener(String name, Log log, Level level, int[] counts) {
        this.name = name;
        this.totalIndexCombination = counts[0];
        this.totalGeometryCombination = counts[1];
        this.out = line ->
        {
            switch (level) {
                case DEBUG:
                    log.debug(line);
                case ERROR:
                    log.error(line);
                case INFO:
                    log.info(line);
                case WARN:
                    log.warn(line);
                default:
                    break;
            }
        };
    }

    public ProgressLoggingListener setTimeWait(long ms) {
        this.timeWait = ms;
        return this;
    }

    @Override
    public void begin(int unitsOfWork) {
        this.totalUnits = unitsOfWork;
        this.workedSoFar = 0;
        this.lastLogTime = 0L;
        this.startTime = System.currentTimeMillis();
        try {
            this.enabled = true;
            out.log("Starting " + name);
        } catch (Exception e) {
            System.err.println("Failed to write to output - disabling progress logger: " + e.getMessage());
            this.enabled = false;
        }
    }

    public void worked(int workedSinceLastNotification, String message) {
        this.workedSoFar += workedSinceLastNotification;
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        String elapsedTimeStr = formatDuration(elapsedTime);
        logNoMoreThanOnceASecond("Running (" + elapsedTimeStr + ")" +
                ", Index Count: " + visitedIndexCount + "/" + totalIndexCombination
                + ", Geometry Count: " + candidateGeometryCount + "/" + totalGeometryCombination
                + ", Message: " + message);
    }

    @Override
    public void worked(int workedSinceLastNotification) {
        worked(workedSinceLastNotification, "");
    }

    @Override
    public void done() {
        this.workedSoFar = this.totalUnits;
        this.lastLogTime = 0L;
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        String durationStr = formatDuration(duration);
        logNoMoreThanOnceASecond("Completed in " + durationStr +
                ", Index Count: " + visitedIndexCount + "/" + totalIndexCombination
                + ", Geometry Count: " + candidateGeometryCount + "/" + totalGeometryCombination);
    }

    private String formatDuration(long duration) {
        return String.format("%02d:%02d:%02d.%03d",
                TimeUnit.MILLISECONDS.toHours(duration),
                TimeUnit.MILLISECONDS.toMinutes(duration) % 60,
                TimeUnit.MILLISECONDS.toSeconds(duration) % 60,
                duration % 1000);
    }

    private void logNoMoreThanOnceASecond(String action) {
        long now = System.currentTimeMillis();
        if (enabled && now - lastLogTime > timeWait) {
            if (totalUnits > 0) {
                out.log(percText() + " (" + workedSoFar + "/" + totalUnits + ") - " + action + " " + name);
            } else {
                out.log(action + " " + name);
            }
            this.lastLogTime = now;
        }
    }

    private String percText() {
        if (totalUnits > 0) {
            return String.format(Locale.ENGLISH, "%.2f", 100.0 * workedSoFar / totalUnits);
        } else {
            return "NaN";
        }
    }

    public void updateCandidateGeometryCount(int count) {
        this.candidateGeometryCount += count;
    }

    public void updateVisitedIndexCount(int count) {
        this.visitedIndexCount += count;
    }
}
