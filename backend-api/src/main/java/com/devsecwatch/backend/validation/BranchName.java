package com.devsecwatch.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = BranchNameValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BranchName {
    String message() default "Branch name must be alphanumeric with dash, underscore, or slash";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
