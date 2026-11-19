package com.sivet.service;

import com.sivet.model.Compra;
import com.sivet.model.DetalleCompra;
import com.sivet.model.MovimientoInventario;
import com.sivet.model.Producto;
import com.sivet.model.Proveedor;
import com.sivet.repository.CompraRepository;
import com.sivet.repository.DetalleCompraRepository;
import com.sivet.repository.MovimientoInventarioRepository;
import com.sivet.repository.ProductoRepository;
import com.sivet.repository.ProveedorRepository;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de compras.
 * Se utilizan mocks para evitar acceder a la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private DetalleCompraRepository detalleCompraRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Mock
    private MovimientoInventarioService movimientoInventarioService;

    @Mock
    private MensajeService mensajeService;

    @InjectMocks
    private CompraService compraService;

    // ---------------------------------------------------------
    // REGISTRAR
    // ---------------------------------------------------------

    @Test
    void registrarDebeGuardarCompraYDetallesCuandoDatosSonValidos() {
        Proveedor proveedor = crearProveedorActivo();
        Producto producto = crearProductoActivo();

        Compra compra = new Compra();
        compra.setProveedor(proveedor);

        DetalleCompra detalle = crearDetalle(producto, 3, "10.00");

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        when(compraRepository.save(any(Compra.class)))
                .thenAnswer(invocation -> {
                    Compra compraGuardada = invocation.getArgument(0);
                    compraGuardada.setId(1L);
                    return compraGuardada;
                });

        when(detalleCompraRepository.save(any(DetalleCompra.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Compra resultado = compraService.registrar(compra, List.of(detalle));

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals(new BigDecimal("30.00"), resultado.getTotal());
        assertEquals("REGISTRADA", resultado.getEstado());

        verify(compraRepository).save(compra);
        verify(detalleCompraRepository).save(detalle);

        verify(movimientoInventarioService)
                .registrarEntradaPorCompra(producto, 3, resultado);
    }

    @Test
    void registrarDebeRechazarCompraSinDetalles() {
        Compra compra = new Compra();

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.registrar(compra, List.of())
        );

        verify(compraRepository, never()).save(any());
    }

    @Test
    void registrarDebeRechazarCompraSinProveedor() {
        Compra compra = new Compra();
        DetalleCompra detalle =
                crearDetalle(crearProductoActivo(), 1, "10.00");

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.registrar(compra, List.of(detalle))
        );

        verify(compraRepository, never()).save(any());
    }

    @Test
    void registrarDebeRechazarProveedorInexistente() {
        Proveedor proveedor = crearProveedorActivo();

        Compra compra = new Compra();
        compra.setProveedor(proveedor);

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> compraService.registrar(
                        compra,
                        List.of(crearDetalle(
                                crearProductoActivo(),
                                1,
                                "10.00"
                        ))
                )
        );
    }

    @Test
    void registrarDebeRechazarProveedorInactivo() {
        Proveedor proveedor = crearProveedorActivo();
        proveedor.setActivo(false);

        Compra compra = new Compra();
        compra.setProveedor(proveedor);

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.registrar(
                        compra,
                        List.of(crearDetalle(
                                crearProductoActivo(),
                                1,
                                "10.00"
                        ))
                )
        );
    }

    @Test
    void registrarDebeRechazarDetalleSinProducto() {
        Proveedor proveedor = crearProveedorActivo();

        Compra compra = new Compra();
        compra.setProveedor(proveedor);

        DetalleCompra detalle = new DetalleCompra();
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(new BigDecimal("10.00"));

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.registrar(compra, List.of(detalle))
        );
    }

    @Test
    void registrarDebeRechazarProductoInexistente() {
        Proveedor proveedor = crearProveedorActivo();
        Producto producto = crearProductoActivo();

        Compra compra = new Compra();
        compra.setProveedor(proveedor);

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));

        when(productoRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> compraService.registrar(
                        compra,
                        List.of(crearDetalle(producto, 1, "10.00"))
                )
        );
    }

    @Test
    void registrarDebeRechazarProductoInactivo() {
        Proveedor proveedor = crearProveedorActivo();
        Producto producto = crearProductoActivo();

        // El estado del producto es booleano.
        producto.setEstado(false);

        Compra compra = new Compra();
        compra.setProveedor(proveedor);

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.registrar(
                        compra,
                        List.of(crearDetalle(producto, 1, "10.00"))
                )
        );
    }

    @Test
    void registrarDebeRechazarCantidadNoPositiva() {
        Proveedor proveedor = crearProveedorActivo();
        Producto producto = crearProductoActivo();

        Compra compra = new Compra();
        compra.setProveedor(proveedor);

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.registrar(
                        compra,
                        List.of(crearDetalle(producto, 0, "10.00"))
                )
        );
    }

    @Test
    void registrarDebeRechazarPrecioNoPositivo() {
        Proveedor proveedor = crearProveedorActivo();
        Producto producto = crearProductoActivo();

        Compra compra = new Compra();
        compra.setProveedor(proveedor);

        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor));

        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto));

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.registrar(
                        compra,
                        List.of(crearDetalle(producto, 1, "0.00"))
                )
        );
    }

    // ---------------------------------------------------------
    // ANULAR
    // ---------------------------------------------------------

    @Test
    void anularDebeAnularCompraYRegistrarSalidas() {
        Compra compra = crearCompraRegistrada();
        Producto producto = crearProductoActivo();
        producto.setStockActual(10);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setTipo("ENTRADA");
        movimiento.setProducto(producto);
        movimiento.setCantidad(3);

        when(compraRepository.findById(1L))
                .thenReturn(Optional.of(compra));

        when(movimientoInventarioRepository.findByCompraId(1L))
                .thenReturn(List.of(movimiento));

        when(compraRepository.save(compra))
                .thenReturn(compra);

        Compra resultado = compraService.anular(1L);

        assertEquals("ANULADA", resultado.getEstado());

        verify(movimientoInventarioService)
                .registrarSalidaPorAnulacionCompra(
                        producto,
                        3,
                        compra
                );

        verify(compraRepository).save(compra);
    }

    @Test
    void anularDebeRechazarCompraYaAnulada() {
        Compra compra = crearCompraRegistrada();
        compra.setEstado("ANULADA");

        when(compraRepository.findById(1L))
                .thenReturn(Optional.of(compra));

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.anular(1L)
        );

        verify(movimientoInventarioRepository, never())
                .findByCompraId(anyLong());
    }

    @Test
    void anularDebeRechazarCompraConEstadoNoAnulable() {
        Compra compra = crearCompraRegistrada();
        compra.setEstado("PENDIENTE");

        when(compraRepository.findById(1L))
                .thenReturn(Optional.of(compra));

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.anular(1L)
        );
    }

    @Test
    void anularDebeRechazarCompraSinMovimientosDeEntrada() {
        Compra compra = crearCompraRegistrada();

        MovimientoInventario salida = new MovimientoInventario();
        salida.setTipo("SALIDA");

        when(compraRepository.findById(1L))
                .thenReturn(Optional.of(compra));

        when(movimientoInventarioRepository.findByCompraId(1L))
                .thenReturn(List.of(salida));

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.anular(1L)
        );
    }

    @Test
    void anularDebeRechazarCuandoStockEsInsuficiente() {
        Compra compra = crearCompraRegistrada();
        Producto producto = crearProductoActivo();
        producto.setStockActual(1);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setTipo("ENTRADA");
        movimiento.setProducto(producto);
        movimiento.setCantidad(3);

        when(compraRepository.findById(1L))
                .thenReturn(Optional.of(compra));

        when(movimientoInventarioRepository.findByCompraId(1L))
                .thenReturn(List.of(movimiento));

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.anular(1L)
        );

        verify(movimientoInventarioService, never())
                .registrarSalidaPorAnulacionCompra(
                        any(),
                        any(),
                        any()
                );
    }

    // ---------------------------------------------------------
    // CONSULTAS
    // ---------------------------------------------------------

    @Test
    void listarTodasDebeRetornarComprasDelRepositorio() {
        List<Compra> compras = List.of(
                new Compra(),
                new Compra()
        );

        when(compraRepository.findAllByOrderByFechaDesc())
                .thenReturn(compras);

        assertEquals(compras, compraService.listarTodas());
    }

    @Test
    void buscarPorIdDebeRetornarCompraExistente() {
        Compra compra = crearCompraRegistrada();

        when(compraRepository.findById(1L))
                .thenReturn(Optional.of(compra));

        assertEquals(compra, compraService.buscarPorId(1L));
    }

    @Test
    void buscarPorIdDebeLanzarExcepcionCuandoNoExiste() {
        when(compraRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> compraService.buscarPorId(1L)
        );
    }

    @Test
    void listarDetallesDebeValidarCompraYRetornarDetalles() {
        Compra compra = crearCompraRegistrada();
        List<DetalleCompra> detalles = List.of(new DetalleCompra());

        when(compraRepository.findById(1L))
                .thenReturn(Optional.of(compra));

        when(detalleCompraRepository.findByCompraId(1L))
                .thenReturn(detalles);

        assertEquals(detalles, compraService.listarDetalles(1L));
    }

    @Test
    void listarPorProveedorDebeRetornarComprasDelProveedor() {
        List<Compra> compras = List.of(new Compra());

        when(compraRepository.findByProveedorId(1L))
                .thenReturn(compras);

        assertEquals(compras, compraService.listarPorProveedor(1L));
    }

    // ---------------------------------------------------------
    // MÉTODOS AUXILIARES
    // ---------------------------------------------------------

    private Proveedor crearProveedorActivo() {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setNombre("Proveedor de prueba");
        proveedor.setActivo(true);
        return proveedor;
    }

    private Producto crearProductoActivo() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Producto de prueba");

        // El estado del producto es booleano.
        producto.setEstado(true);

        producto.setStockActual(10);
        return producto;
    }

    private DetalleCompra crearDetalle(
            Producto producto,
            Integer cantidad,
            String precioUnitario
    ) {
        DetalleCompra detalle = new DetalleCompra();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(new BigDecimal(precioUnitario));
        return detalle;
    }

    private Compra crearCompraRegistrada() {
        Compra compra = new Compra();
        compra.setId(1L);
        compra.setEstado("REGISTRADA");
        compra.setTotal(new BigDecimal("30.00"));
        return compra;
    }
}