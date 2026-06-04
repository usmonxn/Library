package com.example.authservice.service;

import com.example.authservice.entity.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.util.Date;

@Service
public class JwtService {
    @Value("${app.jwt-secret}")
    private String secretKey;

    public String generateToken(UserDto user) {
        return Jwts.builder()
                .subject(user.getPhoneNumber())
                .claim("userId", user.getId())
                .claim("phoneNumber", user.getPhoneNumber())
                .claim("isAdmin", user.getIsAdmin())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(getSignKey())
                .compact();
    }

    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public Boolean getIsAdmin(String token) {
        return getClaims(token).get("isAdmin", Boolean.class);
    }

    public boolean validateToken(String token) {
        try {
            return getUsername(token) != null && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
