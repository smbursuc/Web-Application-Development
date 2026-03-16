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

/**
 * Utility component for JWT creation, validation, and claim extraction.
 *
 * <p>Tokens are signed with HMAC-SHA256 using a Base64-encoded secret configured
 * via {@code jwt.secret} and expire after the millisecond duration set in
 * {@code jwt.expiration}. Tokens are read from an HTTP-only cookie named
 * {@code jwtToken}.
 */
@Component
public class JwtUtil
{
    /** The secret key, taken from application.properties. */
    private final SecretKey secretKey;

    /** The expiration date of the JWT HTTP cookie, taken from application.properties. */
    private final long expiration;

    /**
     * @param   base64Secret    
     *          Base64-encoded HMAC-SHA256 signing key
     * @param   expiration      
     *          token lifetime in milliseconds
     */
    public JwtUtil(@Value("${jwt.secret}") String base64Secret,
                   @Value("${jwt.expiration}") long expiration)
    {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }

    /**
     * Returns the signing key used to sign and verify tokens.
     *
     * @return  HMAC-SHA256 secret key
     */
    public SecretKey getSecretKey()
    {
        return secretKey;
    }


    /**
     * Generates a signed JWT for the given username.
     *
     * @param   username    
     *          the subject to embed in the token
     *
     * @return  compact JWT string
     */
    public String generateToken(String username)
    {
        return createToken(username);
    }

    /**
     * Builds and signs the JWT.
     *
     * @param   subject     
     *          username to set as the JWT subject
     *
     * @return  compact signed JWT
     */
    private String createToken(String subject)
    {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256) // secure signing
                .compact();
    }

    /**
     * Checks whether the token's expiration date is in the future.
     *
     * @param   token   
     *          compact JWT string
     *
     * @return  {@code true} when the token has not yet expired
     */
    public boolean isTokenValid(String token)
    {
        Date expiration = extractAllClaims(token).getExpiration(); // validate while parsing
        return !expiration.before(new Date()); // Check if the current date is after the expiration
    }

    /**
     * Extracts the username (subject claim) from a JWT.
     *
     * @param   token   
     *          compact JWT string
     *
     * @return  username embedded in the token
     */
    public String extractUsername(String token)
    {
        return extractAllClaims(token).getSubject(); // the subject is the username
    }

    /**
     * Parses and verifies the token signature, returning all claims.
     *
     * @param   token   
     *          compact JWT string
     *
     * @return  parsed claims
     *
     * @throws  io.jsonwebtoken.JwtException when the token is malformed or the signature is invalid
     */
    private Claims extractAllClaims(String token)
    {
        return Jwts.parserBuilder()
                   .setSigningKey(secretKey)
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }

    /**
     * Finds and returns the value of the {@code jwtToken} cookie from the request.
     *
     * @param   request     
     *          incoming HTTP request
     *
     * @return  raw JWT string, or {@code null} if the cookie is absent
     */
    public String extractTokenFromCookies(HttpServletRequest request)
    {
        if (request.getCookies() != null)
        {
            for (Cookie cookie : request.getCookies())
            {
                if (cookie.getName().equals("jwtToken"))
                {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
