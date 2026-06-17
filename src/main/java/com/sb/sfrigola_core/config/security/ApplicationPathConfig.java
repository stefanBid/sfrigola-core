package com.sb.sfrigola_core.config.security;

import com.sb.sfrigola_core.common.constant.SCGeneralConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ApplicationPathConfig {

    private final Environment env;


    @Bean("publicPath")
    public List<String> publicPath() {
        return List.of(
                ""
        );
    }

    @Bean("authPath")
    public List<String> authPath() {
        return List.of(
                ""
        );
    }

    @Bean("onlyAdminPath")
    public List<String> adminPath() {
        return List.of(
                ""
        );
    }

    @Bean("onlyUserPath")
    public List<String> userPath() {
        return List.of(
                ""
        );
    }

    @Bean("onlyContributorPath")
    public List<String> contributorPath() {
        return List.of(
                ""
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
