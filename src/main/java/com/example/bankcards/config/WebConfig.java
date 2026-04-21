package com.example.bankcards.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path docDirectory = Paths.get("docs");
        String absolutePath = docDirectory.toFile().getAbsolutePath();

        registry.addResourceHandler("/docs/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
