package org.neo4j.gspatial.index.rtree;

/**
 * Classes that implement this interface will be notified of units of work done,
 * and can therefor be used for progress bars or console logging or similar activities.
 */
public interface Listener {

    void begin(int unitsOfWork);

    void worked(int workedSinceLastNotification);

    void done();

}
