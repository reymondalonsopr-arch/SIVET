package com.sivet.repository;

import com.sivet.model.DetalleVenta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio encargado de las operaciones de base de datos
 * relacionadas con los detalles de las ventas de SIVET.
 */
@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    // Obtiene todos los detalles pertenecientes a una venta.
    List<DetalleVenta> findByVentaId(Long ventaId);

    // Obtiene todos los detalles asociados a un producto.
    List<DetalleVenta> findByProductoId(Long productoId);

    // Consulta los productos más vendidos dentro de un periodo.
    @Query("""
        SELECT dv.producto.nombre, SUM(dv.cantidad)
        FROM DetalleVenta dv
        WHERE dv.venta.fecha BETWEEN :fechaInicio AND :fechaFin
          AND dv.venta.estado = 'REGISTRADA'
        GROUP BY dv.producto.id, dv.producto.nombre
        ORDER BY SUM(dv.cantidad) DESC
    """)
    List<Object[]> obtenerProductosMasVendidos(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );
}