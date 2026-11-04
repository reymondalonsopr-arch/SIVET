package com.sivet.controller.api;

import com.sivet.dto.UsuarioActualizarRequest;
import com.sivet.dto.UsuarioCrearRequest;
import com.sivet.dto.UsuarioResponse;
import com.sivet.model.Usuario;
import com.sivet.service.UsuarioService;

import jakarta.validation.Valid;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para administrar usuarios de SIVET.
 *
 * Expone operaciones para listar, consultar, registrar
 * y actualizar usuarios mediante la API.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioApiController {

    // Servicio que contiene la lógica de negocio de usuarios.
    private final UsuarioService usuarioService;

    /**
     * Inyección de dependencias mediante constructor.
     */
    public UsuarioApiController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * GET /api/usuarios
     * Devuelve todos los usuarios registrados.
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {

        List<UsuarioResponse> usuarios = usuarioService.listarTodos()
                .stream()
                .map(this::convertirAResponse)
                .toList();

        return ResponseEntity.ok(usuarios);
    }

    /**
     * GET /api/usuarios/{id}
     * Devuelve un usuario según su identificador.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarUsuario(
            @PathVariable Long id) {

        Usuario usuario = usuarioService.buscarPorId(id);

        return ResponseEntity.ok(convertirAResponse(usuario));
    }

    /**
     * POST /api/usuarios
     * Registra un usuario nuevo.
     */
    @PostMapping
    public ResponseEntity<UsuarioResponse> crearUsuario(
            @Valid @RequestBody UsuarioCrearRequest request) {

        Usuario usuario = usuarioService.registrar(
                request.nombreCompleto(),
                request.nombreUsuario(),
                request.password(),
                request.rolId()
        );

        UsuarioResponse respuesta = convertirAResponse(usuario);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    /**
     * PUT /api/usuarios/{id}
     * Actualiza los datos generales de un usuario.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioActualizarRequest request) {

        Usuario usuario = usuarioService.actualizar(
                id,
                request.nombreCompleto(),
                request.nombreUsuario(),
                request.rolId()
        );

        return ResponseEntity.ok(convertirAResponse(usuario));
    }

    /**
     * Convierte la entidad Usuario en un DTO de respuesta.
     *
     * Evita exponer la contraseña y la entidad Rol completa.
     */
    private UsuarioResponse convertirAResponse(Usuario usuario) {

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getNombreUsuario(),
                usuario.getRol().getNombre(),
                usuario.isActivo()
        );
    }
}