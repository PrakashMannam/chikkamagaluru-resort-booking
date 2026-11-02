package com.companya.resort.security;

import com.companya.resort.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    public static final long EXPIRATION_TIME_MS = 8 * 60 * 60 * 1000L; // 8 hours

    @Value("${app.jwt.secret:${JWT_SECRET:}}")
    private String jwtSecret;

    private SecretKey key;

    public JwtService() {
    }

    public JwtService(String jwtSecret) {
        this.jwtSecret = jwtSecret;
        init();
    }

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.trim().length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be configured and at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(jwtSecret.trim().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", "ROLE_" + user.getRole().name())
                .claim("is_gm", user.getIsGm())
                .claim("specialty_lane", user.getSpecialtyLane().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_TIME_MS))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String generateTokenWithRole(User user, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", role)
                .claim("is_gm", user.getIsGm())
                .claim("specialty_lane", user.getSpecialtyLane().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION_TIME_MS))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String generateTokenWithExpiration(User user, long expirationMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", "ROLE_" + user.getRole().name())
                .claim("is_gm", user.getIsGm())
                .claim("specialty_lane", user.getSpecialtyLane().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims validateAndExtractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
