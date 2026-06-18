package com.sb.sfrigola_core.config.security.jwt.jwtservice;

import org.springframework.security.core.Authentication;

public interface IJWTService {

     String generateJWTToken(Authentication authentication);
}
