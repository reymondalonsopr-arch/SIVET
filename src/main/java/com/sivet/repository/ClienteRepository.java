
package com.sivet.repository;

import com.sivet.model.Cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio encargado de las operaciones de base de datos
 * relacionadas con los clientes de SIVET.
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Busca un cliente por su número de documento.
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    // Comprueba si un documento ya está registrado.
    boolean existsByNumeroDocumento(String numeroDocumento);

    // Busca clientes que estén habilitados.
    List<Cliente> findByActivoTrue();

    // Busca clientes por su nombre o razón social.
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
}
