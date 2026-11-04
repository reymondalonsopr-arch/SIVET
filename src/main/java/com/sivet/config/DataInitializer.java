
package com.sivet.config;

import com.sivet.model.Rol;
import com.sivet.model.Usuario;
import com.sivet.repository.RolRepository;
import com.sivet.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Inicializa los datos esenciales de SIVET.
 *
 * Crea los tres roles del sistema y, cuando todavía
 * no existen usuarios, registra la cuenta administradora.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    // Repositorios para guardar roles y usuarios.
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    // Componente para cifrar las contraseñas.
    private final PasswordEncoder passwordEncoder;

    // Contraseña inicial obtenida de una variable de entorno.
    @Value("${SIVET_ADMIN_PASSWORD:}")
    private String adminPassword;

    /**
     * Inyección de dependencias mediante constructor.
     */
    public DataInitializer(
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Método ejecutado automáticamente al iniciar Spring Boot.
     */
    @Override
    public void run(String... args) {

        // Creamos los tres roles necesarios para SIVET.
        Rol rolAdmin = crearRolSiNoExiste(
                "ADMIN",
                "Administrador con acceso completo al sistema."
        );

        crearRolSiNoExiste(
                "VENDEDOR",
                "Usuario encargado de gestionar clientes y ventas."
        );

        crearRolSiNoExiste(
                "ALMACENERO",
                "Usuario encargado de productos, compras e inventario."
        );

        // Si ya existen usuarios, no creamos otra cuenta administradora.
        if (usuarioRepository.count() > 0) {
            return;
        }

        // Exigimos una contraseña configurada por el usuario.
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Debes configurar la variable SIVET_ADMIN_PASSWORD."
            );
        }

        // Construimos la primera cuenta administradora.
        Usuario administrador = new Usuario();
        administrador.setNombreCompleto("Administrador del sistema");
        administrador.setNombreUsuario("admin");

        // Guardamos únicamente la contraseña cifrada.
        administrador.setPassword(
                passwordEncoder.encode(adminPassword)
        );

        // Asignamos el rol administrador.
        administrador.setRol(rolAdmin);
        administrador.setActivo(true);

        // Guardamos la cuenta inicial en PostgreSQL.
        usuarioRepository.save(administrador);

        System.out.println(
                "SIVET: cuenta inicial de administrador creada."
        );
    }

    /**
     * Busca un rol por su nombre y lo crea si no existe.
     */
    private Rol crearRolSiNoExiste(String nombre, String descripcion) {

        return rolRepository.findByNombre(nombre)
                .orElseGet(() -> {

                    // Construimos el nuevo rol.
                    Rol nuevoRol = new Rol();
                    nuevoRol.setNombre(nombre);
                    nuevoRol.setDescripcion(descripcion);

                    // Guardamos el rol en PostgreSQL.
                    return rolRepository.save(nuevoRol);
                });
    }
}
