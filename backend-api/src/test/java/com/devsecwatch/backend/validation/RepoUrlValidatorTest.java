package com.devsecwatch.backend.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RepoUrlValidatorTest {

    private RepoUrlValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @BeforeEach
    public void setup() {
        validator = new RepoUrlValidator();
        context = mock(ConstraintValidatorContext.class);
        builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    }

    @Test
    public void testValidGitHubUrl() {
        // github.com/user/repo -> valid
        assertTrue(validator.isValid("https://github.com/user/repo", context));
        assertTrue(validator.isValid("http://github.com/user/repo", context));
    }

    @Test
    public void testInvalidLinkLocalUrl() {
        // http://169.254.169.254/latest -> invalid
        assertFalse(validator.isValid("http://169.254.169.254/latest", context));
    }

    @Test
    public void testInvalidRfc1918Url() {
        // http://192.168.1.1/repo -> invalid
        assertFalse(validator.isValid("http://192.168.1.1/repo", context));
        assertFalse(validator.isValid("https://10.0.0.5/repo", context));
        assertFalse(validator.isValid("http://172.16.0.1/repo", context));
    }

    @Test
    public void testInvalidInternalOrUnresolvableUrl() {
        // http://internal-service/repo -> invalid (unresolvable or internal)
        assertFalse(validator.isValid("http://internal-service/repo", context));
        assertFalse(validator.isValid("http://localhost/repo", context));
    }

    @Test
    public void testInvalidSchemeFileUrl() {
        // file:///etc/passwd -> invalid (not allowlisted host and invalid scheme)
        assertFalse(validator.isValid("file:///etc/passwd", context));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://gitlab.com/repo",
            "https://bitbucket.org/user/repo",
            "https://www.github.com/org/repo"
    })
    public void testOtherAllowlistedHosts(String url) {
        assertTrue(validator.isValid(url, context));
    }
}
