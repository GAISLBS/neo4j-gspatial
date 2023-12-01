package org.neo4j.gspatial.exceptions;

import org.neo4j.logging.Log;

/**
 * This class is responsible for handling exceptions.
 * It logs the exception and throws a new RuntimeException with the given message and the original exception.
 */
public class ExceptionHandler {
    /**
     * Handles the given exception.
     * Logs the exception and throws a new RuntimeException with the given message and the original exception.
     *
     * @param e       the exception to handle
     * @param log     the log to use for logging the exception
     * @param message the message to include in the new RuntimeException
     * @throws RuntimeException a new RuntimeException with the given message and the original exception
     */
    public static <T extends Throwable> void handleException(T e, Log log, String message) {
        log.error(message, e);
        throw new RuntimeException(message + ": " + e.getMessage(), e);
    }
}
