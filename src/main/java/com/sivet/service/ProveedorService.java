
package com.sivet.service;

import com.sivet.model.Proveedor;
import com.sivet.repository.CompraRepository;
import com.sivet.repository.ProveedorRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de gestionar la lógica de negocio
 * relacionada con los proveedores de SIVET.
 */
@Service
@Transactional
public class ProveedorService {

    // Repositorio para acceder a los datos de los proveedores.
    private final ProveedorRepository proveedorRepository;

    // Repositorio para consultar las compras asociadas.
    private final CompraRepository compraRepository;

    // Servicio encargado de recuperar mensajes personalizados.
    private final MensajeService mensajeService;

    // Constructor para inyectar las dependencias.
    public ProveedorService(
            ProveedorRepository proveedorRepository,
            CompraRepository compraRepository,
            MensajeService mensajeService) {

        this.proveedorRepository = proveedorRepository;
        this.compraRepository = compraRepository;
        this.mensajeService = mensajeService;
    }

    // Registra un nuevo proveedor.
    public Proveedor registrar(Proveedor proveedor) {

        // Limpia los espacios innecesarios de los datos.
        proveedor.setNombre(proveedor.getNombre().trim());

        if (proveedor.getNumeroDocumento() != null) {
            proveedor.setNumeroDocumento(
                    proveedor.getNumeroDocumento().trim()
            );
        }

        if (proveedor.getTelefono() != null) {
            proveedor.setTelefono(proveedor.getTelefono().trim());
        }

        if (proveedor.getCorreo() != null) {
            proveedor.setCorreo(proveedor.getCorreo().trim());
        }

        if (proveedor.getDireccion() != null) {
            proveedor.setDireccion(proveedor.getDireccion().trim());
        }

        // Evita registrar documentos duplicados.
        if (proveedor.getNumeroDocumento() != null
                && !proveedor.getNumeroDocumento().isBlank()
                && proveedorRepository.existsByNumeroDocumento(
                        proveedor.getNumeroDocumento())) {

            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("proveedor.documento.duplicado")
            );
        }

        // Todo proveedor nuevo comienza activo.
        proveedor.setActivo(true);

        return proveedorRepository.save(proveedor);
    }

    // Obtiene todos los proveedores registrados.
    @Transactional(readOnly = true)
    public List<Proveedor> listarTodos() {
        return proveedorRepository.findAll();
    }

    // Obtiene solamente los proveedores activos.
    @Transactional(readOnly = true)
    public List<Proveedor> listarActivos() {
        return proveedorRepository.findByActivoTrue();
    }

    // Busca un proveedor mediante su identificador.
    @Transactional(readOnly = true)
    public Proveedor buscarPorId(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("proveedor.no.encontrado")
                ));
    }

    // Busca proveedores por nombre.
    @Transactional(readOnly = true)
    public List<Proveedor> buscarPorNombre(String nombre) {
        return proveedorRepository.findByNombreContainingIgnoreCase(
                nombre.trim()
        );
    }

    // Actualiza los datos de un proveedor existente.
    public Proveedor actualizar(Long id, Proveedor datosActualizados) {

        // Recupera el proveedor que se desea modificar.
        Proveedor proveedor = buscarPorId(id);

        // Limpia el nombre recibido.
        String nombre = datosActualizados.getNombre().trim();

        // Limpia el documento recibido, si existe.
        String documento = datosActualizados.getNumeroDocumento();

        if (documento != null) {
            documento = documento.trim();

            if (documento.isBlank()) {
                documento = null;
            }
        }

        // Comprueba que el documento no pertenezca a otro proveedor.
        if (documento != null) {
            Proveedor proveedorExistente =
                    proveedorRepository.findByNumeroDocumento(documento)
                            .orElse(null);

            if (proveedorExistente != null
                    && !proveedorExistente.getId().equals(id)) {

                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje(
                                "proveedor.documento.duplicado.actualizacion"
                        )
                );
            }
        }

        // Actualiza los datos del proveedor.
        proveedor.setNombre(nombre);
        proveedor.setNumeroDocumento(documento);
        proveedor.setTelefono(datosActualizados.getTelefono());
        proveedor.setCorreo(datosActualizados.getCorreo());
        proveedor.setDireccion(datosActualizados.getDireccion());

        return proveedorRepository.save(proveedor);
    }

    // Cambia el estado de un proveedor entre activo e inactivo.
    public void cambiarEstado(Long id) {

        // Recupera el proveedor solicitado.
        Proveedor proveedor = buscarPorId(id);

        // Invierte su estado actual.
        proveedor.setActivo(!proveedor.isActivo());

        proveedorRepository.save(proveedor);
    }

    // Elimina definitivamente un proveedor si no tiene compras asociadas.
    public void eliminar(Long id) {

        // Comprueba que el proveedor exista.
        Proveedor proveedor = buscarPorId(id);

        // Impide eliminar proveedores que tengan compras registradas.
        if (!compraRepository.findByProveedorId(id).isEmpty()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("proveedor.tiene.compras")
            );
        }

        // Elimina el proveedor cuando no tiene compras relacionadas.
        proveedorRepository.delete(proveedor);
    }
}
