package com.sb.sfrigola_core.config.security.jwt.jwtservice;

import org.springframework.security.core.Authentication;

/**
 * Contract for JWT token operations.
 */
public interface IJWTService {

    /**
     * Generates a signed JWT token for the given authenticated principal.
     *
     * @param authentication the authenticated principal from the security context
     * @return signed JWT token string
     * @throws com.sb.sfrigola_core.config.security.exception.ex.SCAuthenticatedUserNotFoundException if the authenticated user cannot be resolved from the authentication object
     */
    String generateJWTToken(Authentication authentication);

}
