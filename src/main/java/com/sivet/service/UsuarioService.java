package com.sivet.service;

import com.sivet.model.Rol;
import com.sivet.model.Usuario;
import com.sivet.repository.RolRepository;
import com.sivet.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Servicio encargado de gestionar los usuarios de SIVET.
 *
 * Contiene la lógica de negocio para registrar, consultar,
 * actualizar, cambiar el estado y eliminar cuentas.
 */
@Service
@Transactional
public class UsuarioService {

    // Roles oficiales permitidos en el sistema.
    private static final Set<String> ROLES_PERMITIDOS =
            Set.of("ADMIN", "VENDEDOR", "ALMACENERO");

    // Límites de longitud definidos para los datos del usuario.
    private static final int NOMBRE_COMPLETO_MIN = 2;
    private static final int NOMBRE_COMPLETO_MAX = 120;
    private static final int NOMBRE_USUARIO_MIN = 3;
    private static final int NOMBRE_USUARIO_MAX = 50;
    private static final int PASSWORD_MIN = 6;
    private static final int PASSWORD_MAX = 100;

    // Repositorio para acceder a los usuarios.
    private final UsuarioRepository usuarioRepository;

    // Repositorio para consultar los roles disponibles.
    private final RolRepository rolRepository;

    // Componente encargado de cifrar las contraseñas.
    private final PasswordEncoder passwordEncoder;

    // Servicio encargado de recuperar mensajes personalizados.
    private final MensajeService mensajeService;

