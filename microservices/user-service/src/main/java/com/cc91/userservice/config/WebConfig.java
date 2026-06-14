package com.cc91.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    @Value("${app.upload.image-dir:uploads/images}")
    private String imageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:" + avatarDir + "/");
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations("file:" + imageDir + "/");
    }
}
