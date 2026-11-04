package com.sivet.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Servicio para generar y validar tokens JWT.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiracionMs = 8 * 60 * 60 * 1000L;

    public JwtService(
            @Value("${sivet.jwt.secret}") String secret) {

        // Convertimos la clave configurada en una clave criptográfica.
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Genera un token JWT para el usuario autenticado.
     */
    public String generarToken(String nombreUsuario) {
        Date ahora = new Date();

        return Jwts.builder()
                .subject(nombreUsuario)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Obtiene el nombre de usuario desde un token válido.
     */
    public String obtenerUsuario(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }
}