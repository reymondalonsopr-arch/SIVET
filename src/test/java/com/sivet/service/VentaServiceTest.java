package com.sivet.service;

import com.sivet.model.Cliente;
import com.sivet.model.DetalleVenta;
import com.sivet.model.Producto;
import com.sivet.model.Venta;
import com.sivet.repository.ClienteRepository;
import com.sivet.repository.DetalleVentaRepository;
import com.sivet.repository.ProductoRepository;
import com.sivet.repository.VentaRepository;

import jakarta.persistence.EntityNotFoundException;

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
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private DetalleVentaRepository detalleVentaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private MovimientoInventarioService movimientoService;

    @Mock
    private MensajeService mensajeService;

    @InjectMocks
    private VentaService ventaService;

    // ---------------------------------------------------------
    // REGISTRAR
    // ---------------------------------------------------------

    @Test
    void registrarDebeGuardarVentaDetallesYRegistrarSalidas() {
        Producto producto = crearProductoActivo(10);
        Venta venta = new Venta();
        DetalleVenta detalle = crearDetalle(producto, 2);

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        when(ventaRepository.save(any(Venta.class)))
                .thenAnswer(invocation -> {
                    Venta guardada = invocation.getArgument(0);
                    guardada.setId(1L);
                    return guardada;
                });

        when(detalleVentaRepository.save(any(DetalleVenta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Venta resultado = ventaService.registrar(venta, List.of(detalle));

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals(new BigDecimal("20.00"), resultado.getTotal());
        assertEquals("REGISTRADA", resultado.getEstado());

        verify(ventaRepository).save(venta);
        verify(detalleVentaRepository).save(detalle);
        verify(movimientoService)
                .registrarSalida(1L, 2, "Salida por venta VEN-1");
    }

    @Test
    void registrarDebeRechazarVentaSinDetalles() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.registrar(new Venta(), List.of())
        );

        verify(ventaRepository, never()).save(any());
    }

    @Test
    void registrarDebeRechazarClienteInexistente() {
        Cliente cliente = crearClienteActivo();
        Venta venta = new Venta();
        venta.setCliente(cliente);

        when(clienteRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> ventaService.registrar(
                        venta,
                        List.of(crearDetalle(crearProductoActivo(10), 1))
                )
        );
    }

    @Test
    void registrarDebeRechazarClienteInactivo() {
        Cliente cliente = crearClienteActivo();
        cliente.setActivo(false);

        Venta venta = new Venta();
        venta.setCliente(cliente);

        when(clienteRepository.findById(1L))
                .thenReturn(Optional.of(cliente));

        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.registrar(
                        venta,
                        List.of(crearDetalle(crearProductoActivo(10), 1))
                )
        );
    }

    @Test
    void registrarDebePermitirVentaSinCliente() {
        Producto producto = crearProductoActivo(5);
        Venta venta = new Venta();

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        when(ventaRepository.save(any(Venta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(detalleVentaRepository.save(any(DetalleVenta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Venta resultado = ventaService.registrar(
                venta,
                List.of(crearDetalle(producto, 1))
        );

        assertNull(resultado.getCliente());
        verify(ventaRepository).save(venta);
    }

    @Test
    void registrarDebeRechazarDetalleSinProducto() {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setCantidad(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.registrar(new Venta(), List.of(detalle))
        );
    }

    @Test
    void registrarDebeRechazarProductoInexistente() {
        Producto producto = crearProductoActivo(10);

        when(productoRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> ventaService.registrar(
                        new Venta(),
                        List.of(crearDetalle(producto, 1))
                )
        );
    }

    @Test
    void registrarDebeRechazarProductoInactivo() {
        Producto producto = crearProductoActivo(10);
        producto.setEstado(false);

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.registrar(
                        new Venta(),
                        List.of(crearDetalle(producto, 1))
                )
        );
    }

    @Test
    void registrarDebeRechazarCantidadNula() {
        Producto producto = crearProductoActivo(10);

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.registrar(
                        new Venta(),
                        List.of(crearDetalle(producto, null))
                )
        );
    }

    @Test
    void registrarDebeRechazarCantidadNoPositiva() {
        Producto producto = crearProductoActivo(10);

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.registrar(
                        new Venta(),
                        List.of(crearDetalle(producto, 0))
                )
        );
    }

    @Test
    void registrarDebeRechazarStockInsuficiente() {
        Producto producto = crearProductoActivo(1);

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.registrar(
                        new Venta(),
                        List.of(crearDetalle(producto, 2))
                )
        );

        verify(ventaRepository, never()).save(any());
    }

    @Test
    void registrarDebeRechazarPrecioNulo() {
        Producto producto = crearProductoActivo(10);
        producto.setPrecioVenta(null);

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.registrar(
                        new Venta(),
                        List.of(crearDetalle(producto, 1))
                )
        );
    }

    @Test
    void registrarDebeRechazarPrecioNoPositivo() {
        Producto producto = crearProductoActivo(10);
        producto.setPrecioVenta(BigDecimal.ZERO);

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.registrar(
                        new Venta(),
                        List.of(crearDetalle(producto, 1))
                )
        );
    }

    // ---------------------------------------------------------
    // ANULAR
    // ---------------------------------------------------------

    @Test
    void anularDebeAnularVentaYDevolverProductosAlInventario() {
        Venta venta = crearVentaRegistrada();
        Producto producto = crearProductoActivo(5);
        DetalleVenta detalle = crearDetalle(producto, 2);

        when(ventaRepository.findById(1L))
                .thenReturn(Optional.of(venta));

        when(detalleVentaRepository.findByVentaId(1L))
                .thenReturn(List.of(detalle));

        when(ventaRepository.save(venta))
                .thenReturn(venta);

        Venta resultado = ventaService.anular(1L);

        assertEquals("ANULADA", resultado.getEstado());

        verify(movimientoService)
                .registrarEntradaPorAnulacionVenta(producto, 2, 1L);

        verify(ventaRepository).save(venta);
    }

    @Test
    void anularDebeRechazarVentaYaAnulada() {
        Venta venta = crearVentaRegistrada();
        venta.setEstado("ANULADA");

        when(ventaRepository.findById(1L))
                .thenReturn(Optional.of(venta));

        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.anular(1L)
        );

        verify(detalleVentaRepository, never()).findByVentaId(anyLong());
    }

    @Test
    void anularDebeRechazarVentaConEstadoNoAnulable() {
        Venta venta = crearVentaRegistrada();
        venta.setEstado("PENDIENTE");

        when(ventaRepository.findById(1L))
                .thenReturn(Optional.of(venta));

        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.anular(1L)
        );
    }

    @Test
    void anularDebeRechazarVentaSinDetalles() {
        Venta venta = crearVentaRegistrada();

        when(ventaRepository.findById(1L))
                .thenReturn(Optional.of(venta));

        when(detalleVentaRepository.findByVentaId(1L))
                .thenReturn(List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.anular(1L)
        );

        verify(ventaRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // CONSULTAS
    // ---------------------------------------------------------

    @Test
    void listarTodasDebeRetornarVentasDelRepositorio() {
        List<Venta> ventas = List.of(new Venta(), new Venta());

        when(ventaRepository.findAllByOrderByFechaDesc())
                .thenReturn(ventas);

        assertEquals(ventas, ventaService.listarTodas());
    }

    @Test
    void buscarPorIdDebeRetornarVentaExistente() {
        Venta venta = crearVentaRegistrada();

        when(ventaRepository.findById(1L))
                .thenReturn(Optional.of(venta));

        assertEquals(venta, ventaService.buscarPorId(1L));
    }

    @Test
    void buscarPorIdDebeLanzarExcepcionSiNoExiste() {
        when(ventaRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> ventaService.buscarPorId(1L)
        );
    }

    @Test
    void listarDetallesDebeRetornarDetallesDeVentaExistente() {
        Venta venta = crearVentaRegistrada();
        List<DetalleVenta> detalles = List.of(new DetalleVenta());

        when(ventaRepository.findById(1L))
                .thenReturn(Optional.of(venta));

        when(detalleVentaRepository.findByVentaId(1L))
                .thenReturn(detalles);

        assertEquals(detalles, ventaService.listarDetalles(1L));
    }

    @Test
    void listarPorClienteDebeRetornarVentasDelCliente() {
        List<Venta> ventas = List.of(new Venta());

        when(ventaRepository.findByClienteId(1L))
                .thenReturn(ventas);

        assertEquals(ventas, ventaService.listarPorCliente(1L));
    }

    // ---------------------------------------------------------
    // MÉTODOS AUXILIARES
    // ---------------------------------------------------------

    private Producto crearProductoActivo(Integer stock) {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Producto de prueba");
        producto.setEstado(true);
        producto.setStockActual(stock);
        producto.setPrecioVenta(new BigDecimal("10.00"));
        return producto;
    }

    private Cliente crearClienteActivo() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setActivo(true);
        return cliente;
    }

    private DetalleVenta crearDetalle(Producto producto, Integer cantidad) {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        return detalle;
    }

    private Venta crearVentaRegistrada() {
        Venta venta = new Venta();
        venta.setId(1L);
        venta.setEstado("REGISTRADA");
        venta.setTotal(new BigDecimal("20.00"));
        return venta;
    }
}