
package com.sivet.service;

import com.sivet.model.Categoria;
import com.sivet.model.Producto;

import com.sivet.repository.CategoriaRepository;
import com.sivet.repository.ProductoRepository;
import com.sivet.repository.DetalleCompraRepository;
import com.sivet.repository.DetalleVentaRepository;
import com.sivet.repository.MovimientoInventarioRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de gestionar la lógica de negocio
 * relacionada con los productos de SIVET.
 */
@Service
@Transactional
public class ProductoService {

    // Repositorio para acceder a los productos.
    private final ProductoRepository productoRepository;

    // Repositorio para consultar las categorías.
    private final CategoriaRepository categoriaRepository;

    // Repositorio para consultar los detalles de compras.
    private final DetalleCompraRepository detalleCompraRepository;

    // Repositorio para consultar los detalles de ventas.
    private final DetalleVentaRepository detalleVentaRepository;

    // Repositorio para consultar los movimientos de inventario.
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    // Servicio encargado de recuperar mensajes personalizados.
    private final MensajeService mensajeService;

    // Inyección de dependencias mediante constructor.
    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            DetalleCompraRepository detalleCompraRepository,
            DetalleVentaRepository detalleVentaRepository,
            MovimientoInventarioRepository movimientoInventarioRepository,
            MensajeService mensajeService) {

        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.mensajeService = mensajeService;
    }

    // Registra un nuevo producto.
    public Producto registrar(Producto producto) {

        // Limpiamos los espacios del código y del nombre.
        String codigo = producto.getCodigo().trim();
        producto.setCodigo(codigo);
        producto.setNombre(producto.getNombre().trim());

        // Evitamos registrar códigos duplicados.
        if (productoRepository.existsByCodigo(codigo)) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("producto.codigo.duplicado", codigo)
            );
        }

        // Buscamos la categoría seleccionada.
        Categoria categoria = buscarCategoriaActiva(
                producto.getCategoria().getId()
        );

        // Asignamos la categoría al producto.
        producto.setCategoria(categoria);

        // El stock inicial comienza en cero.
        producto.setStockActual(0);

        // El producto se registra activo.
        producto.setEstado(true);

        // Guardamos el producto en PostgreSQL.
        return productoRepository.save(producto);
    }

    // Obtiene todos los productos registrados.
    @Transactional(readOnly = true)
    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    // Obtiene solamente los productos activos.
    @Transactional(readOnly = true)
    public List<Producto> listarActivos() {
        return productoRepository.findByEstadoTrue();
    }

    // Busca un producto por su identificador.
    @Transactional(readOnly = true)
    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("producto.no.encontrado.id", id)
                ));
    }

    // Busca un producto por su código.
    @Transactional(readOnly = true)
    public Producto buscarPorCodigo(String codigo) {
        return productoRepository.findByCodigo(codigo.trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("producto.no.encontrado.codigo", codigo)
                ));
    }

    // Busca productos por su nombre.
    @Transactional(readOnly = true)
    public List<Producto> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCase(nombre);
    }

    // Actualiza los datos comerciales de un producto.
    public Producto actualizar(Long id, Producto datos) {

        // Buscamos el producto existente.
        Producto producto = buscarPorId(id);

        // Limpiamos los datos recibidos.
        String codigo = datos.getCodigo().trim();
        String nombre = datos.getNombre().trim();

        // Comprobamos que el código no pertenezca a otro producto.
        Producto existente = productoRepository.findByCodigo(codigo)
                .orElse(null);

        if (existente != null && !existente.getId().equals(id)) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("producto.codigo.duplicado.actualizacion")
            );
        }

        // Comprobamos que la categoría seleccionada esté activa.
        Categoria categoria = buscarCategoriaActiva(
                datos.getCategoria().getId()
        );

        // Actualizamos los datos comerciales.
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setPrecioCompra(datos.getPrecioCompra());
        producto.setPrecioVenta(datos.getPrecioVenta());
        producto.setStockMinimo(datos.getStockMinimo());
        producto.setCategoria(categoria);

        // Conservamos el stock actual y el estado existentes.
        // Las existencias se modificarán desde compras y ventas.

        return productoRepository.save(producto);
    }

    // Obtiene los productos cuyo stock alcanzó el mínimo o está por debajo.
    @Transactional(readOnly = true)
    public List<Producto> listarStockBajo() {
        return productoRepository.buscarProductosConStockBajo();
    }

    // Activa o desactiva un producto.
    public Producto cambiarEstado(Long id, boolean estado) {

        // Buscamos el producto.
        Producto producto = buscarPorId(id);

        // Actualizamos su estado.
        producto.setEstado(estado);

        // Guardamos los cambios.
        return productoRepository.save(producto);
    }

    // Elimina físicamente un producto solamente cuando es seguro hacerlo.
    public void eliminar(Long id) {

        // Buscamos el producto que se desea eliminar.
        Producto producto = buscarPorId(id);

        // No permitimos eliminar productos que todavía tienen existencias.
        if (producto.getStockActual() != null && producto.getStockActual() > 0) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("producto.no.eliminable.stock")
            );
        }

        // Comprobamos si el producto aparece en alguna compra.
        boolean tieneCompras = !detalleCompraRepository
                .findByProductoId(id).isEmpty();

        // Comprobamos si el producto aparece en alguna venta.
        boolean tieneVentas = !detalleVentaRepository
                .findByProductoId(id).isEmpty();

        // Comprobamos si el producto tiene movimientos de inventario.
        boolean tieneMovimientos = !movimientoInventarioRepository
                .findByProductoId(id).isEmpty();

        // Protegemos el historial del producto.
        if (tieneCompras || tieneVentas || tieneMovimientos) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("producto.no.eliminable.historial")
            );
        }

        // Si no tiene stock ni historial, se puede eliminar definitivamente.
        productoRepository.delete(producto);
    }

    // Busca una categoría y comprueba que esté activa.
    private Categoria buscarCategoriaActiva(Long categoriaId) {

        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje("producto.categoria.no.encontrada")
                ));

        if (!categoria.isActivo()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("producto.categoria.inactiva")
            );
        }

        return categoria;
    }
}
