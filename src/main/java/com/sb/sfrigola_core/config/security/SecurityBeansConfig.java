package com.sb.sfrigola_core.config.security;

import com.sb.sfrigola_core.common.constant.SCGeneralConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityBeansConfig {

    private final Environment env;

    @Bean(name = "scPasswordEncoder")
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean(name = "scCompromisedPasswordChecker")
    public CompromisedPasswordChecker compromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }


    @Bean("publicPath")
    public List<String> publicPath() {
        return List.of(
                "/api/auth/login",
                "/api/auth/register",
                "/api/languages/**",
                "/error"
        );
    }

    @Bean("authPath")
    public List<String> authPath() {
        return List.of(
                "/api/users/**"
        );
    }

    @Bean("onlyAdminPath")
    public List<String> adminPath() {
        return List.of();
    }

    @Bean("onlyUserPath")
    public List<String> userPath() {
        return List.of();
    }

    @Bean("onlyContributorPath")
    public List<String> contributorPath() {
        return List.of();
    }

    @Bean(name="allowedOriginsPaths")
    public List<String> allowedOriginsPaths() {
        var allowedOrigins = env.getProperty(SCGeneralConstants.ALLOWED_ORIGINS);
        if (allowedOrigins == null || allowedOrigins.isBlank())
            return List.of();
        return List.of(allowedOrigins.split(","));
    }
}