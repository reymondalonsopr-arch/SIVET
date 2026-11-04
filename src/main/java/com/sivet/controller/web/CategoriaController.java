
package com.sivet.controller.web;

import com.sivet.model.Categoria;
import com.sivet.service.CategoriaService;
import com.sivet.service.MensajeService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador encargado de gestionar las pantallas
 * y operaciones web de las categorías de SIVET.
 */
@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    // Servicio que contiene la lógica de negocio.
    private final CategoriaService categoriaService;

    // Servicio centralizado para obtener mensajes personalizados.
    private final MensajeService mensajeService;

    // Inyección de servicios mediante constructor.
    public CategoriaController(
            CategoriaService categoriaService,
            MensajeService mensajeService) {

        this.categoriaService = categoriaService;
        this.mensajeService = mensajeService;
    }

    // Muestra el listado de categorías.
    @GetMapping
    public String listar(Model model) {

        // Enviamos todas las categorías a la vista.
        model.addAttribute("categorias", categoriaService.listarTodas());

        return "categorias/index";
    }

    // Muestra el formulario para registrar una categoría.
    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        // Enviamos una categoría vacía al formulario.
        model.addAttribute("categoria", new Categoria());

        return "categorias/formulario";
    }

    // Guarda una categoría nueva.
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("categoria") Categoria categoria,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {

            // Registramos la categoría mediante el servicio.
            categoriaService.registrar(
                    categoria.getNombre(),
                    categoria.getDescripcion()
            );

            // Enviamos el mensaje personalizado de registro.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("categoria.registrada")
            );

            return "redirect:/categorias";

        } catch (IllegalArgumentException e) {

            // Conservamos los datos del formulario si ocurre un error.
            model.addAttribute("mensajeError", e.getMessage());

            return "categorias/formulario";
        }
    }

    // Muestra el formulario para editar una categoría.
    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            // Buscamos la categoría seleccionada.
            model.addAttribute("categoria", categoriaService.buscarPorId(id));

            return "categorias/formulario";

        } catch (EntityNotFoundException e) {

            // Informamos si la categoría no existe.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    mensajeService.obtenerMensaje("categoria.no.encontrada")
            );

            return "redirect:/categorias";
        }
    }

    // Actualiza los datos de una categoría.
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id,
            @ModelAttribute("categoria") Categoria categoria,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {

            // Actualizamos la categoría mediante el servicio.
            categoriaService.actualizar(
                    id,
                    categoria.getNombre(),
                    categoria.getDescripcion()
            );

            // Enviamos el mensaje personalizado de actualización.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("categoria.actualizada")
            );

            return "redirect:/categorias";

        } catch (IllegalArgumentException e) {

            // Conservamos el identificador para mantener el modo edición.
            categoria.setId(id);

            // Mostramos el error en el formulario.
            model.addAttribute("mensajeError", e.getMessage());

            return "categorias/formulario";

        } catch (EntityNotFoundException e) {

            // Informamos si la categoría ya no existe.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    mensajeService.obtenerMensaje("categoria.no.encontrada")
            );

            return "redirect:/categorias";
        }
    }

    // Activa o desactiva una categoría.
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo,
            RedirectAttributes redirectAttributes) {

        try {

            // Cambiamos el estado de la categoría.
            categoriaService.cambiarEstado(id, activo);

            // Informamos que el estado fue actualizado.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("categoria.actualizada")
            );

        } catch (EntityNotFoundException e) {

            // Informamos si no se encontró la categoría.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    mensajeService.obtenerMensaje("categoria.no.encontrada")
            );
        }

        return "redirect:/categorias";
    }

    // Elimina definitivamente una categoría si no tiene productos asociados.
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            // Solicitamos al servicio que valide y elimine la categoría.
            categoriaService.eliminar(id);

            // Informamos que la categoría fue eliminada.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("categoria.eliminada")
            );

        } catch (IllegalArgumentException e) {

            // Mostramos el motivo por el que no se pudo eliminar.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    e.getMessage()
            );

        } catch (EntityNotFoundException e) {

            // Informamos si la categoría no existe.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    mensajeService.obtenerMensaje("categoria.no.encontrada")
            );
        }

        return "redirect:/categorias";
    }
}
