
package com.sivet.controller.web;

import com.sivet.model.Rol;
import com.sivet.service.MensajeService;
import com.sivet.service.RolService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador web encargado de gestionar los roles
 * del sistema SIVET.
 */
@Controller
@RequestMapping("/roles")
public class RolController {

    // Servicio que contiene la lógica de negocio de los roles.
    private final RolService rolService;

    // Servicio encargado de recuperar mensajes personalizados.
    private final MensajeService mensajeService;

    /**
     * Inyección de los servicios mediante constructor.
     */
    public RolController(
            RolService rolService,
            MensajeService mensajeService) {

        this.rolService = rolService;
        this.mensajeService = mensajeService;
    }

    /**
     * Muestra todos los roles registrados.
     */
    @GetMapping
    public String listar(Model model) {

        // Consultamos los roles registrados en PostgreSQL.
        model.addAttribute("roles", rolService.listarTodos());

        return "roles/index";
    }

    /**
     * Muestra el formulario para registrar un rol.
     */
    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {

        // Creamos un objeto vacío para enlazar el formulario.
        model.addAttribute("rol", new Rol());

        return "roles/formulario";
    }

    /**
     * Registra un nuevo rol.
     */
    @PostMapping("/guardar")
    public String guardar(
            Rol rol,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {

            // Registramos el rol utilizando el servicio.
            rolService.registrar(
                    rol.getNombre(),
                    rol.getDescripcion()
            );

            // Mostramos el mensaje personalizado de registro.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("rol.registrado")
            );

            return "redirect:/roles";

        } catch (IllegalArgumentException e) {

            // Si ocurre un error, conservamos los datos ingresados.
            model.addAttribute("rol", rol);
            model.addAttribute("error", e.getMessage());

            return "roles/formulario";
        }
    }

    /**
     * Elimina un rol si no está asignado a ningún usuario.
     */
    @PostMapping("/eliminar")
    public String eliminar(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes) {

        try {

            // Solicitamos al servicio eliminar el rol de forma segura.
            rolService.eliminar(id);

            // Mostramos el mensaje personalizado de eliminación.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("rol.eliminado")
            );

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Informamos por qué no se pudo eliminar.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    e.getMessage()
            );
        }

        return "redirect:/roles";
    }
}
