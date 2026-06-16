package com.example.Bknd_User.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.Bknd_User.entity.User;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;
    
    public String generarToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .signWith(key)
                .compact();
    }
    
    public User comprobarToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token inválido");
        }
        
        String token = authHeader.substring(7);
        return extraerUsuarioDelToken(token);
    }
    
    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }
    
    public boolean esTokenValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private Claims extraerClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    private User extraerUsuarioDelToken(String token) {
        Claims claims = extraerClaims(token);
        
        User user = new User();
        user.setEmail(claims.getSubject());
        user.setId(((Number) claims.get("userId")).longValue());
        user.setUsername((String) claims.get("username"));
        user.setRole((String) claims.get("role"));
        
        return user;
    }
}