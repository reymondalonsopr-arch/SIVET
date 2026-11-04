
package com.sivet.repository;

import com.sivet.model.Venta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio encargado de las operaciones de base de datos
 * relacionadas con las ventas de SIVET.
 */
@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // Busca todas las ventas realizadas a un cliente.
    List<Venta> findByClienteId(Long clienteId);

    // Busca las ventas según su estado.
    List<Venta> findByEstado(String estado);

    // Obtiene las ventas ordenadas desde la más reciente.
    List<Venta> findAllByOrderByFechaDesc();

    // Busca las ventas realizadas dentro de un periodo de fechas.
    List<Venta> findByFechaBetweenOrderByFechaDesc(
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );
}
