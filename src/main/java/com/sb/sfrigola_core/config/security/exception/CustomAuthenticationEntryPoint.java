package com.sb.sfrigola_core.config.security.exception;

import com.sb.sfrigola_core.common.util.SCErrorDataBuilderUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        SCErrorDataBuilderUtils.handleError(
                request, response, objectMapper,
                Map.of("authentication", "Full authentication is required to access this resource"),
                HttpStatus.UNAUTHORIZED
        );
    }
}