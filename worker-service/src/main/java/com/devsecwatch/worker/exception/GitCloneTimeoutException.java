package com.devsecwatch.worker.exception;

public class GitCloneTimeoutException extends RuntimeException {
    public GitCloneTimeoutException(String message) {
        super(message);
    }
}
