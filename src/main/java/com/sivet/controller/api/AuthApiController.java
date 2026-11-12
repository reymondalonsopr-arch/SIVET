
package com.sivet.controller.api;

import com.sivet.model.Usuario;
import com.sivet.repository.UsuarioRepository;
import com.sivet.security.JwtService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador encargado de la autenticación de la API REST.
 * Permite validar las credenciales y generar tokens JWT.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    /**
     * Inyección de dependencias mediante constructor.
     */
    public AuthApiController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioRepository usuarioRepository) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Autentica al usuario y devuelve un token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        try {
            // Validamos las credenciales con Spring Security.
            Authentication autenticacion =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.nombreUsuario(),
                                    request.password()
                            )
                    );

            // Recuperamos el nombre del usuario autenticado.
            // Spring Security devuelve un objeto UserDetails,
            // no necesariamente nuestra entidad Usuario.
            String nombreUsuario = autenticacion.getName();

            // Consultamos nuevamente la entidad Usuario en PostgreSQL.
            Usuario usuario = usuarioRepository
                    .findByNombreUsuario(nombreUsuario)
                    .orElseThrow(() -> new IllegalStateException(
                            "No se encontró el usuario autenticado."
                    ));

            // Generamos el token JWT utilizando la entidad Usuario.
            String token = jwtService.generarToken(usuario);

            // Devolvemos el token al cliente.
            return ResponseEntity.ok(
                    Map.of(
                            "token", token,
                            "tipo", "Bearer"
                    )
            );

        } catch (AuthenticationException excepcion) {

            // No revelamos si falló el usuario o la contraseña.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "mensaje", "Usuario o contraseña incorrectos."
                    ));
        }
    }

    /**
     * DTO que recibe las credenciales del inicio de sesión.
     */
    public record LoginRequest(

            @NotBlank(message = "El nombre de usuario es obligatorio.")
            String nombreUsuario,

            @NotBlank(message = "La contraseña es obligatoria.")
            String password

    ) {
    }
}
