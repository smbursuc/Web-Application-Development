package com.soloproductions.wade.filter;

import com.soloproductions.wade.entity.User;
import com.soloproductions.wade.repository.UserRepository;
import com.soloproductions.wade.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servlet filter that resolves JWT cookies, validates tokens, and populates
 * the Spring Security context for authenticated requests.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter
{

    /** Utility for extracting and validating JWT tokens. */
    private final JwtUtil jwtUtil;

    /** Repository used to resolve authenticated users by username from the token. */
    private final UserRepository userRepository;

    /** Logger. */
    private static final Logger LOG = LogManager.getLogger(JwtAuthenticationFilter.class);

    /**
     * Creates a JWT authentication filter.
     *
     * @param   jwtUtil utility for JWT extraction and validation
     * @param   userRepository repository for loading user entities
     */
    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository)
    {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    /**
     * Resolves JWT cookie authentication for the current request.
     *
     * @param   request incoming HTTP request
     * @param   response outgoing HTTP response
     * @param   filterChain filter chain continuation
     *
     * @throws  ServletException 
     *          if servlet filtering fails
     * @throws  IOException 
     *          if request or response I/O fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException
    {

        String token = jwtUtil.extractTokenFromCookies(request);

        if (token != null)
        {
            try
            {
                if (jwtUtil.isTokenValid(token))
                {
                    String username = jwtUtil.extractUsername(token);
                    User user = userRepository.findByUsername(username);
                    if (user != null)
                    {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        LOG.info("Successfully authenticated user: {}", username);
                    }
                    else
                    {
                        LOG.warn("Token valid for username {}, but user not found in DB", username);
                    }
                }
                else
                {
                    LOG.warn("Invalid JWT token found in cookies");
                }
            }
            catch (Exception e)
            {
                LOG.error("Error during JWT authentication: {}", e.getMessage());
                e.printStackTrace();
            }
        }
        else
        {
            // No token found - this is fine for permitAll requests, just won't be authenticated
            // LOG.debug("No JWT token found in cookies for request to {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}

