package com.sb.sfrigola_core.config.security;

import com.sb.sfrigola_core.common.enums.SCUserRole;
import com.sb.sfrigola_core.config.security.exception.CustomAccessDeniedHandler;
import com.sb.sfrigola_core.config.security.exception.CustomAuthenticationEntryPoint;
import com.sb.sfrigola_core.config.security.jwt.JwtValidationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Inject Hierarchy
    @Qualifier("scHierarchy")
    private final RoleHierarchy roleHierarchy;

    // Inject Paths
    @Qualifier("publicPath")
    private final List<String> publicPaths;
    @Qualifier("authPath")
    private final List<String> authPaths;
    @Qualifier("adminPath")
    private final List<String> adminPaths;
    @Qualifier("userPath")
    private final List<String> userPaths;
    @Qualifier("contributorPath")
    private final List<String> contributorPaths;
    @Qualifier("authenticatedGetPath")
    private final List<String> authenticatedGetPaths;

    @Qualifier("allowedOriginsPaths")
    private final List<String> allowedOriginsPaths;

    // Inject Auth Provider
    @Qualifier("scUsernamePwAuthenticationProvider")
    private final AuthenticationProvider authenticationProvider;

    // Inject Exception Security Service
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    private final ObjectMapper objectMapper;
    private final Environment env;


    @Bean(name="scSecurityFilterChain")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(corsConfig -> corsConfig.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(request -> {
                    publicPaths.forEach(path -> request.requestMatchers(path).permitAll());
                    adminPaths.forEach(path -> request.requestMatchers(path).access(hasAuthorityWithHierarchy(SCUserRole.ROLE_ADMIN.getAuthority())));
                    authenticatedGetPaths.forEach(path -> request.requestMatchers(HttpMethod.GET, path).authenticated());
                    contributorPaths.forEach(path -> request.requestMatchers(path).access(hasAuthorityWithHierarchy(SCUserRole.ROLE_CONTRIBUTOR.getAuthority())));
                    userPaths.forEach(path -> request.requestMatchers(path).access(hasAuthorityWithHierarchy(SCUserRole.ROLE_USER.getAuthority())));
                    authPaths.forEach(path -> request.requestMatchers(path).authenticated());
                    request.anyRequest().denyAll();
                })
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .addFilterBefore(new JwtValidationFilter(env, objectMapper, publicPaths), BasicAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .build();
    }


    @Bean("scCorsConfigurationSource")
    public CorsConfigurationSource corsConfigurationSource() {
            var corsConfig = new CorsConfiguration();
            corsConfig.setAllowedOrigins(allowedOriginsPaths);
            corsConfig.setAllowedMethods(Collections.singletonList("*"));
            corsConfig.setAllowedHeaders(Collections.singletonList("*"));
            corsConfig.setAllowCredentials(true);
            corsConfig.setMaxAge(3600L);
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", corsConfig);
            return source;
    }


    @Bean(name="scAuthenticationManager")
    public AuthenticationManager authenticationManager(){
        return new ProviderManager(authenticationProvider);
    }


    private AuthorizationManager<RequestAuthorizationContext> hasAuthorityWithHierarchy(String authority){
        var manager = AuthorityAuthorizationManager.<RequestAuthorizationContext>hasAuthority(authority);
        manager.setRoleHierarchy(roleHierarchy);
        return manager;
    }
}
