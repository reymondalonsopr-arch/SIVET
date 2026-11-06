
package com.sivet.repository;

import com.sivet.model.Producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio encargado de las operaciones de base de datos
 * relacionadas con los productos de SIVET.
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Busca un producto mediante su código único.
    Optional<Producto> findByCodigo(String codigo);

    // Comprueba si ya existe un producto con ese código.
    boolean existsByCodigo(String codigo);

    // Busca productos cuyo código contenga el texto ingresado.
    List<Producto> findByCodigoContainingIgnoreCase(String codigo);

    // Obtiene los códigos que comienzan con un prefijo determinado.
    List<Producto> findByCodigoStartingWithIgnoreCase(String prefijo);

    // Obtiene solamente los productos activos.
    List<Producto> findByEstadoTrue();

    // Busca productos por su nombre, sin distinguir mayúsculas.
    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    // Obtiene los productos pertenecientes a una categoría.
    List<Producto> findByCategoriaId(Long categoriaId);

    // Busca productos cuyo stock actual sea menor o igual a su stock mínimo.
    @Query("SELECT p FROM Producto p WHERE p.stockActual <= p.stockMinimo")
    List<Producto> buscarProductosConStockBajo();
}