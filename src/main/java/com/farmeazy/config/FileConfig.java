package com.farmeazy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Intentionally do NOT expose raw /uploads/** as a static resource.
        // Attachments must be served via SecureAttachmentController to enforce access checks.
    }
}
