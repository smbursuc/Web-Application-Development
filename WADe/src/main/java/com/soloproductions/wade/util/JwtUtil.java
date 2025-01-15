package com.soloproductions.wade.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil
{

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String base64Secret,
                   @Value("${jwt.expiration}") long expiration)
    {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }

    public SecretKey getSecretKey()
    {
        return secretKey;
    }


    public String generateToken(String username)
    {
        return createToken(username);
    }

    private String createToken(String subject)
    {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256) // secure signing
                .compact();
    }

    public boolean isTokenValid(String token)
    {
        Date expiration = extractAllClaims(token).getExpiration(); // validate while parsing
        return !expiration.before(new Date()); // Check if the current date is after the expiration
    }

    public String extractUsername(String token)
    {
        return extractAllClaims(token).getSubject(); // the subject is the username
    }

    private Claims extractAllClaims(String token)
    {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractTokenFromCookies(HttpServletRequest request)
    {
        if (request.getCookies() != null)
        {
            for (Cookie cookie : request.getCookies())
            {
                if ("jwtToken".equals(cookie.getName()))
                {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
