
package com.sivet.controller.web;

import com.sivet.model.Producto;
import com.sivet.service.CategoriaService;
import com.sivet.service.MensajeService;
import com.sivet.service.ProductoService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador encargado de gestionar las operaciones web
 * relacionadas con los productos de SIVET.
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

    // Inyección de dependencias mediante constructor.
    public ProductoController(
            ProductoService productoService,
            CategoriaService categoriaService,
            MensajeService mensajeService) {

        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.mensajeService = mensajeService;
    }

    // Muestra el listado de productos.
    @GetMapping
    public String listar(Model model) {

        // Enviamos todos los productos a la vista.
        model.addAttribute("productos", productoService.listarTodos());

        return "productos/index";
    }

    // Muestra el formulario para registrar un producto.
    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        // Enviamos un producto vacío al formulario.
        model.addAttribute("producto", new Producto());

        // Cargamos las categorías disponibles.
        model.addAttribute("categorias", categoriaService.listarActivas());

        return "productos/formulario";
    }

    // Guarda un producto nuevo.
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("producto") Producto producto,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Registramos el producto mediante el servicio.
            productoService.registrar(producto);

            // Recuperamos el mensaje desde messages.properties.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("producto.registrado")
            );

            return "redirect:/productos";

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Conservamos las categorías para volver al formulario.
            model.addAttribute("categorias", categoriaService.listarActivas());

            // Mostramos el error encontrado.
            model.addAttribute("mensajeError", e.getMessage());

            return "productos/formulario";
        }
    }

    // Muestra el formulario para editar un producto.
    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Buscamos el producto seleccionado.
            model.addAttribute("producto", productoService.buscarPorId(id));

            // Cargamos las categorías activas.
            model.addAttribute("categorias", categoriaService.listarActivas());

            return "productos/formulario";

        } catch (EntityNotFoundException e) {

            // Recuperamos el mensaje de error desde properties.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    mensajeService.obtenerMensaje("producto.no.encontrado")
            );

            return "redirect:/productos";
        }
    }

    // Actualiza los datos de un producto.
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id,
            @ModelAttribute("producto") Producto producto,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Actualizamos el producto mediante el servicio.
            productoService.actualizar(id, producto);

            // Recuperamos el mensaje desde messages.properties.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("producto.actualizado")
            );

            return "redirect:/productos";

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Conservamos el identificador del producto.
            producto.setId(id);

            // Volvemos a cargar las categorías.
            model.addAttribute("categorias", categoriaService.listarActivas());

            // Mostramos el error en el formulario.
            model.addAttribute("mensajeError", e.getMessage());

            return "productos/formulario";
        }
    }

    // Activa o desactiva un producto.
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean estado,
            RedirectAttributes redirectAttributes) {

        try {
            // Cambiamos el estado del producto.
            productoService.cambiarEstado(id, estado);

            // Recuperamos el mensaje desde properties.
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

    // Elimina un producto únicamente si no tiene stock ni historial.
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

    // Muestra los productos que necesitan reposición.
    @GetMapping("/stock-bajo")
    public String listarStockBajo(Model model) {

        // Consultamos los productos con stock bajo.
        model.addAttribute("productos", productoService.listarStockBajo());

        return "productos/stock-bajo";
    }
}
