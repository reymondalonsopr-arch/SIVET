
package com.sivet.controller.web;

import com.sivet.model.Cliente;
import com.sivet.service.ClienteService;
import com.sivet.service.MensajeService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador web encargado de gestionar las páginas
 * y operaciones del módulo de clientes de SIVET.
 */
@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final MensajeService mensajeService;

    public ClienteController(
            ClienteService clienteService,
            MensajeService mensajeService) {

        this.clienteService = clienteService;
        this.mensajeService = mensajeService;
    }

    /**
     * Muestra la lista de clientes registrados.
     */
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clienteService.listarTodos());
        return "clientes/index";
    }

    /**
     * Muestra el formulario para registrar un cliente.
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "clientes/formulario";
    }

    /**
     * Registra un cliente nuevo.
     */
    @PostMapping("/guardar")
    public String guardar(
            Cliente cliente,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            clienteService.registrar(cliente);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    mensajeService.obtenerMensaje("cliente.registrado")
            );

            return "redirect:/clientes";

        } catch (IllegalArgumentException e) {
            model.addAttribute("cliente", cliente);
            model.addAttribute("error", e.getMessage());
            return "clientes/formulario";
        }
    }

    /**
     * Muestra el formulario para editar un cliente.
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            model.addAttribute("cliente", clienteService.buscarPorId(id));
            return "clientes/formulario";

        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    mensajeService.obtenerMensaje("cliente.no.encontrado")
            );

            return "redirect:/clientes";
        }
    }

    /**
     * Actualiza los datos de un cliente.
     */
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id,
            Cliente cliente,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            clienteService.actualizar(id, cliente);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    mensajeService.obtenerMensaje("cliente.actualizado")
            );

            return "redirect:/clientes";

        } catch (IllegalArgumentException e) {
            cliente.setId(id);
            model.addAttribute("cliente", cliente);
            model.addAttribute("error", e.getMessage());
            return "clientes/formulario";

        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    mensajeService.obtenerMensaje("cliente.no.encontrado")
            );

            return "redirect:/clientes";
        }
    }

    /**
     * Activa o desactiva un cliente.
     */
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id,
            boolean activo,
            RedirectAttributes redirectAttributes) {

        try {
            clienteService.cambiarEstado(id, activo);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    mensajeService.obtenerMensaje("cliente.actualizado")
            );

        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    mensajeService.obtenerMensaje("cliente.no.encontrado")
            );
        }

        return "redirect:/clientes";
    }

    /**
     * Elimina un cliente si no tiene ventas asociadas.
     */
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            clienteService.eliminar(id);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    mensajeService.obtenerMensaje("cliente.eliminado")
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensaje", e.getMessage());

        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    mensajeService.obtenerMensaje("cliente.no.encontrado")
            );
        }

        return "redirect:/clientes";
    }
}
