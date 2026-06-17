package com.sb.sfrigola_core.config.security.exception;

import com.sb.sfrigola_core.common.dto.external.response.SCErrorDataDto;
import com.sb.sfrigola_core.common.dto.external.response.SCGeneralResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        SCErrorDataDto errorData = new SCErrorDataDto(
                request.getRequestURI(),
                HttpStatus.FORBIDDEN,
                Map.of("authorization", "You don't have permission to access this resource"),
                LocalDateTime.now()
        );

        SCGeneralResponseDto<Void,Void> errorResponse = SCGeneralResponseDto.error(errorData);

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
