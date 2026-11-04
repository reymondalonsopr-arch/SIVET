
package com.sivet.service;

import com.sivet.model.Cliente;
import com.sivet.repository.ClienteRepository;
import com.sivet.repository.VentaRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de gestionar la lógica de negocio
 * relacionada con los clientes de SIVET.
 */
@Service
@Transactional
public class ClienteService {

    // Repositorio utilizado para acceder a los clientes.
    private final ClienteRepository clienteRepository;

    // Repositorio utilizado para consultar las ventas.
    private final VentaRepository ventaRepository;

    // Servicio encargado de recuperar mensajes personalizados.
    private final MensajeService mensajeService;

    /**
     * Inyección de dependencias mediante constructor.
     */
    public ClienteService(
            ClienteRepository clienteRepository,
            VentaRepository ventaRepository,
            MensajeService mensajeService) {

        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
        this.mensajeService = mensajeService;
    }

    /**
     * Registra un cliente nuevo.
     */
    public Cliente registrar(Cliente cliente) {

        // Validamos los datos obligatorios.
        validarDatos(cliente);

        // Limpiamos los espacios innecesarios.
        cliente.setNombre(cliente.getNombre().trim());

        // Normalizamos el documento cuando fue proporcionado.
        if (cliente.getNumeroDocumento() != null
                && !cliente.getNumeroDocumento().isBlank()) {

            cliente.setNumeroDocumento(
                    cliente.getNumeroDocumento().trim()
            );

            // Evitamos registrar documentos duplicados.
            if (clienteRepository.existsByNumeroDocumento(
                    cliente.getNumeroDocumento())) {

                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje("cliente.documento.duplicado")
                );
            }

        } else {
            // Permitimos registrar clientes sin documento.
            cliente.setNumeroDocumento(null);
        }

        // Guardamos el cliente en PostgreSQL.
        return clienteRepository.save(cliente);
    }

    /**
     * Obtiene todos los clientes registrados.
     */
    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    /**
     * Obtiene solamente los clientes activos.
     */
    @Transactional(readOnly = true)
    public List<Cliente> listarActivos() {
        return clienteRepository.findByActivoTrue();
    }

    /**
     * Busca un cliente por su identificador.
     */
    @Transactional(readOnly = true)
    public Cliente buscarPorId(Long id) {

        return clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("cliente.no.encontrado.id", id)
                ));
    }

    /**
     * Busca clientes por nombre o razón social.
     */
    @Transactional(readOnly = true)
    public List<Cliente> buscarPorNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            return listarTodos();
        }

        return clienteRepository.findByNombreContainingIgnoreCase(
                nombre.trim()
        );
    }

    /**
     * Actualiza los datos de un cliente existente.
     */
    public Cliente actualizar(Long id, Cliente datos) {

        // Buscamos el cliente que vamos a modificar.
        Cliente cliente = buscarPorId(id);

        // Validamos los datos recibidos.
        validarDatos(datos);

        // Normalizamos el nombre.
        String nombre = datos.getNombre().trim();

        // Normalizamos el documento si fue proporcionado.
        String documento = datos.getNumeroDocumento();

        if (documento != null && !documento.isBlank()) {

            documento = documento.trim();

            // Comprobamos que otro cliente no tenga ese documento.
            String documentoFinal = documento;

            clienteRepository.findByNumeroDocumento(documentoFinal)
                    .filter(existente -> !existente.getId().equals(id))
                    .ifPresent(existente -> {
                        throw new IllegalArgumentException(
                                mensajeService.obtenerMensaje(
                                        "cliente.documento.duplicado"
                                )
                        );
                    });

        } else {
            documento = null;
        }

        // Actualizamos los datos del cliente.
        cliente.setNombre(nombre);
        cliente.setNumeroDocumento(documento);
        cliente.setTelefono(datos.getTelefono());
        cliente.setCorreo(datos.getCorreo());
        cliente.setDireccion(datos.getDireccion());

        // Conservamos el estado actual de la cuenta.
        return clienteRepository.save(cliente);
    }

    /**
     * Activa o desactiva un cliente.
     */
    public Cliente cambiarEstado(Long id, boolean activo) {

        // Buscamos el cliente seleccionado.
        Cliente cliente = buscarPorId(id);

        // Actualizamos su estado.
        cliente.setActivo(activo);

        return clienteRepository.save(cliente);
    }

    /**
     * Elimina un cliente si no tiene ventas asociadas.
     */
    public void eliminar(Long id) {

        // Comprobamos que el cliente exista.
        Cliente cliente = buscarPorId(id);

        // Evitamos eliminar clientes que tengan ventas registradas.
        if (!ventaRepository.findByClienteId(id).isEmpty()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("cliente.tiene.ventas")
            );
        }

        // Eliminamos el cliente de la base de datos.
        clienteRepository.delete(cliente);
    }

    /**
     * Comprueba que los datos obligatorios sean válidos.
     */
    private void validarDatos(Cliente cliente) {

        if (cliente == null) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("cliente.datos.obligatorios")
            );
        }

        if (cliente.getNombre() == null
                || cliente.getNombre().isBlank()) {

            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("cliente.nombre.obligatorio")
            );
        }
    }
}
