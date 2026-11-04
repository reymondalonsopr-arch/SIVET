
package com.sivet.repository;

import com.sivet.model.Proveedor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio encargado de las operaciones de base de datos
 * relacionadas con los proveedores de SIVET.
 */
@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // Busca un proveedor mediante su número de documento.
    Optional<Proveedor> findByNumeroDocumento(String numeroDocumento);

    // Comprueba si ya existe un proveedor con ese documento.
    boolean existsByNumeroDocumento(String numeroDocumento);

    // Obtiene solamente los proveedores activos.
    List<Proveedor> findByActivoTrue();

    // Busca proveedores por nombre sin distinguir mayúsculas.
    List<Proveedor> findByNombreContainingIgnoreCase(String nombre);
}
