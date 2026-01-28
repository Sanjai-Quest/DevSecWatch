package com.devsecwatch.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = GitHubUrlValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GitHubUrl {
    String message() default "Must be a valid public GitHub repository URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
