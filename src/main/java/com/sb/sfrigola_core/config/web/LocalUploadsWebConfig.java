package com.sb.sfrigola_core.config.web;

import com.sb.sfrigola_core.common.constant.SCGeneralConstants;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class LocalUploadsWebConfig implements WebMvcConfigurer {

    private final Environment env;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        var rootDirProperty = env.getProperty(SCGeneralConstants.FILE_STORAGE_ROOT_DIR);
        if (rootDirProperty == null || rootDirProperty.isBlank()) {
            throw new IllegalStateException(SCGeneralConstants.FILE_STORAGE_ROOT_DIR + " is not configured");
        }
        String location = Path.of(rootDirProperty).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

}
