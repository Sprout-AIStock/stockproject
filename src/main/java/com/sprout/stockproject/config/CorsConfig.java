package com.sprout.stockproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5175",
                        "http://localhost:5174",
                        "http://localhost:5173",
                        "http://localhost:4173",
                        "https://stock-project-ok8zdefdo-qkrwnsmirs-projects.vercel.app",
                        "https://stock-project-69wj9ztx8-qkrwnsmirs-projects.vercel.app",
                        "https://stock-project-ateh4mmph-qkrwnsmirs-projects.vercel.app",
                        "https://*.vercel.app"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}


