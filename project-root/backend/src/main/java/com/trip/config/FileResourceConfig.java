package com.trip.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 本地上传文件访问配置。
 */
@Configuration
public class FileResourceConfig implements WebMvcConfigurer {

    private final Path uploadRoot;
    private final String accessPrefix;

    public FileResourceConfig(
            @Value("${trip.file.upload-dir:uploads}") String uploadDir,
            @Value("${trip.file.access-prefix:/files}") String accessPrefix) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.accessPrefix = normalizeAccessPrefix(accessPrefix);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(accessPrefix + "/**")
                .addResourceLocations(normalizeResourceLocation(uploadRoot.toUri().toString()));
    }

    private String normalizeAccessPrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "/files";
        }
        String normalizedPrefix = prefix.trim();
        if (!normalizedPrefix.startsWith("/")) {
            normalizedPrefix = "/" + normalizedPrefix;
        }
        if (normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }
        return normalizedPrefix;
    }

    private String normalizeResourceLocation(String resourceLocation) {
        if (resourceLocation.endsWith("/")) {
            return resourceLocation;
        }
        return resourceLocation + "/";
    }
}
