package com.devsecwatch.backend.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {
    private final int remainingSeconds;

    public RateLimitExceededException(String message, int remainingSeconds) {
        super(message);
        this.remainingSeconds = remainingSeconds;
    }
}
