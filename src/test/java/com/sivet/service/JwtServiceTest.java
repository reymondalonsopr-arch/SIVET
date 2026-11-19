package com.sivet.service;

import com.sivet.model.Rol;
import com.sivet.model.Usuario;
import com.sivet.security.JwtService;
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
 * Pruebas unitarias para el servicio JWT.
 */
class JwtServiceTest {

    private static final String SECRET =
            "EstaEsUnaClaveSecretaDePruebaConMasDe32Bytes";

    private JwtService jwtService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        // Creamos el servicio con una clave segura para las pruebas.
        jwtService = new JwtService(SECRET);

        // Preparamos un usuario con rol para generar el token.
        Rol rol = new Rol();
        rol.setNombre("ADMIN");

        usuario = new Usuario();
        usuario.setNombreUsuario("admin");
        usuario.setRol(rol);
    }

    @Test
    void generarToken_debeCrearTokenValido() {
        // Act
        String token = jwtService.generarToken(usuario);

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());

        // Verificamos que el token tenga las tres partes JWT.
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void obtenerUsuario_debeRetornarNombreUsuarioDelToken() {
        // Arrange
        String token = jwtService.generarToken(usuario);

        // Act
        String usuarioObtenido = jwtService.obtenerUsuario(token);

        // Assert
        assertEquals("admin", usuarioObtenido);
    }

    @Test
    void generarToken_debeIncluirFechaEmisionExpiracionYRol() {
        // Arrange
        String token = jwtService.generarToken(usuario);
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
        assertEquals("ADMIN", claims.get("rol", String.class));

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