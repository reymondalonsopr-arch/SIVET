package com.sivet.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para JwtService.
 */
class JwtServiceTest {

    private static final String SECRET =
            "EstaEsUnaClaveSecretaDePruebaConMasDe32Bytes";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Creamos el servicio con una clave segura para las pruebas.
        jwtService = new JwtService(SECRET);
    }

    @Test
    void generarToken_debeCrearTokenValido() {
        // Arrange
        String nombreUsuario = "admin";

        // Act
        String token = jwtService.generarToken(nombreUsuario);

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());

        // Verificamos que el token tenga las tres partes JWT.
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void obtenerUsuario_debeRetornarNombreUsuarioDelToken() {
        // Arrange
        String nombreUsuario = "admin";
        String token = jwtService.generarToken(nombreUsuario);

        // Act
        String usuarioObtenido = jwtService.obtenerUsuario(token);

        // Assert
        assertEquals(nombreUsuario, usuarioObtenido);
    }

    @Test
    void generarToken_debeIncluirFechaDeEmisionYExpiracion() {
        // Arrange
        String token = jwtService.generarToken("admin");
        SecretKey secretKey = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8)
        );

        // Act
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Assert
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        // El token debe expirar después de su emisión.
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void obtenerUsuario_debeLanzarExcepcionConTokenInvalido() {
        // Arrange
        String tokenInvalido = "token.invalido";

        // Act y Assert
        assertThrows(
                RuntimeException.class,
                () -> jwtService.obtenerUsuario(tokenInvalido)
        );
    }

    @Test
    void obtenerUsuario_debeLanzarExcepcionConTokenExpirado() {
        // Arrange
        SecretKey secretKey = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8)
        );

        Date ahora = new Date();
        String tokenExpirado = Jwts.builder()
                .subject("admin")
                .issuedAt(new Date(ahora.getTime() - 10_000))
                .expiration(new Date(ahora.getTime() - 1_000))
                .signWith(secretKey)
                .compact();

        // Act y Assert
        assertThrows(
                RuntimeException.class,
                () -> jwtService.obtenerUsuario(tokenExpirado)
        );
    }
}
