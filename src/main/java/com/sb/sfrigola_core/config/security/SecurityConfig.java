package com.sb.sfrigola_core.config.security;

import com.sb.sfrigola_core.domains.users.enums.SCUserRole;
import com.sb.sfrigola_core.config.security.exception.CustomAccessDeniedHandler;
import com.sb.sfrigola_core.config.security.exception.CustomAuthenticationEntryPoint;
import com.sb.sfrigola_core.config.security.jwt.JwtValidationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
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

    // Inject Paths
    @Qualifier("publicPath")
    private final List<String> publicPaths;
    @Qualifier("authPath")
    private final List<String> authPaths;
    @Qualifier("onlyAdminPath")
    private final List<String> onlyAdminPaths;
    @Qualifier("onlyUserPath")
    private final List<String> onlyUserPaths;
    @Qualifier("onlyContributorPath")
    private final List<String> onlyContributorPaths;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(corsConfig -> corsConfig.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(request -> {
                    publicPaths.forEach(path -> request.requestMatchers(path).permitAll());
                    onlyAdminPaths.forEach(path -> request.requestMatchers(path).hasAuthority(SCUserRole.ROLE_ADMIN.getAuthority()));
                    onlyContributorPaths.forEach(path -> request.requestMatchers(path).hasAuthority(SCUserRole.ROLE_CONTRIBUTOR.getAuthority()));
                    onlyUserPaths.forEach(path -> request.requestMatchers(path).hasAuthority(SCUserRole.ROLE_USER.getAuthority()));
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


}
