package com.bankflow.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Two registrations on purpose: the {@link CorsConfigurationSource} bean is what
 * Spring Security's filter chain consumes from Phase 3 onwards, while the MVC
 * mapping keeps CORS working before the security chain exists.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String[] METHODS =
            { "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS" };

    @Value("${bankflow.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods(METHODS)
                .allowedHeaders("Authorization", "Content-Type", "X-Idempotency-Key")
                .exposedHeaders("Retry-After", "X-Total-Count")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of(METHODS));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Idempotency-Key"));
        config.setExposedHeaders(List.of("Retry-After", "X-Total-Count"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
