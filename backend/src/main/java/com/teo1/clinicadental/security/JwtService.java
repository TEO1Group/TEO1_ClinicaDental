package com.teo1.clinicadental.security;

import com.teo1.clinicadental.model.Rol;
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

    private static final String ROLE_CLAIM = "rol";

    private final SecretKey signingKey;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String extractSubject(String token) {
        return extractClaims(token).getSubject();
    }

    public Rol extractRol(String token) {
        String role = extractClaims(token).get(ROLE_CLAIM, String.class);
        return Rol.valueOf(role);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            String subject = claims.getSubject();
            Date expiration = claims.getExpiration();
            String role = claims.get(ROLE_CLAIM, String.class);

            return subject != null
                    && !subject.isBlank()
                    && expiration != null
                    && expiration.after(new Date())
                    && role != null
                    && isValidRole(role);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isValidRole(String role) {
        try {
            Rol.valueOf(role);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
