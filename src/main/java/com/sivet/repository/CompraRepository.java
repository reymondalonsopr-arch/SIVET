
package com.sivet.repository;

import com.sivet.model.Compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio encargado de las operaciones de base de datos
 * relacionadas con las compras de SIVET.
 */
@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    // Busca todas las compras realizadas a un proveedor.
    List<Compra> findByProveedorId(Long proveedorId);

    // Busca las compras según su estado.
    List<Compra> findByEstado(String estado);

    // Obtiene las compras ordenadas desde la más reciente.
    List<Compra> findAllByOrderByFechaDesc();

    // Busca las compras realizadas dentro de un periodo de fechas.
    List<Compra> findByFechaBetweenOrderByFechaDesc(
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );
}
