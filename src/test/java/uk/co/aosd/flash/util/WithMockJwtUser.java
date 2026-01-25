package uk.co.aosd.flash.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.test.context.support.WithSecurityContext;

import uk.co.aosd.flash.domain.UserRole;

/**
 * Annotation to mock JWT authentication in tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = TestJwtUtils.TestSecurityContextFactory.class)
public @interface WithMockJwtUser {
    String userId() default "00000000-0000-0000-0000-000000000001";
    UserRole role() default UserRole.USER;
}
