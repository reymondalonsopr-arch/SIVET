
package com.sivet.service;

import com.sivet.model.Rol;
import com.sivet.repository.RolRepository;
import com.sivet.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Servicio encargado de gestionar la lógica de negocio
 * relacionada con los roles de SIVET.
 */
@Service
@Transactional
public class RolService {

    // Repositorio utilizado para acceder a los roles.
    private final RolRepository rolRepository;

    // Repositorio utilizado para comprobar los usuarios asociados.
    private final UsuarioRepository usuarioRepository;

    // Servicio encargado de recuperar mensajes personalizados.
    private final MensajeService mensajeService;

    // Roles principales que nunca deben eliminarse.
    private static final Set<String> ROLES_PROTEGIDOS =
            Set.of("ADMIN", "VENDEDOR", "ALMACENERO");

    /**
     * Inyección de dependencias mediante constructor.
     */
    public RolService(
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository,
            MensajeService mensajeService) {

        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.mensajeService = mensajeService;
    }

    /**
     * Registra un nuevo rol después de comprobar
     * que su nombre no esté registrado.
     */
    public Rol registrar(String nombre, String descripcion) {

        // Eliminamos espacios innecesarios al inicio y al final.
        nombre = nombre.trim();

        // Evitamos registrar nombres duplicados.
        if (rolRepository.existsByNombre(nombre)) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("rol.duplicado", nombre)
            );
        }

        // Creamos y asignamos los datos del nuevo rol.
        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(descripcion);

        // Guardamos el rol en PostgreSQL.
        return rolRepository.save(rol);
    }

    /**
     * Obtiene todos los roles registrados.
     */
    @Transactional(readOnly = true)
    public List<Rol> listarTodos() {
        return rolRepository.findAll();
    }

    /**
     * Busca un rol por su identificador.
     * Si no existe, genera una excepción.
     */
    @Transactional(readOnly = true)
    public Rol buscarPorId(Long id) {

        return rolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("rol.no.encontrado.id", id)
                ));
    }

    /**
     * Busca un rol utilizando su nombre.
     */
    @Transactional(readOnly = true)
    public Rol buscarPorNombre(String nombre) {

        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("rol.no.encontrado.nombre", nombre)
                ));
    }

    /**
     * Elimina un rol únicamente cuando no está protegido
     * y no se encuentra asignado a ningún usuario.
     */
    public void eliminar(Long id) {

        // Comprobamos que el rol exista.
        Rol rol = buscarPorId(id);

        // Normalizamos el nombre para evitar diferencias de mayúsculas.
        String nombreRol = rol.getNombre()
                .trim()
                .toUpperCase(Locale.ROOT);

        // Impedimos eliminar los roles principales.
        if (ROLES_PROTEGIDOS.contains(nombreRol)) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("rol.protegido")
            );
        }

        // Comprobamos si existen usuarios asociados al rol.
        if (usuarioRepository.existsByRolId(id)) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("rol.asignado")
            );
        }

        // Eliminamos el rol cuando cumple todas las condiciones.
        rolRepository.delete(rol);
    }
}
