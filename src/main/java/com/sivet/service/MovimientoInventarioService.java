package com.sivet.service;

import com.sivet.model.Compra;
import com.sivet.model.MovimientoInventario;
import com.sivet.model.Producto;

import com.sivet.repository.MovimientoInventarioRepository;
import com.sivet.repository.ProductoRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio encargado de controlar las entradas y salidas de productos
 * y mantener actualizado el inventario.
 */
@Service
@Transactional
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final MensajeService mensajeService;

    public MovimientoInventarioService(
            MovimientoInventarioRepository movimientoRepository,
            ProductoRepository productoRepository,
            MensajeService mensajeService) {

        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.mensajeService = mensajeService;
    }

    // Registra una entrada manual al inventario.
    public MovimientoInventario registrarEntrada(
            Long productoId,
            Integer cantidad,
            String motivo) {

        return registrarMovimiento(
                productoId, cantidad, "ENTRADA", motivo, null, false
        );
    }

    // Registra una salida manual del inventario.
    public MovimientoInventario registrarSalida(
            Long productoId,
            Integer cantidad,
            String motivo) {

        return registrarMovimiento(
                productoId, cantidad, "SALIDA", motivo, null, false
        );
    }

    // Registra una entrada originada por una compra.
    public MovimientoInventario registrarEntradaPorCompra(
            Producto producto,
            Integer cantidad,
            Compra compra) {

        return registrarMovimiento(
                producto.getId(),
                cantidad,
                "ENTRADA",
                "Entrada por compra COM-" + compra.getId(),
                compra,
                false
        );
    }

    // Registra una salida compensatoria por anulación de compra.
    public MovimientoInventario registrarSalidaPorAnulacionCompra(
            Producto producto,
            Integer cantidad,
            Compra compra) {

        return registrarMovimiento(
                producto.getId(),
                cantidad,
                "SALIDA",
                "Anulación de compra COM-" + compra.getId(),
                compra,
                false
        );
    }

    // Registra una entrada compensatoria por anulación de venta.
    // Se permite aunque el producto esté inactivo, porque se está
    // revirtiendo una operación previamente registrada.
    public MovimientoInventario registrarEntradaPorAnulacionVenta(
            Producto producto,
            Integer cantidad,
            Long ventaId) {

        return registrarMovimiento(
                producto.getId(),
                cantidad,
                "ENTRADA",
                "Anulación de venta VEN-" + ventaId,
                null,
                true
        );
    }

    /**
     * Método centralizado para registrar movimientos.
     * Actualiza el stock y guarda el historial en una transacción.
     */
    private MovimientoInventario registrarMovimiento(
            Long productoId,
            Integer cantidad,
            String tipo,
            String motivo,
            Compra compra,
            boolean permitirProductoInactivo) {

        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("movimiento.cantidad.invalida")
            );
        }

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("movimiento.motivo.obligatorio")
            );
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("movimiento.producto.no.encontrado")
                ));

        // Las operaciones normales requieren un producto activo.
        // Las anulaciones compensatorias pueden afectar productos inactivos.
        if (!permitirProductoInactivo && !producto.isEstado()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("movimiento.producto.inactivo")
            );
        }

        if ("ENTRADA".equals(tipo)) {

            producto.setStockActual(
                    producto.getStockActual() + cantidad
            );

        } else if ("SALIDA".equals(tipo)) {

            if (producto.getStockActual() < cantidad) {
                throw new IllegalArgumentException(
                        mensajeService.obtenerMensaje("movimiento.stock.insuficiente")
                );
            }

            producto.setStockActual(
                    producto.getStockActual() - cantidad
            );

        } else {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("movimiento.tipo.invalido")
            );
        }

        productoRepository.save(producto);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setCompra(compra);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setMotivo(motivo.trim());

        return movimientoRepository.save(movimiento);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventario> listarTodos() {
        return movimientoRepository.findAllByOrderByFechaDesc();
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventario> listarPorProducto(Long productoId) {
        return movimientoRepository.findByProductoId(productoId);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventario> listarEntradas() {
        return movimientoRepository.findByTipo("ENTRADA");
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventario> listarSalidas() {
        return movimientoRepository.findByTipo("SALIDA");
    }
}