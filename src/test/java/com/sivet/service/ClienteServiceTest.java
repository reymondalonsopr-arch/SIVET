
package com.sivet.service;

import com.sivet.model.Cliente;
import com.sivet.repository.ClienteRepository;
import com.sivet.repository.VentaRepository;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio ClienteService.
 * Se simulan los repositorios para no acceder a la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private MensajeService mensajeService;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Ana Pérez");
        cliente.setNumeroDocumento("12345678");
        cliente.setTelefono("987654321");
        cliente.setCorreo("ana@example.com");
        cliente.setDireccion("Ica");
        cliente.setActivo(true);
    }

    @Test
    void registrarClienteValidoDebeGuardarCliente() {
        // Arrange
        when(clienteRepository.existsByNumeroDocumento("12345678"))
                .thenReturn(false);
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Cliente resultado = clienteService.registrar(cliente);

        // Assert
        assertNotNull(resultado);
        assertEquals("Ana Pérez", resultado.getNombre());
        assertEquals("12345678", resultado.getNumeroDocumento());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void registrarClienteConNombreConEspaciosDebeLimpiarNombre() {
        // Arrange
        cliente.setNombre("  Ana Pérez  ");
        cliente.setNumeroDocumento(null);
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Cliente resultado = clienteService.registrar(cliente);

        // Assert
        assertEquals("Ana Pérez", resultado.getNombre());
        assertNull(resultado.getNumeroDocumento());
        verify(clienteRepository, never()).existsByNumeroDocumento(anyString());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void registrarClienteConDocumentoDuplicadoDebeLanzarExcepcion() {
        // Arrange
        when(clienteRepository.existsByNumeroDocumento("12345678"))
                .thenReturn(true);
        when(mensajeService.obtenerMensaje("cliente.documento.duplicado"))
                .thenReturn("El documento ya está registrado");

        // Act y Assert
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> clienteService.registrar(cliente)
        );

        assertEquals("El documento ya está registrado", excepcion.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void registrarClienteNuloDebeLanzarExcepcion() {
        // Arrange
        when(mensajeService.obtenerMensaje("cliente.datos.obligatorios"))
                .thenReturn("Los datos del cliente son obligatorios");

        // Act y Assert
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> clienteService.registrar(null)
        );

        assertEquals("Los datos del cliente son obligatorios", excepcion.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void registrarClienteSinNombreDebeLanzarExcepcion() {
        // Arrange
        cliente.setNombre(" ");
        when(mensajeService.obtenerMensaje("cliente.nombre.obligatorio"))
                .thenReturn("El nombre es obligatorio");

        // Act y Assert
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> clienteService.registrar(cliente)
        );

        assertEquals("El nombre es obligatorio", excepcion.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void listarTodosDebeRetornarClientes() {
        // Arrange
        List<Cliente> clientes = List.of(cliente);
        when(clienteRepository.findAll()).thenReturn(clientes);

        // Act
        List<Cliente> resultado = clienteService.listarTodos();

        // Assert
        assertSame(clientes, resultado);
        verify(clienteRepository).findAll();
    }

    @Test
    void listarActivosDebeRetornarClientesActivos() {
        // Arrange
        List<Cliente> clientes = List.of(cliente);
        when(clienteRepository.findByActivoTrue()).thenReturn(clientes);

        // Act
        List<Cliente> resultado = clienteService.listarActivos();

        // Assert
        assertSame(clientes, resultado);
        verify(clienteRepository).findByActivoTrue();
    }

    @Test
    void buscarPorIdExistenteDebeRetornarCliente() {
        // Arrange
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        // Act
        Cliente resultado = clienteService.buscarPorId(1L);

        // Assert
        assertSame(cliente, resultado);
    }

    @Test
    void buscarPorIdInexistenteDebeLanzarExcepcion() {
        // Arrange
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());
        when(mensajeService.obtenerMensaje("cliente.no.encontrado.id", 99L))
                .thenReturn("Cliente no encontrado");

        // Act y Assert
        EntityNotFoundException excepcion = assertThrows(
                EntityNotFoundException.class,
                () -> clienteService.buscarPorId(99L)
        );

        assertEquals("Cliente no encontrado", excepcion.getMessage());
    }

    @Test
    void buscarPorNombreDebeBuscarIgnorandoMayusculas() {
        // Arrange
        when(clienteRepository.findByNombreContainingIgnoreCase("Ana"))
                .thenReturn(List.of(cliente));

        // Act
        List<Cliente> resultado = clienteService.buscarPorNombre(" Ana ");

        // Assert
        assertEquals(1, resultado.size());
        verify(clienteRepository).findByNombreContainingIgnoreCase("Ana");
    }

    @Test
    void buscarPorNombreVacioDebeListarTodos() {
        // Arrange
        when(clienteRepository.findAll()).thenReturn(List.of(cliente));

        // Act
        List<Cliente> resultado = clienteService.buscarPorNombre(" ");

        // Assert
        assertEquals(1, resultado.size());
        verify(clienteRepository).findAll();
        verify(clienteRepository, never()).findByNombreContainingIgnoreCase(anyString());
    }

    @Test
    void actualizarClienteDebeGuardarCambios() {
        // Arrange
        Cliente datos = new Cliente();
        datos.setNombre("  Ana María  ");
        datos.setNumeroDocumento("87654321");
        datos.setTelefono("912345678");
        datos.setCorreo("anamaria@example.com");
        datos.setDireccion("Lima");

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByNumeroDocumento("87654321"))
                .thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Cliente resultado = clienteService.actualizar(1L, datos);

        // Assert
        assertEquals("Ana María", resultado.getNombre());
        assertEquals("87654321", resultado.getNumeroDocumento());
        assertEquals("912345678", resultado.getTelefono());
        assertEquals("anamaria@example.com", resultado.getCorreo());
        assertEquals("Lima", resultado.getDireccion());
        assertTrue(resultado.isActivo());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void actualizarConDocumentoDeOtroClienteDebeLanzarExcepcion() {
        // Arrange
        Cliente otroCliente = new Cliente();
        otroCliente.setId(2L);
        otroCliente.setNumeroDocumento("87654321");

        Cliente datos = new Cliente();
        datos.setNombre("Ana Pérez");
        datos.setNumeroDocumento("87654321");

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByNumeroDocumento("87654321"))
                .thenReturn(Optional.of(otroCliente));
        when(mensajeService.obtenerMensaje("cliente.documento.duplicado"))
                .thenReturn("El documento ya está registrado");

        // Act y Assert
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> clienteService.actualizar(1L, datos)
        );

        assertEquals("El documento ya está registrado", excepcion.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void actualizarConMismoDocumentoDebePermitirGuardar() {
        // Arrange
        Cliente datos = new Cliente();
        datos.setNombre("Ana Pérez Actualizada");
        datos.setNumeroDocumento("12345678");

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByNumeroDocumento("12345678"))
                .thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Cliente resultado = clienteService.actualizar(1L, datos);

        // Assert
        assertEquals("Ana Pérez Actualizada", resultado.getNombre());
        assertEquals("12345678", resultado.getNumeroDocumento());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void cambiarEstadoDebeActualizarCliente() {
        // Arrange
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Cliente resultado = clienteService.cambiarEstado(1L, false);

        // Assert
        assertFalse(resultado.isActivo());
        verify(clienteRepository).save(cliente);
    }

    @Test
    void eliminarClienteSinVentasDebeEliminarlo() {
        // Arrange
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(ventaRepository.findByClienteId(1L)).thenReturn(List.of());

        // Act
        clienteService.eliminar(1L);

        // Assert
        verify(clienteRepository).delete(cliente);
    }

    @Test
    void eliminarClienteConVentasDebeLanzarExcepcion() {
        // Arrange
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(ventaRepository.findByClienteId(1L))
                .thenReturn(List.of(mock(com.sivet.model.Venta.class)));
        when(mensajeService.obtenerMensaje("cliente.tiene.ventas"))
                .thenReturn("El cliente tiene ventas asociadas");

        // Act y Assert
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> clienteService.eliminar(1L)
        );

        assertEquals("El cliente tiene ventas asociadas", excepcion.getMessage());
        verify(clienteRepository, never()).delete(any(Cliente.class));
    }
}
