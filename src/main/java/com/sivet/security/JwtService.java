
package com.sivet.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sivet.model.Usuario;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(@Value("${sivet.jwt.secret}") String secret) {
        // Convierte la clave configurada en una clave criptográfica.
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();

        // Genera un token con el nombre de usuario, rol y vencimiento.
        return Jwts.builder()
                .subject(usuario.getNombreUsuario())
                .claim("rol", usuario.getRol().getNombre())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(8, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    public String obtenerUsuario(String token) {
        // Verifica la firma y la fecha de expiración del token.
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
