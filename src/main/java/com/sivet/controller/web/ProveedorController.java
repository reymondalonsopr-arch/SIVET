
package com.sivet.controller.web;

import com.sivet.model.Proveedor;
import com.sivet.service.ProveedorService;
import com.sivet.service.MensajeService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador web encargado de gestionar las operaciones
 * del módulo de proveedores de SIVET.
 */
@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    // Servicio que contiene la lógica de negocio de proveedores.
    private final ProveedorService proveedorService;

    // Servicio centralizado para obtener mensajes personalizados.
    private final MensajeService mensajeService;

    // Constructor para inyectar los servicios.
    public ProveedorController(
            ProveedorService proveedorService,
            MensajeService mensajeService) {

        this.proveedorService = proveedorService;
        this.mensajeService = mensajeService;
    }

    // Muestra el listado de proveedores.
    @GetMapping
    public String listar(Model model) {

        // Envía todos los proveedores a la vista.
        model.addAttribute(
                "proveedores",
                proveedorService.listarTodos()
        );

        return "proveedores/index";
    }

    // Muestra el formulario para registrar un proveedor.
    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        // Crea un proveedor vacío para enlazarlo con el formulario.
        model.addAttribute("proveedor", new Proveedor());

        return "proveedores/formulario";
    }

    // Guarda un proveedor nuevo.
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("proveedor") Proveedor proveedor,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {

            // Registra el proveedor mediante el servicio.
            proveedorService.registrar(proveedor);

            // Envía el mensaje personalizado de registro.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("proveedor.registrado")
            );

            return "redirect:/proveedores";

        } catch (IllegalArgumentException e) {

            // Conserva el formulario y muestra el error.
            model.addAttribute("mensajeError", e.getMessage());

            return "proveedores/formulario";
        }
    }

    // Muestra el formulario con los datos del proveedor seleccionado.
    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            // Busca el proveedor que se desea editar.
            model.addAttribute(
                    "proveedor",
                    proveedorService.buscarPorId(id)
            );

            return "proveedores/formulario";

        } catch (EntityNotFoundException e) {

            // Muestra el mensaje cuando no se encuentra el proveedor.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    e.getMessage()
            );

            return "redirect:/proveedores";
        }
    }

    // Actualiza los datos de un proveedor existente.
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id,
            @ModelAttribute("proveedor") Proveedor proveedor,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {

            // Ejecuta la actualización mediante el servicio.
            proveedorService.actualizar(id, proveedor);

            // Envía el mensaje personalizado de actualización.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("proveedor.actualizado")
            );

            return "redirect:/proveedores";

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Conserva el identificador para volver a editar.
            proveedor.setId(id);

            model.addAttribute("mensajeError", e.getMessage());

            return "proveedores/formulario";
        }
    }

    // Activa o desactiva un proveedor.
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            // Cambia el estado del proveedor seleccionado.
            proveedorService.cambiarEstado(id);

            // Informa que el estado se actualizó correctamente.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("proveedor.actualizado")
            );

        } catch (EntityNotFoundException e) {

            // Informa si no se encuentra el proveedor.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    e.getMessage()
            );
        }

        return "redirect:/proveedores";
    }

    // Elimina un proveedor si no tiene compras asociadas.
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            // Ejecuta la eliminación segura mediante el servicio.
            proveedorService.eliminar(id);

            // Informa que el proveedor se eliminó correctamente.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("proveedor.eliminado")
            );

        } catch (IllegalArgumentException e) {

            // Informa por qué no se puede eliminar el proveedor.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    e.getMessage()
            );

        } catch (EntityNotFoundException e) {

            // Informa si el proveedor ya no existe.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    e.getMessage()
            );
        }

        return "redirect:/proveedores";
    }
}
