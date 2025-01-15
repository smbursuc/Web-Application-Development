package com.soloproductions.wade.filter;

import com.soloproductions.wade.entity.User;
import com.soloproductions.wade.repository.UserRepository;
import com.soloproductions.wade.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter
{

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository)
    {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException
    {

        String token = jwtUtil.extractTokenFromCookies(request);

        if (token != null && jwtUtil.isTokenValid(token))
        {
            try
            {
                String username = jwtUtil.extractUsername(token);

                User user = userRepository.findByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, null);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }

        filterChain.doFilter(request, response);
    }
}

