
package com.sivet.service;

import com.sivet.model.Compra;
import com.sivet.model.Proveedor;
import com.sivet.repository.CompraRepository;
import com.sivet.repository.ProveedorRepository;

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
 * Pruebas unitarias del servicio de proveedores.
 * Se utilizan mocks para evitar acceder a la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private MensajeService mensajeService;

    @InjectMocks
    private ProveedorService proveedorService;

    private Proveedor proveedor;

    @BeforeEach
    void configurar() {
        proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Proveedor de prueba");
        proveedor.setNumeroDocumento("20123456789");
        proveedor.setTelefono("987654321");
        proveedor.setCorreo("proveedor@correo.com");
        proveedor.setDireccion("Av. Principal 123");
        proveedor.setActivo(true);
    }

    @Test
    void registrarDebeLimpiarDatosYGuardarProveedorActivo() {
        Proveedor nuevo = new Proveedor();
        nuevo.setNombre("  Proveedor nuevo  ");
        nuevo.setNumeroDocumento("  20123456789  ");
        nuevo.setTelefono("  987654321  ");
        nuevo.setCorreo("  proveedor@correo.com  ");
        nuevo.setDireccion("  Av. Principal 123  ");

        when(proveedorRepository.existsByNumeroDocumento("20123456789"))
                .thenReturn(false);
        when(proveedorRepository.save(nuevo)).thenReturn(nuevo);

        Proveedor resultado = proveedorService.registrar(nuevo);

        assertEquals("Proveedor nuevo", resultado.getNombre());
        assertEquals("20123456789", resultado.getNumeroDocumento());
        assertEquals("987654321", resultado.getTelefono());
        assertEquals("proveedor@correo.com", resultado.getCorreo());
        assertEquals("Av. Principal 123", resultado.getDireccion());
        assertTrue(resultado.isActivo());

        verify(proveedorRepository).save(nuevo);
    }

    @Test
    void registrarDebePermitirDocumentoNulo() {
        Proveedor nuevo = new Proveedor();
        nuevo.setNombre("Proveedor sin documento");

        when(proveedorRepository.save(nuevo)).thenReturn(nuevo);

        Proveedor resultado = proveedorService.registrar(nuevo);

        assertNull(resultado.getNumeroDocumento());
        assertTrue(resultado.isActivo());
        verify(proveedorRepository, never())
                .existsByNumeroDocumento(anyString());
        verify(proveedorRepository).save(nuevo);
    }

    @Test
    void registrarDebeLanzarExcepcionSiDocumentoEstaDuplicado() {
        proveedor.setNumeroDocumento("20123456789");

        when(proveedorRepository.existsByNumeroDocumento("20123456789"))
                .thenReturn(true);
        when(mensajeService.obtenerMensaje("proveedor.documento.duplicado"))
                .thenReturn("El documento ya está registrado");

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> proveedorService.registrar(proveedor)
        );

        assertEquals(
                "El documento ya está registrado",
                excepcion.getMessage()
        );
        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }

    @Test
    void listarTodosDebeRetornarTodosLosProveedores() {
        List<Proveedor> proveedores = List.of(proveedor);

        when(proveedorRepository.findAll()).thenReturn(proveedores);

        List<Proveedor> resultado = proveedorService.listarTodos();

        assertEquals(proveedores, resultado);
        verify(proveedorRepository).findAll();
    }

    @Test
    void listarActivosDebeRetornarProveedoresActivos() {
        List<Proveedor> proveedores = List.of(proveedor);

        when(proveedorRepository.findByActivoTrue()).thenReturn(proveedores);

        List<Proveedor> resultado = proveedorService.listarActivos();

        assertEquals(proveedores, resultado);
        verify(proveedorRepository).findByActivoTrue();
    }

    @Test
    void buscarPorIdDebeRetornarProveedorCuandoExiste() {
        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));

        Proveedor resultado = proveedorService.buscarPorId(1L);

        assertEquals(proveedor, resultado);
        verify(proveedorRepository).findById(1L);
    }

    @Test
    void buscarPorIdDebeLanzarExcepcionCuandoNoExiste() {
        when(proveedorRepository.findById(99L))
                .thenReturn(Optional.empty());
        when(mensajeService.obtenerMensaje("proveedor.no.encontrado"))
                .thenReturn("Proveedor no encontrado");

        EntityNotFoundException excepcion = assertThrows(
                EntityNotFoundException.class,
                () -> proveedorService.buscarPorId(99L)
        );

        assertEquals("Proveedor no encontrado", excepcion.getMessage());
    }

    @Test
    void buscarPorNombreDebeLimpiarElTextoAntesDeBuscar() {
        List<Proveedor> proveedores = List.of(proveedor);

        when(proveedorRepository.findByNombreContainingIgnoreCase("Proveedor"))
                .thenReturn(proveedores);

        List<Proveedor> resultado =
                proveedorService.buscarPorNombre("  Proveedor  ");

        assertEquals(proveedores, resultado);
        verify(proveedorRepository)
                .findByNombreContainingIgnoreCase("Proveedor");
    }

    @Test
    void actualizarDebeModificarDatosYGuardarProveedor() {
        Proveedor datos = new Proveedor();
        datos.setNombre("  Nuevo nombre  ");
        datos.setNumeroDocumento("  20987654321  ");
        datos.setTelefono("999888777");
        datos.setCorreo("nuevo@correo.com");
        datos.setDireccion("Nueva dirección");

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));
        when(proveedorRepository.findByNumeroDocumento("20987654321"))
                .thenReturn(Optional.empty());
        when(proveedorRepository.save(proveedor)).thenReturn(proveedor);

        Proveedor resultado = proveedorService.actualizar(1L, datos);

        assertEquals("Nuevo nombre", resultado.getNombre());
        assertEquals("20987654321", resultado.getNumeroDocumento());
        assertEquals("999888777", resultado.getTelefono());
        assertEquals("nuevo@correo.com", resultado.getCorreo());
        assertEquals("Nueva dirección", resultado.getDireccion());

        verify(proveedorRepository).save(proveedor);
    }

    @Test
    void actualizarDebePermitirMantenerElMismoDocumento() {
        Proveedor datos = new Proveedor();
        datos.setNombre("Proveedor actualizado");
        datos.setNumeroDocumento("20123456789");

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));
        when(proveedorRepository.findByNumeroDocumento("20123456789"))
                .thenReturn(Optional.of(proveedor));
        when(proveedorRepository.save(proveedor)).thenReturn(proveedor);

        Proveedor resultado = proveedorService.actualizar(1L, datos);

        assertEquals("Proveedor actualizado", resultado.getNombre());
        assertEquals("20123456789", resultado.getNumeroDocumento());
        verify(proveedorRepository).save(proveedor);
    }

    @Test
    void actualizarDebeLanzarExcepcionSiDocumentoPerteneceAOtroProveedor() {
        Proveedor otro = new Proveedor();
        otro.setId(2L);
        otro.setNumeroDocumento("20987654321");

        Proveedor datos = new Proveedor();
        datos.setNombre("Proveedor actualizado");
        datos.setNumeroDocumento("20987654321");

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));
        when(proveedorRepository.findByNumeroDocumento("20987654321"))
                .thenReturn(Optional.of(otro));
        when(mensajeService.obtenerMensaje(
                "proveedor.documento.duplicado.actualizacion"))
                .thenReturn("El documento pertenece a otro proveedor");

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> proveedorService.actualizar(1L, datos)
        );

        assertEquals(
                "El documento pertenece a otro proveedor",
                excepcion.getMessage()
        );
        verify(proveedorRepository, never()).save(any(Proveedor.class));
    }

    @Test
    void actualizarDebeConvertirDocumentoEnBlancoANull() {
        Proveedor datos = new Proveedor();
        datos.setNombre("Proveedor actualizado");
        datos.setNumeroDocumento("   ");

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));
        when(proveedorRepository.save(proveedor)).thenReturn(proveedor);

        Proveedor resultado = proveedorService.actualizar(1L, datos);

        assertNull(resultado.getNumeroDocumento());
        verify(proveedorRepository, never())
                .findByNumeroDocumento(anyString());
        verify(proveedorRepository).save(proveedor);
    }

    @Test
    void cambiarEstadoDebeDesactivarProveedorActivo() {
        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));
        when(proveedorRepository.save(proveedor)).thenReturn(proveedor);

        proveedorService.cambiarEstado(1L);

        assertFalse(proveedor.isActivo());
        verify(proveedorRepository).save(proveedor);
    }

    @Test
    void cambiarEstadoDebeActivarProveedorInactivo() {
        proveedor.setActivo(false);

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));
        when(proveedorRepository.save(proveedor)).thenReturn(proveedor);

        proveedorService.cambiarEstado(1L);

        assertTrue(proveedor.isActivo());
        verify(proveedorRepository).save(proveedor);
    }

    @Test
    void eliminarDebeBorrarProveedorSiNoTieneCompras() {
        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));
        when(compraRepository.findByProveedorId(1L))
                .thenReturn(List.of());

        proveedorService.eliminar(1L);

        verify(proveedorRepository).delete(proveedor);
    }

    @Test
    void eliminarDebeLanzarExcepcionSiTieneCompras() {
        Compra compra = new Compra();

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));
        when(compraRepository.findByProveedorId(1L))
                .thenReturn(List.of(compra));
        when(mensajeService.obtenerMensaje("proveedor.tiene.compras"))
                .thenReturn("El proveedor tiene compras asociadas");

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> proveedorService.eliminar(1L)
        );

        assertEquals(
                "El proveedor tiene compras asociadas",
                excepcion.getMessage()
        );
        verify(proveedorRepository, never()).delete(any(Proveedor.class));
    }
}
