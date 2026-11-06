
package com.sivet.controller.web;

import com.sivet.model.Producto;
import com.sivet.service.CategoriaService;
import com.sivet.service.MensajeService;
import com.sivet.service.ProductoService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador encargado de gestionar las operaciones web
 * relacionadas con los productos de SIVET.
 *
 * Funcionalidades:
 * - Listar productos.
 * - Buscar productos por código.
 * - Mostrar vista previa del código automático.
 * - Registrar productos.
 * - Editar productos.
 * - Activar y desactivar productos.
 * - Eliminar productos.
 * - Consultar productos con stock bajo.
 */
@Controller
@RequestMapping("/productos")
public class ProductoController {

    // Servicio encargado de la lógica de productos.
    private final ProductoService productoService;

    // Servicio encargado de consultar las categorías.
    private final CategoriaService categoriaService;

    // Servicio encargado de recuperar mensajes personalizados.
    private final MensajeService mensajeService;

    // =========================================================
    // CONSTRUCTOR E INYECCIÓN DE DEPENDENCIAS
    // =========================================================

    public ProductoController(
            ProductoService productoService,
            CategoriaService categoriaService,
            MensajeService mensajeService) {

        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.mensajeService = mensajeService;
    }

    // =========================================================
    // LISTADO Y BÚSQUEDA DE PRODUCTOS
    // =========================================================

    /**
     * Muestra todos los productos o filtra por código.
     */
    @GetMapping
    public String listar(
            @RequestParam(required = false) String codigo,
            Model model) {

        // Si se ingresó un código, realizamos la búsqueda.
        if (codigo != null && !codigo.isBlank()) {

            model.addAttribute(
                    "productos",
                    productoService.buscarPorCodigoParcial(codigo.trim())
            );

        } else {

            // Si no hay búsqueda, mostramos todos los productos.
            model.addAttribute(
                    "productos",
                    productoService.listarTodos()
            );
        }

        // Conservamos el texto ingresado en el buscador.
        model.addAttribute("busquedaCodigo", codigo);

        return "productos/index";
    }

    // =========================================================
    // VISTA PREVIA DEL CÓDIGO AUTOMÁTICO
    // =========================================================

    /**
     * Genera una vista previa del siguiente código disponible
     * según la categoría seleccionada.
     *
     * Esta operación no registra ni reserva el código.
     *
     * URL:
     * /productos/codigo-preview?categoriaId=1
     */
    @GetMapping("/codigo-preview")
    @ResponseBody
    public ResponseEntity<String> previsualizarCodigo(
            @RequestParam Long categoriaId) {

        try {

            // Validamos que se haya recibido una categoría.
            if (categoriaId == null || categoriaId <= 0) {

                return ResponseEntity.badRequest()
                        .body("Debe seleccionar una categoría válida.");
            }

            // Solicitamos al servicio la vista previa del código.
            String codigo = productoService.previsualizarCodigo(categoriaId);

            // Devolvemos el código como texto al navegador.
            return ResponseEntity.ok(codigo);

        } catch (EntityNotFoundException | IllegalArgumentException e) {

            // La categoría no existe o no está disponible.
            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            // Evitamos exponer detalles internos al navegador.
            return ResponseEntity.internalServerError()
                    .body("No fue posible generar la vista previa del código.");
        }
    }

    // =========================================================
    // REGISTRO DE PRODUCTOS
    // =========================================================

    /**
     * Muestra el formulario para registrar un producto.
     */
    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        // Enviamos un producto vacío al formulario.
        model.addAttribute("producto", new Producto());

        // Cargamos las categorías activas.
        model.addAttribute(
                "categorias",
                categoriaService.listarActivas()
        );

        return "productos/formulario";
    }

    /**
     * Registra un nuevo producto.
     */
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("producto") Producto producto,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            // Registramos el producto mediante el servicio.
            // El código se genera automáticamente en el backend.
            productoService.registrar(producto);

            // Mostramos el mensaje de éxito.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("producto.registrado")
            );

            return "redirect:/productos";

        } catch (IllegalArgumentException
                 | EntityNotFoundException
                 | IllegalStateException e) {

            // Conservamos las categorías para volver al formulario.
            model.addAttribute(
                    "categorias",
                    categoriaService.listarActivas()
            );

            // Mostramos el error encontrado.
            model.addAttribute("mensajeError", e.getMessage());

            return "productos/formulario";
        }
    }

    // =========================================================
    // EDICIÓN DE PRODUCTOS
    // =========================================================

    /**
     * Muestra el formulario para editar un producto.
     */
    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            // Buscamos el producto seleccionado.
            model.addAttribute(
                    "producto",
                    productoService.buscarPorId(id)
            );

            // Cargamos las categorías activas.
            model.addAttribute(
                    "categorias",
                    categoriaService.listarActivas()
            );

            return "productos/formulario";

        } catch (EntityNotFoundException e) {

            // Informamos que el producto no existe.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    mensajeService.obtenerMensaje("producto.no.encontrado")
            );

            return "redirect:/productos";
        }
    }

    /**
     * Actualiza los datos comerciales de un producto.
     */
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id,
            @ModelAttribute("producto") Producto producto,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            // Actualizamos el producto mediante el servicio.
            // El código original se conserva.
            productoService.actualizar(id, producto);

            // Mostramos el mensaje de éxito.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("producto.actualizado")
            );

            return "redirect:/productos";

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Conservamos el identificador del producto.
            producto.setId(id);

            // Volvemos a cargar las categorías activas.
            model.addAttribute(
                    "categorias",
                    categoriaService.listarActivas()
            );

            // Mostramos el error.
            model.addAttribute("mensajeError", e.getMessage());

            return "productos/formulario";
        }
    }

    // =========================================================
    // CAMBIO DE ESTADO
    // =========================================================

    /**
     * Activa o desactiva un producto.
     */
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean estado,
            RedirectAttributes redirectAttributes) {

        try {

            // Cambiamos el estado del producto.
            productoService.cambiarEstado(id, estado);

            // Mostramos el mensaje de éxito.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("producto.actualizado")
            );

        } catch (EntityNotFoundException e) {

            // Informamos si no se encontró el producto.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    mensajeService.obtenerMensaje("producto.no.encontrado")
            );
        }

        return "redirect:/productos";
    }

    // =========================================================
    // ELIMINACIÓN DE PRODUCTOS
    // =========================================================

    /**
     * Elimina un producto únicamente cuando es seguro hacerlo.
     */
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            // Solicitamos al servicio la eliminación segura.
            productoService.eliminar(id);

            // Mostramos el mensaje de éxito.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("producto.eliminado")
            );

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Mostramos el motivo por el que no se pudo eliminar.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    e.getMessage()
            );
        }

        return "redirect:/productos";
    }

    // =========================================================
    // PRODUCTOS CON STOCK BAJO
    // =========================================================

    /**
     * Muestra los productos que necesitan reposición.
     */
    @GetMapping("/stock-bajo")
    public String listarStockBajo(Model model) {

        // Consultamos los productos con stock bajo.
        model.addAttribute(
                "productos",
                productoService.listarStockBajo()
        );

        return "productos/stock-bajo";
    }
}
