
package com.sivet.repository;

import com.sivet.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio encargado de las operaciones de base de datos
 * relacionadas con los usuarios del sistema.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca un usuario mediante su nombre de usuario.
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    // Comprueba si un nombre de usuario ya está registrado.
    boolean existsByNombreUsuario(String nombreUsuario);

    // Busca un usuario que tenga su cuenta habilitada.
    Optional<Usuario> findByNombreUsuarioAndActivoTrue(String nombreUsuario);

    // Comprueba si un rol está asignado a algún usuario.
    boolean existsByRolId(Long rolId);
}
