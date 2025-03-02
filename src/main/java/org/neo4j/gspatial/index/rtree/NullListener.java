package org.neo4j.gspatial.index.rtree;

/**
 * This listener ignores all notifications of progress. It is useful when progress is not necessary.
 */
public class NullListener implements Listener {

    // Public methods

    @Override
    public void begin(int unitsOfWork) {
    }

    @Override
    public void worked(int workedSinceLastNotification) {
    }

    @Override
    public void done() {
    }

}
