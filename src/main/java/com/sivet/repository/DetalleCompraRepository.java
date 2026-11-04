
package com.sivet.repository;

import com.sivet.model.DetalleCompra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio encargado de las operaciones de base de datos
 * relacionadas con los detalles de las compras de SIVET.
 */
@Repository
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {

    // Obtiene todos los detalles pertenecientes a una compra.
    List<DetalleCompra> findByCompraId(Long compraId);

    // Obtiene todos los detalles asociados a un producto.
    List<DetalleCompra> findByProductoId(Long productoId);
}
