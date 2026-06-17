package com.sb.sfrigola_core.config.security.exception;

import com.sb.sfrigola_core.common.dto.external.response.SCErrorDataDto;
import com.sb.sfrigola_core.common.dto.external.response.SCGeneralResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;


    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        SCErrorDataDto errorData = new SCErrorDataDto(
                request.getRequestURI(),
                HttpStatus.UNAUTHORIZED,
                Map.of("authentication", "Full authentication is required to access this resource"),
                LocalDateTime.now()
        );

        SCGeneralResponseDto<Void,Void> errorResponse = SCGeneralResponseDto.error(errorData);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}