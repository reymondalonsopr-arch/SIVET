
package com.sivet.repository;

import com.sivet.model.Categoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio encargado de las operaciones de base de datos
 * relacionadas con las categorías de productos.
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Busca una categoría por su nombre.
    Optional<Categoria> findByNombre(String nombre);

    // Comprueba si ya existe una categoría con ese nombre.
    boolean existsByNombre(String nombre);

    // Obtiene solamente las categorías activas.
    List<Categoria> findByActivoTrue();

    // Busca categorías cuyo nombre contenga un texto.
    List<Categoria> findByNombreContainingIgnoreCase(String nombre);
}
