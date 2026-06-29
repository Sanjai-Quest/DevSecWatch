package com.devsecwatch.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
@Getter
public class RateLimitExceededException extends RuntimeException {
    private final int remainingSeconds;

    public RateLimitExceededException(String message, int remainingSeconds) {
        super(message);
        this.remainingSeconds = remainingSeconds;
    }
}
