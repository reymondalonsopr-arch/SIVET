
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

        // Creamos una categoría real para evitar simulaciones innecesarias.
        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Bebidas");
        categoria.setActivo(true);

        // Preparamos un producto para las pruebas.
        producto = new Producto();

        // El servicio genera el código automáticamente.
        producto.setCodigo("PROD-001");

        producto.setNombre("  Producto de prueba  ");
        producto.setPrecioCompra(new BigDecimal("10.00"));
        producto.setPrecioVenta(new BigDecimal("15.00"));
        producto.setStockActual(5);
        producto.setStockMinimo(2);
        producto.setCategoria(categoria);
    }

    // =========================================================
    // PRUEBAS DE REGISTRO
    // =========================================================

    @Test
    void registrarProductoValidoDebeGuardarProducto() {

        // Simulamos que la categoría existe y está activa.
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        // Simulamos la lista de categorías registradas.
        when(categoriaRepository.findAll())
                .thenReturn(List.of(categoria));

        // Simulamos que no existen productos con el prefijo B.
        when(productoRepository.findByCodigoStartingWithIgnoreCase("B"))
                .thenReturn(List.of());

        // Comprobamos que el código B001 esté disponible.
        when(productoRepository.existsByCodigo("B001"))
                .thenReturn(false);

        // Simulamos el guardado del producto.
        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Ejecutamos el registro.
        Producto resultado = productoService.registrar(producto);

        // Comprobamos los datos del producto registrado.
        assertEquals("B001", resultado.getCodigo());
        assertEquals("Producto de prueba", resultado.getNombre());
        assertEquals(0, resultado.getStockActual());
        assertTrue(resultado.isEstado());
        assertSame(categoria, resultado.getCategoria());

        // Verificamos que se haya guardado.
        verify(productoRepository).save(producto);
    }

    @Test
    void registrarProductoConCodigoExistenteDebeGenerarSiguienteCodigo() {

        // Simulamos que la categoría existe y está activa.
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        // Simulamos las categorías registradas.
        when(categoriaRepository.findAll())
                .thenReturn(List.of(categoria));

        // Simulamos que ya existe un producto con código B001.
        Producto productoExistente = new Producto();
        productoExistente.setCodigo("B001");

        when(productoRepository.findByCodigoStartingWithIgnoreCase("B"))
                .thenReturn(List.of(productoExistente));

        // El siguiente código disponible es B002.
        when(productoRepository.existsByCodigo("B002"))
                .thenReturn(false);

        // Simulamos el guardado.
        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Ejecutamos el registro.
        Producto resultado = productoService.registrar(producto);

        // Comprobamos que se generó el siguiente código.
        assertEquals("B002", resultado.getCodigo());

        // Verificamos que se guardó el producto.
        verify(productoRepository).save(producto);
    }

    // =========================================================
    // PRUEBAS DE CONSULTA
    // =========================================================

    @Test
    void buscarPorIdInexistenteDebeLanzarExcepcion() {

        // Simulamos que el producto no existe.
        when(productoRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Comprobamos que se lance la excepción correspondiente.
        assertThrows(
                EntityNotFoundException.class,
                () -> productoService.buscarPorId(99L)
        );

        // Verificamos la consulta.
        verify(productoRepository).findById(99L);
    }

    @Test
    void listarActivosDebeConsultarRepositorio() {

        // Preparamos una lista simulada de productos.
        List<Producto> productos = List.of(producto);

        when(productoRepository.findByEstadoTrue())
                .thenReturn(productos);

        // Ejecutamos la consulta.
        List<Producto> resultado = productoService.listarActivos();

        // Comprobamos los resultados.
        assertEquals(1, resultado.size());
        assertSame(producto, resultado.get(0));

        // Verificamos que se consultó el repositorio.
        verify(productoRepository).findByEstadoTrue();
    }

    // =========================================================
    // PRUEBAS DE ELIMINACIÓN
    // =========================================================

    @Test
    void eliminarProductoConStockDebeLanzarExcepcion() {

        // Simulamos que el producto existe y tiene stock.
        producto.setId(1L);
        producto.setStockActual(5);

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        // No debe permitirse eliminar un producto con existencias.
        assertThrows(
                IllegalArgumentException.class,
                () -> productoService.eliminar(1L)
        );

        // No debe consultar el historial.
        verifyNoInteractions(
                detalleCompraRepository,
                detalleVentaRepository,
                movimientoInventarioRepository
        );

        // Tampoco debe eliminar el producto.
        verify(productoRepository, never())
                .delete(any(Producto.class));
    }

    @Test
    void eliminarProductoSinStockNiHistorialDebeEliminarlo() {

        // Simulamos un producto sin existencias.
        producto.setId(2L);
        producto.setStockActual(0);

        when(productoRepository.findById(2L))
                .thenReturn(Optional.of(producto));

        // Simulamos que no tiene compras relacionadas.
        when(detalleCompraRepository.findByProductoId(2L))
                .thenReturn(List.of());

        // Simulamos que no tiene ventas relacionadas.
        when(detalleVentaRepository.findByProductoId(2L))
                .thenReturn(List.of());

        // Simulamos que no tiene movimientos de inventario.
        when(movimientoInventarioRepository.findByProductoId(2L))
                .thenReturn(List.of());

        // Ejecutamos la eliminación.
        assertDoesNotThrow(
                () -> productoService.eliminar(2L)
        );

        // Verificamos que se eliminó el producto.
        verify(productoRepository).delete(producto);
    }
}
