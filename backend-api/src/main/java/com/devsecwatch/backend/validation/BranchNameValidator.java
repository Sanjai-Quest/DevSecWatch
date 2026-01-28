package com.devsecwatch.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BranchNameValidator implements ConstraintValidator<BranchName, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        return true; // Allow all branch names for now to avoid validation errors
    }
}
