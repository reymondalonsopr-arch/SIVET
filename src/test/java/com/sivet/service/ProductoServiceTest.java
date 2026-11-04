package com.sivet.service;

import com.sivet.model.Categoria;
import com.sivet.model.Producto;
import com.sivet.repository.CategoriaRepository;
import com.sivet.repository.DetalleCompraRepository;
import com.sivet.repository.DetalleVentaRepository;
import com.sivet.repository.MovimientoInventarioRepository;
import com.sivet.repository.ProductoRepository;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private DetalleCompraRepository detalleCompraRepository;

    @Mock
    private DetalleVentaRepository detalleVentaRepository;

    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Mock
    private MensajeService mensajeService;

    @InjectMocks
    private ProductoService productoService;

    private Producto producto;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        // Creamos una categoría simulada.
        categoria = mock(Categoria.class);

        // Preparamos un producto para las pruebas.
        producto = new Producto();
        producto.setCodigo(" PROD-001 ");
        producto.setNombre("  Producto de prueba  ");
        producto.setPrecioCompra(new BigDecimal("10.00"));
        producto.setPrecioVenta(new BigDecimal("15.00"));
        producto.setStockActual(5);
        producto.setStockMinimo(2);
        producto.setCategoria(categoria);
    }

    @Test
    void registrarProductoValidoDebeGuardarProducto() {
        // Configuramos la categoría activa para esta prueba.
        when(categoria.getId()).thenReturn(1L);
        when(categoria.isActivo()).thenReturn(true);

        // Simulamos que el código no existe y la categoría sí existe.
        when(productoRepository.existsByCodigo("PROD-001")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Ejecutamos el registro.
        Producto resultado = productoService.registrar(producto);

        // Verificamos los datos normalizados y los valores iniciales.
        assertEquals("PROD-001", resultado.getCodigo());
        assertEquals("Producto de prueba", resultado.getNombre());
        assertEquals(0, resultado.getStockActual());
        assertTrue(resultado.isEstado());
        assertSame(categoria, resultado.getCategoria());

        verify(productoRepository).save(producto);
    }

    @Test
    void registrarProductoConCodigoDuplicadoDebeLanzarExcepcion() {
        // Simulamos que el código ya está registrado.
        when(productoRepository.existsByCodigo("PROD-001")).thenReturn(true);

        // El servicio debe rechazar el registro.
        assertThrows(
                IllegalArgumentException.class,
                () -> productoService.registrar(producto)
        );

        // No debe guardar el producto duplicado.
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    void buscarPorIdInexistenteDebeLanzarExcepcion() {
        // Simulamos que no existe un producto con ese identificador.
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> productoService.buscarPorId(99L)
        );
    }

    @Test
    void listarActivosDebeConsultarRepositorio() {
        // Preparamos la lista simulada.
        List<Producto> productos = List.of(producto);
        when(productoRepository.findByEstadoTrue()).thenReturn(productos);

        // Ejecutamos y verificamos el resultado.
        List<Producto> resultado = productoService.listarActivos();

        assertEquals(1, resultado.size());
        assertSame(producto, resultado.get(0));
        verify(productoRepository).findByEstadoTrue();
    }

    @Test
    void eliminarProductoConStockDebeLanzarExcepcion() {
        // Simulamos que el producto existe y tiene stock.
        producto.setId(1L);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThrows(
                IllegalArgumentException.class,
                () -> productoService.eliminar(1L)
        );

        // No debe consultar el historial ni eliminarlo.
        verifyNoInteractions(
                detalleCompraRepository,
                detalleVentaRepository,
                movimientoInventarioRepository
        );
        verify(productoRepository, never()).delete(any(Producto.class));
    }

    @Test
    void eliminarProductoSinStockNiHistorialDebeEliminarlo() {
        // Simulamos un producto sin stock ni movimientos relacionados.
        producto.setId(2L);
        producto.setStockActual(0);

        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));
        when(detalleCompraRepository.findByProductoId(2L)).thenReturn(List.of());
        when(detalleVentaRepository.findByProductoId(2L)).thenReturn(List.of());
        when(movimientoInventarioRepository.findByProductoId(2L)).thenReturn(List.of());

        // Ejecutamos la eliminación.
        assertDoesNotThrow(() -> productoService.eliminar(2L));

        // Verificamos que se eliminó el producto.
        verify(productoRepository).delete(producto);
    }
}