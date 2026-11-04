package com.sivet.repository;

import com.sivet.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio encargado de las operaciones de base de datos
 * relacionadas con los roles del sistema.
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    // Busca un rol por su nombre.
    Optional<Rol> findByNombre(String nombre);

    // Comprueba si ya existe un rol con ese nombre.
    boolean existsByNombre(String nombre);
}