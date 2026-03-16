package com.soloproductions.wade.config;

import com.soloproductions.wade.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * The security configuration for this application. IMPR is a stateless application, where state is preserved by JWT tokens, and
 * communication is exchanged using REST API calls.
 * <p>
 * The following profile is for test environments only.
 */
@Configuration
public class SecurityConfig
{
    /** The JWT authentication filter used to validate JWT tokens in incoming requests. */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 
     * Constructor for SecurityConfig. Initializes the JWT authentication filter.
     * 
     * @param jwtAuthenticationFilter the JWT authentication filter to be used in the security filter chain
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter)
    {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configure the security filter chain for the application. The security filter chain is responsible 
     * for handling authentication and authorization for incoming requests. 
     * 
     * @param http the HttpSecurity object to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs while configuring the security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        // TODO: add a more robust profile, this is suitable for development but not production.
        http
                // Disable CSRF (Cross-Site Request Forgery) for simplicity in APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS (Cross-Origin Resource Sharing)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Set session management to stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Allow all requests without authentication
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                
                // Add JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configure CORS settings for the application.
     * 
     * @return the configured CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource()
    {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow requests from React app (localhost:3000)
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));

        // Allow HTTP methods such as GET, POST, PUT, DELETE, OPTIONS
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow all headers to be included in requests
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (cookies, authorization headers) to be included (for JWT)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apply CORS configuration to all API endpoints
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}


