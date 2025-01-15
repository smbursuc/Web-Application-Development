package com.soloproductions.wade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (Cross-Site Request Forgery) for simplicity in APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS (Cross-Origin Resource Sharing)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Allow all requests without authentication
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow requests from React app (localhost:3000)
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));

        // Allow HTTP methods such as GET, POST, PUT, DELETE, OPTIONS
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow all headers to be included in requests
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (cookies, authorization headers) to be included
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apply CORS configuration to all API endpoints
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}


