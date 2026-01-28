package com.devsecwatch.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class GitHubUrlValidator implements ConstraintValidator<GitHubUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.toLowerCase().contains("github.com/");
    }
}
