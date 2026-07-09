package com.sb.sfrigola_core.config.security;

import com.sb.sfrigola_core.common.constant.SCGeneralConstants;
import com.sb.sfrigola_core.common.enums.SCUserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
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

    @Bean("scHierarchy")
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                %s > %s
                %s > %s
                """.formatted(SCUserRole.ROLE_ADMIN.getAuthority(), SCUserRole.ROLE_CONTRIBUTOR.getAuthority(),
                        SCUserRole.ROLE_CONTRIBUTOR.getAuthority(), SCUserRole.ROLE_USER.getAuthority())
        );
    }


    @Bean("publicPath")
    public List<String> publicPath() {
        return List.of(
                "/api/auth/login",
                "/api/auth/register",
                "/api/languages/**",
                "/api/categories",
                "/error",
                "/api/v3/api-docs/**",
                "/swagger-ui/**"
        );
    }

    /**
     * GET-only paths open to anyone, unauthenticated included. Checked before
     * {@code contributorPath}/{@code adminPath} so a broader mutating rule on the
     * same path (e.g. {@code POST /api/recipes}) does not get exposed as public too.
     */
    @Bean("publicGetPath")
    public List<String> publicGetPath() {
        return List.of(
                "/api/recipes",
                "/api/recipes/home/category/**",
                "/api/recipes/search/**",
                "/api/recipes/details/**"
        );
    }

    @Bean("adminPath")
    public List<String> adminPath() {
        return List.of(
                "/api/users/admin/**",
                "/api/categories/admin/**",
                "/api/tags/admin/**",
                "/api/ingredients/admin/**",
                "/api/recipes/admin/**"
        );
    }

    @Bean("userPath")
    public List<String> userPath() {
        return List.of();
    }

    /**
     * GET-only paths accessible to any authenticated role (USER/CONTRIBUTOR/ADMIN),
     * checked before {@code contributorPath}/{@code adminPath} so a broader mutating
     * rule on the same path (e.g. {@code POST /api/recipes}) does not shadow it.
     */
    @Bean("authenticatedGetPath")
    public List<String> authenticatedGetPath() {
        return List.of(
                "/api/recipes"
        );
    }

    @Bean("contributorPath")
    public List<String> contributorPath() {
        return List.of(
                "/api/tags/**",
                "/api/ingredients/**",
                "/api/recipes/**"
        );
    }

    @Bean("authPath")
    public List<String> authPath() {
        return List.of(
                "/api/users/settings/**",
                "/api/users/profile/**",
                "/api/auth/change-password",
                "/api/auth/change-email"
        );
    }

    @Bean(name="allowedOriginsPaths")
    public List<String> allowedOriginsPaths() {
        var allowedOrigins = env.getProperty(SCGeneralConstants.ALLOWED_ORIGINS);
        if (allowedOrigins == null || allowedOrigins.isBlank())
            return List.of();
        return List.of(allowedOrigins.split(","));
    }
}