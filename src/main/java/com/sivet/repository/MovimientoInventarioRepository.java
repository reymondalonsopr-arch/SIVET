
package com.sivet.repository;

import com.sivet.model.MovimientoInventario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio encargado de las operaciones de base de datos
 * relacionadas con los movimientos del inventario de SIVET.
 */
@Repository
public interface MovimientoInventarioRepository
        extends JpaRepository<MovimientoInventario, Long> {

    // Obtiene los movimientos de un producto específico.
    List<MovimientoInventario> findByProductoId(Long productoId);

    // Obtiene los movimientos según su tipo: ENTRADA o SALIDA.
    List<MovimientoInventario> findByTipo(String tipo);

    // Obtiene el historial ordenado desde el movimiento más reciente.
    List<MovimientoInventario> findAllByOrderByFechaDesc();

    // Obtiene los movimientos relacionados con una compra.
    List<MovimientoInventario> findByCompraId(Long compraId);
}
