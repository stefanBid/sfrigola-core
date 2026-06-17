package com.sb.sfrigola_core.config.security.jwt;

import com.sb.sfrigola_core.common.dto.external.response.SCErrorDataDto;
import com.sb.sfrigola_core.common.dto.external.response.SCGeneralResponseDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class JwtValidationFilter extends OncePerRequestFilter {

    private final AntPathMatcher pathMatcher;
    private final ObjectMapper objectMapper;

    @Qualifier("publicPath")
    private final List<String> publicPaths;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return super.shouldNotFilter(request);
    }



    private void handleError(HttpServletRequest req, HttpServletResponse resp, String errorKey, String errorMessage) throws IOException {
        SCErrorDataDto errorData = new SCErrorDataDto(
                req.getRequestURI(),
                HttpStatus.UNAUTHORIZED,
                Map.of(errorKey, errorMessage),
                LocalDateTime.now()
        );

        SCGeneralResponseDto<Void, Void> responseDto = SCGeneralResponseDto.error(errorData);

        resp.setStatus(HttpStatus.UNAUTHORIZED.value());
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.getWriter().write(objectMapper.writeValueAsString(responseDto));
    }
}
