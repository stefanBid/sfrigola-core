package com.sb.sfrigola_core.config.security.jwt;

import com.sb.sfrigola_core.common.constant.SCGeneralConstants;
import com.sb.sfrigola_core.common.util.SCErrorDataBuilderUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class JwtValidationFilter extends OncePerRequestFilter {

    private final Environment env;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper;

    @Qualifier("publicPath")
    private final List<String> publicPaths;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(env == null ){
            SCErrorDataBuilderUtils.handleError(request, response, objectMapper, Map.of("jwt_env_error", "Environment is not available"), HttpStatus.UNAUTHORIZED);
        }

        // 1) Read env variable for token filter
        assert env != null;
        String headerName = env.getProperty(SCGeneralConstants.JWT_HEADER);
        String secret = env.getProperty(SCGeneralConstants.JWT_SECRET_KEY, SCGeneralConstants.JWT_SECRET_KEY_DEFAULT);


        String authHeader = request.getHeader(headerName);

        if(authHeader != null){
            try{

                var validator1S = validateJWTToken(authHeader);
                if(!validator1S.isValid())
                    SCErrorDataBuilderUtils.handleError(request, response, objectMapper, validator1S.error(), HttpStatus.UNAUTHORIZED);

                String jwt = validator1S.jwt();
                assert  jwt != null;
                SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(jwt).getPayload();


                var validator2S = validateUsernameAndRoles(claims.get("username"), claims.get("roles"));

                if(!validator2S.isValid())
                    SCErrorDataBuilderUtils.handleError(request, response, objectMapper, validator2S.error(), HttpStatus.UNAUTHORIZED);

                String username = validator2S.username();
                String roles = validator2S.roles();

                assert username != null && roles != null;
                Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, AuthorityUtils.commaSeparatedStringToAuthorityList(roles));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }catch(ExpiredJwtException ex){
                SCErrorDataBuilderUtils.handleError(request, response, objectMapper, Map.of("jwt_expired_error", "JWT token has expired"), HttpStatus.UNAUTHORIZED);
            } catch (Exception ex) {
                SCErrorDataBuilderUtils.handleError(request, response, objectMapper, Map.of("jwt_validation_error", "JWT token validation failed: " + ex.getMessage()), HttpStatus.UNAUTHORIZED);
            }

            // After successful validation, continue the filter chain
            filterChain.doFilter(request, response);
        }


    }

    /**
     * Manage the paths that do not require JWT validation. If the request path matches any of the public paths, the filter will be skipped.
     * @param request current HTTP request
     * @return true if the request should not be filtered, false otherwise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return publicPaths.stream().anyMatch(publicPath -> pathMatcher.match(publicPath, path));
    }


    // Private Validation Token Helpers

    /**
     * Helper record to encapsulate the result of token validation, including whether the token is valid, any error messages if invalid, and the extracted JWT token if valid.
     * @param isValid
     * @param error
     * @param jwt
     */
    private record TokenValidationResult(boolean isValid, Map<String, String> error, String jwt) {}


    private record UsernameAndRoleValidationResult(boolean isValid, Map<String, String> error, String username, String roles) { }


    /**
     * Validate that the Authorization header contains a Bearer token and extract the JWT token from it. If the header does not start with "Bearer " or if the token is empty, an error is returned.
     * @param authHeader
     * @return TokenValidationResult containing the validation result, any error messages, and the extracted JWT token if valid
     */
    private TokenValidationResult validateJWTToken (String authHeader) {
        if(!authHeader.startsWith(SCGeneralConstants.JWT_HEADER_PREFIX))
            return new TokenValidationResult(false, Map.of("jwt_format_error", "Invalid JWT token format"), null);

        String jwt = authHeader.substring(SCGeneralConstants.JWT_HEADER_PREFIX.length());

        if(jwt.isBlank())
            return new TokenValidationResult(false, Map.of("jwt_format_error", "JWT token is empty"), null);


        return new TokenValidationResult(true, null, jwt);

    }

    private UsernameAndRoleValidationResult validateUsernameAndRoles (Object usernameObj, Object rolesObj) {
        Map<String, String> er = new HashMap<>();
        String username = null;
        String roles = null;

        if(usernameObj == null || usernameObj.toString().isBlank()){
            er.put("jwt_username_error", "Username claim is missing or empty");
        } else {
            username = usernameObj.toString();
        }

        if(rolesObj == null || rolesObj.toString().isBlank()){
            er.put("jwt_roles_error", "Roles claim is missing or empty");
        } else {
            roles = rolesObj.toString();
        }

        return new UsernameAndRoleValidationResult(er.isEmpty(), er, username, roles);
    }
}