    /**
     * Inyección de dependencias mediante constructor.
     */
    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            MensajeService mensajeService) {

        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.mensajeService = mensajeService;
    }

    /**
     * Registra un usuario nuevo.
     */
    public Usuario registrar(
            String nombreCompleto,
            String nombreUsuario,
            String password,
            Long rolId) {

        // Validamos los datos recibidos antes de guardarlos.
        validarDatos(nombreCompleto, nombreUsuario, password);

        // Eliminamos espacios sobrantes en los campos de texto.
        nombreCompleto = nombreCompleto.trim();
        nombreUsuario = nombreUsuario.trim();

        // Comprobamos que el nombre de usuario no esté registrado.
        if (usuarioRepository.existsByNombreUsuario(nombreUsuario)) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("usuario.nombre.duplicado")
            );
        }

        // Buscamos y validamos el rol seleccionado.
        Rol rol = buscarRolPermitido(rolId);

        // Construimos la entidad del nuevo usuario.
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setNombreUsuario(nombreUsuario);

        // Nunca almacenamos la contraseña original.
        usuario.setPassword(passwordEncoder.encode(password));

        // Asignamos el rol seleccionado.
        usuario.setRol(rol);

        // Las cuentas nuevas comienzan habilitadas.
        usuario.setActivo(true);

        // Guardamos el usuario en PostgreSQL.
        return usuarioRepository.save(usuario);
    }

    /**
     * Obtiene todos los usuarios registrados.
     */
    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    /**
     * Busca un usuario por su identificador.
     */
    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {

        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("usuario.no.encontrado.id", id)
                ));
    }

    /**
     * Busca un usuario por su nombre de acceso.
     */
    @Transactional(readOnly = true)
    public Usuario buscarPorNombreUsuario(String nombreUsuario) {

        return usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("usuario.no.encontrado.nombre", nombreUsuario)
                ));
    }

    /**
     * Actualiza los datos generales de un usuario.
     *
     * La contraseña no se modifica desde este método.
     */
    public Usuario actualizar(
            Long id,
            String nombreCompleto,
            String nombreUsuario,
            Long rolId) {

        // Buscamos el usuario que vamos a modificar.
        Usuario usuario = buscarPorId(id);

        // Validamos los datos que se actualizarán.
        validarDatos(nombreCompleto, nombreUsuario, null);

        // Eliminamos espacios sobrantes en los campos de texto.
        nombreCompleto = nombreCompleto.trim();
        nombreUsuario = nombreUsuario.trim();

        // Evitamos que otro usuario tenga el mismo nombre de acceso.
        usuarioRepository.findByNombreUsuario(nombreUsuario)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new IllegalArgumentException(
                            mensajeService.obtenerMensaje("usuario.nombre.duplicado")
                    );
                });

        // Comprobamos que el nuevo rol sea válido.
        Rol rol = buscarRolPermitido(rolId);

        // Actualizamos los datos del usuario.
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setRol(rol);

        return usuarioRepository.save(usuario);
    }

    /**
     * Activa o desactiva una cuenta de usuario.
     */
    public Usuario cambiarEstado(Long id, boolean activo) {

        // Buscamos el usuario que cambiará de estado.
        Usuario usuario = buscarPorId(id);

        // Evitamos desactivar al último administrador activo.
        if (!activo && esAdministradorActivo(usuario)) {
            validarQueNoSeaUltimoAdministrador(usuario);
        }

        // Actualizamos el estado de la cuenta.
        usuario.setActivo(activo);

        return usuarioRepository.save(usuario);
    }

    /**
     * Elimina un usuario existente.
     */
    public void eliminar(Long id) {

        // Buscamos el usuario para comprobar que exista.
        Usuario usuario = buscarPorId(id);

        // Evitamos eliminar al último administrador activo.
        if (esAdministradorActivo(usuario)) {
            validarQueNoSeaUltimoAdministrador(usuario);
        }

        // Eliminamos el usuario de la base de datos.
        usuarioRepository.delete(usuario);
    }

    /**
     * Comprueba si el usuario es un administrador activo.
     */
    private boolean esAdministradorActivo(Usuario usuario) {

        return usuario.isActivo()
                && usuario.getRol() != null
                && "ADMIN".equals(usuario.getRol().getNombre());
    }

    /**
     * Impide desactivar o eliminar al último administrador activo.
     */
    private void validarQueNoSeaUltimoAdministrador(Usuario usuario) {

        long administradoresActivos = usuarioRepository.findAll()
                .stream()
                .filter(Usuario::isActivo)
                .filter(u -> u.getRol() != null)
                .filter(u -> "ADMIN".equals(u.getRol().getNombre()))
                .count();

        if (administradoresActivos <= 1) {
            throw new IllegalArgumentException(
                    "No se puede desactivar ni eliminar al último administrador activo."
            );
        }
    }

    /**
     * Busca un rol y comprueba que pertenezca a los perfiles oficiales.
     */
    private Rol buscarRolPermitido(Long rolId) {

        // Comprobamos que se haya seleccionado un rol.
        if (rolId == null) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("usuario.rol.obligatorio")
            );
        }

        // Buscamos el rol en PostgreSQL.
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("usuario.rol.no.encontrado")
                ));

        // Evitamos asignar perfiles que no pertenecen a SIVET.
        if (!ROLES_PERMITIDOS.contains(rol.getNombre())) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("usuario.rol.invalido")
            );
        }

        return rol;
    }

    /**
     * Comprueba que los datos obligatorios y sus longitudes sean válidos.
     */
    private void validarDatos(
            String nombreCompleto,
            String nombreUsuario,
            String password) {

        // Validamos que el nombre completo no esté vacío.
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("usuario.nombre.completo.obligatorio")
            );
        }

        // Validamos la longitud del nombre completo después de quitar espacios.
        int longitudNombre = nombreCompleto.trim().length();

        if (longitudNombre < NOMBRE_COMPLETO_MIN
                || longitudNombre > NOMBRE_COMPLETO_MAX) {
            throw new IllegalArgumentException(
                    "El nombre completo debe tener entre 2 y 120 caracteres."
            );
        }

        // Validamos que el nombre de usuario no esté vacío.
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("usuario.nombre.usuario.obligatorio")
            );
        }

        // Validamos la longitud del nombre de usuario.
        int longitudUsuario = nombreUsuario.trim().length();

        if (longitudUsuario < NOMBRE_USUARIO_MIN
                || longitudUsuario > NOMBRE_USUARIO_MAX) {
            throw new IllegalArgumentException(
                    "El nombre de usuario debe tener entre 3 y 50 caracteres."
            );
        }

        // La contraseña solamente es obligatoria al registrar.
        if (password != null) {

            if (password.isBlank()) {
                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje("usuario.password.obligatorio")
                );
            }

            // Validamos la longitud sin modificar la contraseña.
            if (password.length() < PASSWORD_MIN
                    || password.length() > PASSWORD_MAX) {
                throw new IllegalArgumentException(
                        "La contraseña debe tener entre 6 y 100 caracteres."
                );
            }
        }
    }
}
