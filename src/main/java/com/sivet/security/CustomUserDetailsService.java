
package com.sivet.security;

import com.sivet.model.Usuario;
import com.sivet.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que permite a Spring Security consultar
 * los usuarios registrados en la base de datos SIVET.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    // Repositorio encargado de consultar los usuarios.
    private final UsuarioRepository usuarioRepository;

    /**
     * Inyección del repositorio mediante constructor.
     */
    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Busca un usuario por su nombre de acceso y construye
     * el objeto UserDetails que necesita Spring Security.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nombreUsuario)
            throws UsernameNotFoundException {

        // Buscamos el usuario en PostgreSQL.
        Usuario usuario = usuarioRepository
                .findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe el usuario: " + nombreUsuario
                ));

        // Obtenemos el rol asignado al usuario.
        // El prefijo ROLE_ permite utilizar hasRole() posteriormente.
        String autoridad = "ROLE_" + usuario.getRol().getNombre();

        // Convertimos nuestra entidad Usuario al formato
        // que Spring Security utiliza para autenticar.
        return User.withUsername(usuario.getNombreUsuario())
                .password(usuario.getPassword())

                // Entregamos la autoridad como texto individual.
                .authorities(autoridad)

                // Impedimos el acceso si la cuenta está desactivada.
                .disabled(!usuario.isActivo())

                .build();
    }
}
