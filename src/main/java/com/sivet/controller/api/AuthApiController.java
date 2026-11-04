package com.sivet.controller.api;

import com.sivet.model.Usuario;
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
 * Controlador para la autenticación de la API REST.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Inyección de dependencias mediante constructor.
     */
    public AuthApiController(
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
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

            // Recuperamos al usuario autenticado.
            Usuario usuario = (Usuario) autenticacion.getPrincipal();

            // Generamos el token incluyendo el rol del usuario.
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
     * DTO para recibir las credenciales del login.
     */
    public record LoginRequest(
            @NotBlank(message = "El nombre de usuario es obligatorio.")
            String nombreUsuario,

            @NotBlank(message = "La contraseña es obligatoria.")
            String password
    ) {
    }
}