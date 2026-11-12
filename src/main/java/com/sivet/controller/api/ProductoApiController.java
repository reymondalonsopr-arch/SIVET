
package com.sivet.controller.api;

import com.sivet.model.Categoria;
import com.sivet.model.Producto;
import com.sivet.service.ProductoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador REST encargado de gestionar los productos de SIVET.
 *
 * Permite listar, consultar, registrar y actualizar productos.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoApiController {

    // Servicio que contiene la lógica de negocio de productos.
    private final ProductoService productoService;

    // Inyección de dependencias mediante constructor.
    public ProductoApiController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * GET /api/productos
     * Lista todos los productos.
     */
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listarProductos() {

        List<ProductoResponse> productos = productoService.listarTodos()
                .stream()
                .map(this::convertirAResponse)
                .toList();

        return ResponseEntity.ok(productos);
    }

    /**
     * GET /api/productos/{id}
     * Consulta un producto por su identificador.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> buscarProducto(
            @PathVariable Long id) {

        Producto producto = productoService.buscarPorId(id);

        return ResponseEntity.ok(convertirAResponse(producto));
    }

    /**
     * POST /api/productos
     * Registra un producto nuevo.
     */
    @PostMapping
    public ResponseEntity<ProductoResponse> registrarProducto(
            @Valid @RequestBody ProductoCrearRequest request) {

        // Construimos el producto con los datos recibidos.
        Producto producto = new Producto();

        producto.setNombre(request.nombre());
        producto.setPrecioCompra(request.precioCompra());
        producto.setPrecioVenta(request.precioVenta());
        producto.setStockMinimo(request.stockMinimo());

        // Asignamos la categoría seleccionada.
        Categoria categoria = new Categoria();
        categoria.setId(request.categoriaId());

        producto.setCategoria(categoria);

        // El servicio genera el código y establece stock y estado.
        Producto productoRegistrado = productoService.registrar(producto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(convertirAResponse(productoRegistrado));
    }

    /**
     * PUT /api/productos/{id}
     * Actualiza los datos comerciales de un producto.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoActualizarRequest request) {

        // Construimos un objeto con los nuevos datos comerciales.
        Producto datos = new Producto();

        datos.setNombre(request.nombre());
        datos.setPrecioCompra(request.precioCompra());
        datos.setPrecioVenta(request.precioVenta());
        datos.setStockMinimo(request.stockMinimo());

        // Asignamos la nueva categoría.
        Categoria categoria = new Categoria();
        categoria.setId(request.categoriaId());

        datos.setCategoria(categoria);

        // El servicio conserva código, stock actual y estado.
        Producto productoActualizado = productoService.actualizar(id, datos);

        return ResponseEntity.ok(convertirAResponse(productoActualizado));
    }

    /**
     * Convierte la entidad Producto en un DTO de respuesta.
     */
    private ProductoResponse convertirAResponse(Producto producto) {

        return new ProductoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getPrecioCompra(),
                producto.getPrecioVenta(),
                producto.getStockActual(),
                producto.getStockMinimo(),
                producto.isEstado(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre()
        );
    }

    /**
     * Datos necesarios para registrar un producto.
     */
    public record ProductoCrearRequest(

            @NotBlank(message = "El nombre del producto es obligatorio.")
            @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
            String nombre,

            @NotNull(message = "El precio de compra es obligatorio.")
            @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo.")
            BigDecimal precioCompra,

            @NotNull(message = "El precio de venta es obligatorio.")
            @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo.")
            BigDecimal precioVenta,

            @NotNull(message = "El stock mínimo es obligatorio.")
            @Min(value = 0, message = "El stock mínimo no puede ser negativo.")
            Integer stockMinimo,

            @NotNull(message = "Debe seleccionar una categoría.")
            @Positive(message = "La categoría debe ser válida.")
            Long categoriaId

    ) {}

    /**
     * Datos necesarios para actualizar un producto.
     */
    public record ProductoActualizarRequest(

            @NotBlank(message = "El nombre del producto es obligatorio.")
            @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
            String nombre,

            @NotNull(message = "El precio de compra es obligatorio.")
            @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo.")
            BigDecimal precioCompra,

            @NotNull(message = "El precio de venta es obligatorio.")
            @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo.")
            BigDecimal precioVenta,

            @NotNull(message = "El stock mínimo es obligatorio.")
            @Min(value = 0, message = "El stock mínimo no puede ser negativo.")
            Integer stockMinimo,

            @NotNull(message = "Debe seleccionar una categoría.")
            @Positive(message = "La categoría debe ser válida.")
            Long categoriaId

    ) {}

    /**
     * Datos que devuelve la API REST.
     */
    public record ProductoResponse(
            Long id,
            String codigo,
            String nombre,
            BigDecimal precioCompra,
            BigDecimal precioVenta,
            Integer stockActual,
            Integer stockMinimo,
            boolean estado,
            Long categoriaId,
            String categoriaNombre
    ) {}
}
