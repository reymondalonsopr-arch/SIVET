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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio encargado de gestionar la lógica de negocio
 * relacionada con las ventas de SIVET.
 */
@Service
@Transactional
public class VentaService {

    // Repositorios necesarios para gestionar las ventas.
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;

    // Servicio encargado de actualizar el inventario.
    private final MovimientoInventarioService movimientoService;

    // Servicio encargado de recuperar mensajes personalizados.
    private final MensajeService mensajeService;

    // Constructor para inyectar las dependencias.
    public VentaService(
            VentaRepository ventaRepository,
            DetalleVentaRepository detalleVentaRepository,
            ProductoRepository productoRepository,
            ClienteRepository clienteRepository,
            MovimientoInventarioService movimientoService,
            MensajeService mensajeService) {

        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.movimientoService = movimientoService;
        this.mensajeService = mensajeService;
    }

    /**
     * Registra una venta, calcula sus importes,
     * guarda sus detalles y genera las salidas del inventario.
     */
    public Venta registrar(Venta venta, List<DetalleVenta> detalles) {

        // Comprueba que la venta contenga al menos un producto.
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("venta.sin.detalles")
            );
        }

        // Recupera el cliente si se seleccionó uno.
        Cliente cliente = null;

        if (venta.getCliente() != null
                && venta.getCliente().getId() != null) {

            cliente = clienteRepository
                    .findById(venta.getCliente().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            mensajeService.obtenerMensaje("venta.cliente.no.encontrado")
                    ));

            // No permite ventas a clientes inactivos.
            if (!cliente.isActivo()) {
                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje("venta.cliente.inactivo")
                );
            }
        }

        // Prepara el total y los detalles validados.
        BigDecimal total = BigDecimal.ZERO;
        List<DetalleVenta> detallesValidados = new ArrayList<>();

        // Valida los productos y calcula sus importes.
        for (DetalleVenta detalle : detalles) {

            // Comprueba que se haya seleccionado un producto.
            if (detalle.getProducto() == null
                    || detalle.getProducto().getId() == null) {

                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje("venta.producto.obligatorio")
                );
            }

            // Recupera el producto desde PostgreSQL.
            Producto producto = productoRepository
                    .findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            mensajeService.obtenerMensaje("venta.producto.no.encontrado")
                    ));

            // Comprueba que el producto esté activo.
            if (!producto.isEstado()) {
                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje(
                                "venta.producto.inactivo",
                                producto.getNombre()
                        )
                );
            }

            // Comprueba que la cantidad sea válida.
            if (detalle.getCantidad() == null
                    || detalle.getCantidad() <= 0) {

                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje("venta.cantidad.invalida")
                );
            }

            // Comprueba que exista stock suficiente.
            if (producto.getStockActual() < detalle.getCantidad()) {
                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje(
                                "venta.stock.insuficiente",
                                producto.getNombre(),
                                producto.getStockActual()
                        )
                );
            }

            // Obtiene el precio de venta registrado en el producto.
            BigDecimal precioVenta = producto.getPrecioVenta();

            if (precioVenta == null
                    || precioVenta.compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje(
                                "venta.precio.invalido",
                                producto.getNombre()
                        )
                );
            }

            // Calcula el subtotal del detalle.
            BigDecimal subtotal = precioVenta.multiply(
                    BigDecimal.valueOf(detalle.getCantidad())
            );

            // Asocia el producto recuperado desde la base de datos.
            detalle.setProducto(producto);

            // Guarda el precio real del producto.
            detalle.setPrecioUnitario(precioVenta);

            // Guarda el subtotal calculado.
            detalle.setSubtotal(subtotal);

            // Acumula el importe en el total de la venta.
            total = total.add(subtotal);

            detallesValidados.add(detalle);
        }

        // Completa los datos generales de la venta.
        venta.setCliente(cliente);
        venta.setFecha(LocalDateTime.now());
        venta.setTotal(total);
        venta.setEstado("REGISTRADA");

        // Guarda la cabecera de la venta.
        Venta ventaGuardada = ventaRepository.save(venta);

        // Guarda los detalles y registra las salidas del inventario.
        for (DetalleVenta detalle : detallesValidados) {

            // Asocia el detalle con la venta guardada.
            detalle.setVenta(ventaGuardada);

            // Guarda el detalle de venta.
            detalleVentaRepository.save(detalle);

            // Registra la salida y descuenta el stock automáticamente.
            movimientoService.registrarSalida(
                    detalle.getProducto().getId(),
                    detalle.getCantidad(),
                    "Salida por venta VEN-" + ventaGuardada.getId()
            );
        }

        return ventaGuardada;
    }

    /**
     * Anula una venta registrada y devuelve sus productos al inventario.
     * La venta y sus detalles se conservan como historial.
     */
    public Venta anular(Long id) {

        // Recupera la venta que se desea anular.
        Venta venta = buscarPorId(id);

        // Evita anular una venta que ya fue anulada.
        if ("ANULADA".equalsIgnoreCase(venta.getEstado())) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("venta.ya.anulada", id)
            );
        }

        // Solo permite anular ventas registradas.
        if (!"REGISTRADA".equalsIgnoreCase(venta.getEstado())) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("venta.estado.no.anulable", id)
            );
        }

        // Recupera los detalles de la venta.
        List<DetalleVenta> detalles = detalleVentaRepository.findByVentaId(id);

        // Verifica que la venta tenga detalles asociados.
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("venta.sin.detalles")
            );
        }

        // Devuelve al inventario las cantidades vendidas.
        for (DetalleVenta detalle : detalles) {

            Producto producto = detalle.getProducto();

            movimientoService.registrarEntradaPorAnulacionVenta(
                    producto,
                    detalle.getCantidad(),
                    venta.getId()
            );
        }

        // Actualiza el estado sin eliminar la venta.
        venta.setEstado("ANULADA");

        // Guarda la venta con su nuevo estado.
        return ventaRepository.save(venta);
    }

    // Obtiene todas las ventas ordenadas desde la más reciente.
    @Transactional(readOnly = true)
    public List<Venta> listarTodas() {
        return ventaRepository.findAllByOrderByFechaDesc();
    }

    // Busca una venta mediante su identificador.
    @Transactional(readOnly = true)
    public Venta buscarPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("venta.no.encontrada", id)
                ));
    }

    // Obtiene los detalles de una venta.
    @Transactional(readOnly = true)
    public List<DetalleVenta> listarDetalles(Long ventaId) {

        // Comprueba que la venta exista.
        buscarPorId(ventaId);

        return detalleVentaRepository.findByVentaId(ventaId);
    }

    // Busca las ventas realizadas a un cliente.
    @Transactional(readOnly = true)
    public List<Venta> listarPorCliente(Long clienteId) {
        return ventaRepository.findByClienteId(clienteId);
    }
}
