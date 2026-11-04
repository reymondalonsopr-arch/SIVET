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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio encargado de gestionar la lógica de negocio
 * relacionada con las compras de SIVET.
 */
@Service
@Transactional
public class CompraService {

    // Repositorios necesarios para gestionar las compras.
    private final CompraRepository compraRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    // Servicio encargado de registrar los movimientos del inventario.
    private final MovimientoInventarioService movimientoService;

    // Servicio encargado de recuperar mensajes personalizados.
    private final MensajeService mensajeService;

    // Constructor para inyectar las dependencias.
    public CompraService(
            CompraRepository compraRepository,
            DetalleCompraRepository detalleCompraRepository,
            ProductoRepository productoRepository,
            ProveedorRepository proveedorRepository,
            MovimientoInventarioRepository movimientoRepository,
            MovimientoInventarioService movimientoService,
            MensajeService mensajeService) {

        this.compraRepository = compraRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.movimientoRepository = movimientoRepository;
        this.movimientoService = movimientoService;
        this.mensajeService = mensajeService;
    }

    /**
     * Registra una compra, calcula sus importes,
     * guarda sus detalles y genera las entradas de inventario.
     */
    public Compra registrar(Compra compra, List<DetalleCompra> detalles) {

        // Comprueba que la compra tenga al menos un producto.
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("compra.sin.detalles")
            );
        }

        // Comprueba que se haya seleccionado un proveedor.
        if (compra.getProveedor() == null
                || compra.getProveedor().getId() == null) {

            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("compra.proveedor.obligatorio")
            );
        }

        // Recupera el proveedor desde la base de datos.
        Proveedor proveedor = proveedorRepository
                .findById(compra.getProveedor().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("compra.proveedor.no.encontrado")
                ));

        // No permite compras a proveedores inactivos.
        if (!proveedor.isActivo()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("compra.proveedor.inactivo")
            );
        }

        // Prepara el total y los detalles validados.
        BigDecimal total = BigDecimal.ZERO;
        List<DetalleCompra> detallesValidados = new ArrayList<>();

        // Valida todos los detalles antes de modificar el inventario.
        for (DetalleCompra detalle : detalles) {

            // Comprueba que se haya seleccionado un producto.
            if (detalle.getProducto() == null
                    || detalle.getProducto().getId() == null) {

                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje("compra.producto.obligatorio")
                );
            }

            // Recupera el producto desde PostgreSQL.
            Producto producto = productoRepository
                    .findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            mensajeService.obtenerMensaje("compra.producto.no.encontrado")
                    ));

            // Comprueba que el producto esté activo.
            if (!producto.isEstado()) {
                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje(
                                "compra.producto.inactivo",
                                producto.getNombre()
                        )
                );
            }

            // Valida la cantidad adquirida.
            if (detalle.getCantidad() == null
                    || detalle.getCantidad() <= 0) {

                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje("compra.cantidad.invalida")
                );
            }

            // Valida el precio unitario.
            if (detalle.getPrecioUnitario() == null
                    || detalle.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje("compra.precio.invalido")
                );
            }

            // Calcula el subtotal del producto.
            BigDecimal subtotal = detalle.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(detalle.getCantidad()));

            // Asocia las entidades recuperadas desde la base de datos.
            detalle.setProducto(producto);
            detalle.setCompra(compra);
            detalle.setSubtotal(subtotal);

            // Acumula el subtotal en el total general.
            total = total.add(subtotal);

            detallesValidados.add(detalle);
        }

        // Completa la información general de la compra.
        compra.setProveedor(proveedor);
        compra.setFecha(LocalDateTime.now());
        compra.setTotal(total);
        compra.setEstado("REGISTRADA");

        // Guarda la cabecera de la compra.
        Compra compraGuardada = compraRepository.save(compra);

        // Guarda los detalles y registra las entradas del inventario.
        for (DetalleCompra detalle : detallesValidados) {

            // Asocia el detalle con la compra guardada.
            detalle.setCompra(compraGuardada);

            // Guarda el detalle de compra.
            detalleCompraRepository.save(detalle);

            // Registra la entrada y actualiza el stock automáticamente.
            movimientoService.registrarEntradaPorCompra(
                    detalle.getProducto(),
                    detalle.getCantidad(),
                    compraGuardada
            );
        }

        return compraGuardada;
    }

    /**
     * Anula una compra registrada y revierte sus entradas de inventario
     * mediante movimientos compensatorios, conservando el historial.
     */
    public Compra anular(Long compraId) {

        // Recupera la compra que se desea anular.
        Compra compra = buscarPorId(compraId);

        // Evita anular una compra que ya fue anulada.
        if ("ANULADA".equalsIgnoreCase(compra.getEstado())) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("compra.ya.anulada")
            );
        }

        // Solo permite anular compras registradas.
        if (!"REGISTRADA".equalsIgnoreCase(compra.getEstado())) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("compra.estado.no.anulable")
            );
        }

        // Recupera los movimientos asociados a la compra.
        List<MovimientoInventario> movimientos =
                movimientoRepository.findByCompraId(compraId);

        // Filtra las entradas originales de la compra.
        List<MovimientoInventario> entradas = movimientos.stream()
                .filter(movimiento ->
                        "ENTRADA".equalsIgnoreCase(movimiento.getTipo()))
                .toList();

        // Comprueba que existan entradas que revertir.
        if (entradas.isEmpty()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("compra.sin.movimientos")
            );
        }

        // Verifica primero que todos los productos tengan stock suficiente.
        for (MovimientoInventario entrada : entradas) {

            Producto producto = entrada.getProducto();

            if (producto.getStockActual() < entrada.getCantidad()) {
                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje(
                                "compra.stock.insuficiente.anulacion",
                                producto.getNombre(),
                                producto.getStockActual(),
                                entrada.getCantidad()
                        )
                );
            }
        }

        // Registra una salida compensatoria por cada entrada original.
        for (MovimientoInventario entrada : entradas) {

            movimientoService.registrarSalidaPorAnulacionCompra(
                    entrada.getProducto(),
                    entrada.getCantidad(),
                    compra
            );
        }

        // Marca la compra como anulada y conserva sus datos.
        compra.setEstado("ANULADA");

        return compraRepository.save(compra);
    }

    // Obtiene todas las compras ordenadas desde la más reciente.
    @Transactional(readOnly = true)
    public List<Compra> listarTodas() {
        return compraRepository.findAllByOrderByFechaDesc();
    }

    // Busca una compra mediante su identificador.
    @Transactional(readOnly = true)
    public Compra buscarPorId(Long id) {
        return compraRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("compra.no.encontrada", id)
                ));
    }

    // Obtiene los detalles de una compra.
    @Transactional(readOnly = true)
    public List<DetalleCompra> listarDetalles(Long compraId) {

        // Comprueba que la compra exista.
        buscarPorId(compraId);

        return detalleCompraRepository.findByCompraId(compraId);
    }

    // Busca las compras realizadas a un proveedor.
    @Transactional(readOnly = true)
    public List<Compra> listarPorProveedor(Long proveedorId) {
        return compraRepository.findByProveedorId(proveedorId);
    }
}