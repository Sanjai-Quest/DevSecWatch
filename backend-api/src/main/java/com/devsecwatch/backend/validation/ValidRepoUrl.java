package com.devsecwatch.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = RepoUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRepoUrl {
    String message() default "Repository URL must be from github.com, gitlab.com, or bitbucket.org";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
