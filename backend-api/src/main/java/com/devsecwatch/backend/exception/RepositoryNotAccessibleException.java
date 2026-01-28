package com.devsecwatch.backend.exception;

public class RepositoryNotAccessibleException extends RuntimeException {
    public RepositoryNotAccessibleException(String message) {
        super(message);
    }
}
