package org.neo4j.gspatial.exceptions;

import org.neo4j.logging.Log;

public class ExceptionHandler {
    public static <T extends Throwable> void handleException(T e, Log log, String message) {
        log.error(message, e);
        throw new RuntimeException(message + ": " + e.getMessage(), e);
    }
}
