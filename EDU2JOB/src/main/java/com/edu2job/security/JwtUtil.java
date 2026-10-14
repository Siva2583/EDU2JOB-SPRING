package com.edu2job.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    /**
     * JWT signing secret, injected from {@code edu2job.jwt.secret}
     * (environment variable {@code JWT_SECRET} in deployment). The fallback
     * value only exists so local development keeps working out of the box —
     * always set a long random secret outside local dev. HS256 needs a key of
     * at least 32 bytes.
     */
    private final String SECRET_KEY;

    public JwtUtil(@Value("${edu2job.jwt.secret:your_very_secure_secret_key_change_this_in_production}") String secretKey) {
        this.SECRET_KEY = secretKey;
    }

    // Converts your plain string into a cryptographic Key object
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}