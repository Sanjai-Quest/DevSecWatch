package com.devsecwatch.backend.exception;

public class ScanNotFoundException extends RuntimeException {
    public ScanNotFoundException(String message) {
        super(message);
    }
}
