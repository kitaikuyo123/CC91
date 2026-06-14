package com.cc91.forumservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // No static resource handlers needed for Forum Service
    // All user-uploaded content is served by User Service
}
