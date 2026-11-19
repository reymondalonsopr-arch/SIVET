
package com.sivet.config;

import com.sivet.model.Rol;
import com.sivet.model.Usuario;
import com.sivet.repository.RolRepository;
import com.sivet.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${SIVET_ADMIN_PASSWORD:}")
    private String adminPassword;

    public DataInitializer(
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {

        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // Crear roles si todavía no existen.
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

        // La contraseña debe estar configurada.
        if (adminPassword == null || adminPassword.isBlank()) {

            if (usuarioRepository.findByNombreUsuario("admin").isEmpty()) {
                throw new IllegalStateException(
                        "Debes configurar SIVET_ADMIN_PASSWORD para crear el administrador."
                );
            }

            System.out.println(
                    "SIVET: administrador existente. No se modificó su contraseña."
            );
            return;
        }

        // Buscar el administrador existente.
        Usuario administrador = usuarioRepository
                .findByNombreUsuario("admin")
                .orElse(null);

        if (administrador == null) {

            // Crear administrador si no existe.
            administrador = new Usuario();
            administrador.setNombreCompleto(
                    "Administrador del sistema"
            );
            administrador.setNombreUsuario("admin");

            System.out.println(
                    "SIVET: creando cuenta administradora."
            );

        } else {

            System.out.println(
                    "SIVET: administrador encontrado. Actualizando contraseña."
            );
        }

        // Actualizar contraseña utilizando BCrypt.
        administrador.setPassword(
                passwordEncoder.encode(adminPassword)
        );

        // Garantizar rol y estado activo del administrador.
        administrador.setRol(rolAdmin);
        administrador.setActivo(true);

        // Guardar cambios en PostgreSQL.
        usuarioRepository.save(administrador);

        System.out.println(
                "SIVET: administrador configurado correctamente."
        );
    }

    private Rol crearRolSiNoExiste(
            String nombre,
            String descripcion) {

        return rolRepository.findByNombre(nombre)
                .orElseGet(() -> {

                    Rol nuevoRol = new Rol();
                    nuevoRol.setNombre(nombre);
                    nuevoRol.setDescripcion(descripcion);

                    return rolRepository.save(nuevoRol);
                });
    }
}
