package com.devsecwatch.worker.exception;

public class OutOfMemoryException extends ScanExecutionException {
    public OutOfMemoryException(String message) {
        super(message);
    }

    public OutOfMemoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
