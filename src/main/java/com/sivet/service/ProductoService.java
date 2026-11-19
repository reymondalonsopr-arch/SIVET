
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

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

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

    // =========================================================
    // REGISTRO DE PRODUCTOS
    // =========================================================

    /**
     * Registra un producto y genera automáticamente su código.
     */
    public Producto registrar(Producto producto) {

        // Limpiamos los espacios del nombre.
        producto.setNombre(producto.getNombre().trim());

        // Comprobamos que se haya seleccionado una categoría.
        if (producto.getCategoria() == null
                || producto.getCategoria().getId() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una categoría."
            );
        }

        // Buscamos la categoría seleccionada.
        Categoria categoria = buscarCategoriaActiva(
                producto.getCategoria().getId()
        );

        // Asignamos la categoría al producto.
        producto.setCategoria(categoria);

        // Generamos el código automáticamente en el servidor.
        // No confiamos en el código enviado desde el formulario.
        String codigo = generarCodigo(categoria);

        producto.setCodigo(codigo);

        // El stock inicial comienza en cero.
        producto.setStockActual(0);

        // El producto se registra activo.
        producto.setEstado(true);

        // Guardamos el producto en PostgreSQL.
        return productoRepository.save(producto);
    }

    // =========================================================
    // GENERACIÓN AUTOMÁTICA DE CÓDIGOS
    // =========================================================

    /**
     * Genera una vista previa del siguiente código disponible.
     *
     * Ejemplos:
     * Bebidas -> B001
     * Abarrotes -> A001
     * Belleza -> BE001, si existe otra categoría que comienza con B.
     */
    @Transactional(readOnly = true)
    public String previsualizarCodigo(Long categoriaId) {

        // Recuperamos la categoría seleccionada.
        Categoria categoria = buscarCategoriaActiva(categoriaId);

        // Generamos el siguiente código disponible.
        return generarCodigo(categoria);
    }

    /**
     * Genera el código definitivo de un producto.
     */
    private String generarCodigo(Categoria categoria) {

        // Obtenemos el prefijo único de la categoría.
        String prefijo = generarPrefijoCategoria(categoria);

        // Consultamos los productos que utilizan ese prefijo.
        List<Producto> productos = productoRepository
                .findByCodigoStartingWithIgnoreCase(prefijo);

        // Buscamos el número correlativo más alto.
        int numeroMayor = 0;

        for (Producto producto : productos) {

            String codigoExistente = producto.getCodigo();

            // Extraemos la parte numérica del código.
            String parteNumerica = codigoExistente
                    .substring(prefijo.length());

            // Solo consideramos códigos con sufijo numérico.
            if (parteNumerica.matches("\\d+")) {

                int numero = Integer.parseInt(parteNumerica);

                if (numero > numeroMayor) {
                    numeroMayor = numero;
                }
            }
        }

        // Calculamos el siguiente número.
        int siguienteNumero = numeroMayor + 1;

        // Construimos el código con tres dígitos como mínimo.
        String codigo;

        do {

            codigo = prefijo + String.format(
                    Locale.ROOT,
                    "%03d",
                    siguienteNumero
            );

            // Comprobamos que el código no esté registrado.
            if (productoRepository.existsByCodigo(codigo)) {
                siguienteNumero++;
            } else {
                break;
            }

        } while (true);

        return codigo;
    }

    /**
     * Genera un prefijo que distingue una categoría de las demás.
     *
     * Utiliza una letra cuando es suficiente.
     * Utiliza dos o más letras cuando existen coincidencias.
     */
    private String generarPrefijoCategoria(Categoria categoria) {

        // Normalizamos el nombre de la categoría.
        String nombre = normalizarNombre(categoria.getNombre());

        if (nombre.isEmpty()) {
            throw new IllegalArgumentException(
                    "El nombre de la categoría no permite generar un código."
            );
        }

        // Recuperamos todas las categorías registradas.
        List<Categoria> categorias = categoriaRepository.findAll();

        // Comenzamos utilizando una sola letra.
        int longitud = 1;

        // Buscamos el prefijo mínimo que no coincida con otra categoría.
        while (longitud <= nombre.length()) {

            String prefijo = nombre.substring(
                    0,
                    longitud
            );

            boolean existeConflicto = false;

            for (Categoria otraCategoria : categorias) {

                // Excluimos la categoría seleccionada.
                if (otraCategoria.getId().equals(categoria.getId())) {
                    continue;
                }

                // Normalizamos el nombre de la otra categoría.
                String otroNombre = normalizarNombre(
                        otraCategoria.getNombre()
                );

                // Comprobamos si ambos nombres comparten el prefijo.
                if (otroNombre.startsWith(prefijo)
                        || prefijo.startsWith(otroNombre)) {

                    existeConflicto = true;
                    break;
                }
            }

            // Si no hay coincidencias, utilizamos el prefijo.
            if (!existeConflicto) {
                return prefijo;
            }

            // Si hay coincidencias, ampliamos el prefijo.
            longitud++;
        }

        // Caso excepcional: una categoría es prefijo completo de otra.
        // Utilizamos su identificador para distinguirla.
        return nombre + categoria.getId();
    }

    /**
     * Elimina tildes, espacios y caracteres especiales.
     * Convierte el nombre a mayúsculas.
     */
    private String normalizarNombre(String nombre) {

        return Normalizer.normalize(
                        nombre.trim(),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toUpperCase(Locale.ROOT);
    }

    // =========================================================
    // CONSULTAS DE PRODUCTOS
    // =========================================================

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
                        mensajeService.obtenerMensaje(
                                "producto.no.encontrado.id",
                                id
                        )
                ));
    }

    // Busca un producto por su código exacto.
    @Transactional(readOnly = true)
    public Producto buscarPorCodigo(String codigo) {

        return productoRepository.findByCodigo(codigo.trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje(
                                "producto.no.encontrado.codigo",
                                codigo
                        )
                ));
    }

    /**
     * Busca productos por una parte del código.
     * No distingue entre mayúsculas y minúsculas.
     *
     * Ejemplo:
     * B -> B001, B002, BE001.
     */
    @Transactional(readOnly = true)
    public List<Producto> buscarPorCodigoParcial(String codigo) {

        if (codigo == null || codigo.isBlank()) {
            return listarTodos();
        }

        return productoRepository.findByCodigoContainingIgnoreCase(
                codigo.trim()
        );
    }

    // Busca productos por su nombre.
    @Transactional(readOnly = true)
    public List<Producto> buscarPorNombre(String nombre) {

        return productoRepository.findByNombreContainingIgnoreCase(
                nombre.trim()
        );
    }

    // =========================================================
    // ACTUALIZACIÓN DE PRODUCTOS
    // =========================================================

    /**
     * Actualiza los datos comerciales de un producto.
     * Conserva su código y sus existencias.
     */
    public Producto actualizar(Long id, Producto datos) {

        // Buscamos el producto existente.
        Producto producto = buscarPorId(id);

        // Limpiamos los datos recibidos.
        String nombre = datos.getNombre().trim();

        // Comprobamos que se haya seleccionado una categoría.
        if (datos.getCategoria() == null
                || datos.getCategoria().getId() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una categoría."
            );
        }

        // Comprobamos que la categoría seleccionada esté activa.
        Categoria categoria = buscarCategoriaActiva(
                datos.getCategoria().getId()
        );

        // Actualizamos los datos comerciales.
        producto.setNombre(nombre);
        producto.setPrecioCompra(datos.getPrecioCompra());
        producto.setPrecioVenta(datos.getPrecioVenta());
        producto.setStockMinimo(datos.getStockMinimo());
        producto.setCategoria(categoria);

        // Conservamos el código original.
        // Conservamos el stock actual.
        // Conservamos el estado existente.

        return productoRepository.save(producto);
    }

    // =========================================================
    // CONTROL DE EXISTENCIAS
    // =========================================================

    // Obtiene los productos cuyo stock alcanzó el mínimo o está por debajo.
    @Transactional(readOnly = true)
    public List<Producto> listarStockBajo() {

        return productoRepository.buscarProductosConStockBajo();
    }

    // =========================================================
    // CAMBIO DE ESTADO
    // =========================================================

    // Activa o desactiva un producto.
    public Producto cambiarEstado(Long id, boolean estado) {

        // Buscamos el producto.
        Producto producto = buscarPorId(id);

        // Actualizamos su estado.
        producto.setEstado(estado);

        // Guardamos los cambios.
        return productoRepository.save(producto);
    }

    // =========================================================
    // ELIMINACIÓN SEGURA
    // =========================================================

    /**
     * Elimina físicamente un producto solamente cuando es seguro hacerlo.
     */
    public void eliminar(Long id) {

        // Buscamos el producto que se desea eliminar.
        Producto producto = buscarPorId(id);

        // No permitimos eliminar productos que todavía tienen existencias.
        if (producto.getStockActual() != null
                && producto.getStockActual() > 0) {

            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje(
                            "producto.no.eliminable.stock"
                    )
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
                    mensajeService.obtenerMensaje(
                            "producto.no.eliminable.historial"
                    )
            );
        }

        // Si no tiene stock ni historial, se puede eliminar definitivamente.
        productoRepository.delete(producto);
    }

    // =========================================================
    // VALIDACIÓN DE CATEGORÍAS
    // =========================================================

    /**
     * Busca una categoría y comprueba que esté activa.
     */
    private Categoria buscarCategoriaActiva(Long categoriaId) {

        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje(
                                "producto.categoria.no.encontrada"
                        )
                ));

        if (!categoria.isActivo()) {

            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje(
                            "producto.categoria.inactiva"
                    )
            );
        }

        return categoria;
    }
}