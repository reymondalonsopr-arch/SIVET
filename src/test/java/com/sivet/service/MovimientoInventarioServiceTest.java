package com.sivet.service;

import com.sivet.model.Compra;
import com.sivet.model.MovimientoInventario;
import com.sivet.model.Producto;
import com.sivet.repository.MovimientoInventarioRepository;
import com.sivet.repository.ProductoRepository;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoInventarioServiceTest {

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private MensajeService mensajeService;

    @InjectMocks
    private MovimientoInventarioService movimientoService;

    // ---------------------------------------------------------
    // ENTRADAS
    // ---------------------------------------------------------

    @Test
    void registrarEntradaDebeAumentarStockYGuardarMovimiento() {
        Producto producto = crearProductoActivo(10);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventario resultado =
                movimientoService.registrarEntrada(1L, 5, "  Reposición  ");

        assertEquals(15, producto.getStockActual());
        assertEquals("ENTRADA", resultado.getTipo());
        assertEquals(5, resultado.getCantidad());
        assertEquals("Reposición", resultado.getMotivo());
        assertNotNull(resultado.getFecha());

        verify(productoRepository).save(producto);
        verify(movimientoRepository).save(resultado);
    }

    @Test
    void registrarEntradaPorCompraDebeAsociarCompra() {
        Producto producto = crearProductoActivo(10);
        Compra compra = new Compra();
        compra.setId(7L);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventario resultado =
                movimientoService.registrarEntradaPorCompra(producto, 4, compra);

        assertEquals(14, producto.getStockActual());
        assertEquals(compra, resultado.getCompra());
        assertEquals("Entrada por compra COM-7", resultado.getMotivo());
    }

    @Test
    void registrarEntradaPorAnulacionVentaPermiteProductoInactivo() {
        Producto producto = crearProductoActivo(5);
        producto.setEstado(false);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventario resultado =
                movimientoService.registrarEntradaPorAnulacionVenta(producto, 2, 9L);

        assertEquals(7, producto.getStockActual());
        assertEquals("ENTRADA", resultado.getTipo());
        assertEquals("Anulación de venta VEN-9", resultado.getMotivo());
    }

    // ---------------------------------------------------------
    // SALIDAS
    // ---------------------------------------------------------

    @Test
    void registrarSalidaDebeDisminuirStockYGuardarMovimiento() {
        Producto producto = crearProductoActivo(10);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventario resultado =
                movimientoService.registrarSalida(1L, 3, "Venta manual");

        assertEquals(7, producto.getStockActual());
        assertEquals("SALIDA", resultado.getTipo());
        assertEquals(3, resultado.getCantidad());

        verify(productoRepository).save(producto);
        verify(movimientoRepository).save(resultado);
    }

    @Test
    void registrarSalidaPorAnulacionCompraDebeAsociarCompra() {
        Producto producto = crearProductoActivo(10);
        Compra compra = new Compra();
        compra.setId(4L);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovimientoInventario resultado =
                movimientoService.registrarSalidaPorAnulacionCompra(
                        producto, 2, compra
                );

        assertEquals(8, producto.getStockActual());
        assertEquals(compra, resultado.getCompra());
        assertEquals("Anulación de compra COM-4", resultado.getMotivo());
    }

    @Test
    void registrarSalidaDebeRechazarStockInsuficiente() {
        Producto producto = crearProductoActivo(2);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThrows(
                IllegalArgumentException.class,
                () -> movimientoService.registrarSalida(1L, 3, "Venta")
        );

        verify(productoRepository, never()).save(any(Producto.class));
        verify(movimientoRepository, never()).save(any(MovimientoInventario.class));
    }

    // ---------------------------------------------------------
    // VALIDACIONES
    // ---------------------------------------------------------

    @Test
    void registrarMovimientoDebeRechazarCantidadNula() {
        assertThrows(
                IllegalArgumentException.class,
                () -> movimientoService.registrarEntrada(1L, null, "Reposición")
        );

        verifyNoInteractions(productoRepository, movimientoRepository);
    }

    @Test
    void registrarMovimientoDebeRechazarCantidadCero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> movimientoService.registrarEntrada(1L, 0, "Reposición")
        );
    }

    @Test
    void registrarMovimientoDebeRechazarCantidadNegativa() {
        assertThrows(
                IllegalArgumentException.class,
                () -> movimientoService.registrarSalida(1L, -1, "Venta")
        );
    }

    @Test
    void registrarMovimientoDebeRechazarMotivoNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> movimientoService.registrarEntrada(1L, 1, null)
        );

        verifyNoInteractions(productoRepository, movimientoRepository);
    }

    @Test
    void registrarMovimientoDebeRechazarMotivoEnBlanco() {
        assertThrows(
                IllegalArgumentException.class,
                () -> movimientoService.registrarEntrada(1L, 1, "   ")
        );
    }

    @Test
    void registrarMovimientoDebeRechazarProductoInexistente() {
        when(productoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> movimientoService.registrarEntrada(1L, 1, "Reposición")
        );

        verify(productoRepository, never()).save(any(Producto.class));
        verify(movimientoRepository, never()).save(any(MovimientoInventario.class));
    }

    @Test
    void registrarMovimientoDebeRechazarProductoInactivo() {
        Producto producto = crearProductoActivo(10);
        producto.setEstado(false);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThrows(
                IllegalArgumentException.class,
                () -> movimientoService.registrarEntrada(1L, 1, "Reposición")
        );

        verify(productoRepository, never()).save(any(Producto.class));
        verify(movimientoRepository, never()).save(any(MovimientoInventario.class));
    }

    // ---------------------------------------------------------
    // CONSULTAS
    // ---------------------------------------------------------

    @Test
    void listarTodosDebeRetornarMovimientosOrdenados() {
        List<MovimientoInventario> movimientos =
                List.of(new MovimientoInventario(), new MovimientoInventario());

        when(movimientoRepository.findAllByOrderByFechaDesc())
                .thenReturn(movimientos);

        assertEquals(movimientos, movimientoService.listarTodos());
    }

    @Test
    void listarPorProductoDebeRetornarMovimientosDelProducto() {
        List<MovimientoInventario> movimientos =
                List.of(new MovimientoInventario());

        when(movimientoRepository.findByProductoId(1L))
                .thenReturn(movimientos);

        assertEquals(movimientos, movimientoService.listarPorProducto(1L));
    }

    @Test
    void listarEntradasDebeRetornarMovimientosDeEntrada() {
        List<MovimientoInventario> movimientos =
                List.of(new MovimientoInventario());

        when(movimientoRepository.findByTipo("ENTRADA"))
                .thenReturn(movimientos);

        assertEquals(movimientos, movimientoService.listarEntradas());
    }

    @Test
    void listarSalidasDebeRetornarMovimientosDeSalida() {
        List<MovimientoInventario> movimientos =
                List.of(new MovimientoInventario());

        when(movimientoRepository.findByTipo("SALIDA"))
                .thenReturn(movimientos);

        assertEquals(movimientos, movimientoService.listarSalidas());
    }

    // ---------------------------------------------------------
    // MÉTODO AUXILIAR
    // ---------------------------------------------------------

    private Producto crearProductoActivo(Integer stock) {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Producto de prueba");
        producto.setEstado(true);
        producto.setStockActual(stock);
        return producto;
    }
}