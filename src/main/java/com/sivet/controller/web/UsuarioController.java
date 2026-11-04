package com.sivet.controller.web;

import com.sivet.model.Rol;
import com.sivet.model.Usuario;
import com.sivet.repository.RolRepository;
import com.sivet.service.UsuarioService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador web encargado de gestionar las cuentas de usuario.
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final RolRepository rolRepository;

    /**
     * Inyección de dependencias mediante constructor.
     */
    public UsuarioController(
            UsuarioService usuarioService,
            RolRepository rolRepository) {

        this.usuarioService = usuarioService;
        this.rolRepository = rolRepository;
    }

    /**
     * Muestra la lista de usuarios registrados.
     */
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "usuarios/index";
    }

    /**
     * Muestra el formulario para registrar un usuario.
     *
     * Si viene de un error de validación, conserva los datos enviados
     * mediante los atributos flash.
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioRegistro(Model model) {

        // Solo creamos un usuario nuevo si no viene uno desde un redirect.
        if (!model.containsAttribute("usuario")) {
            model.addAttribute("usuario", new Usuario());
        }

        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("modoEdicion", false);

        return "usuarios/formulario";
    }

    /**
     * Registra un usuario nuevo.
     */
    @PostMapping("/nuevo")
    public String registrar(
            @RequestParam String nombreCompleto,
            @RequestParam String nombreUsuario,
            @RequestParam String password,
            @RequestParam(required = false) Long rolId,
            RedirectAttributes redirectAttributes) {

        try {
            usuarioService.registrar(
                    nombreCompleto,
                    nombreUsuario,
                    password,
                    rolId
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Usuario registrado correctamente."
            );

            return "redirect:/usuarios";

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Conservamos los datos ingresados, excepto la contraseña.
            Usuario usuarioFormulario = new Usuario();
            usuarioFormulario.setNombreCompleto(nombreCompleto);
            usuarioFormulario.setNombreUsuario(nombreUsuario);

            // Conservamos el rol seleccionado si todavía existe.
            if (rolId != null) {
                rolRepository.findById(rolId)
                        .ifPresent(usuarioFormulario::setRol);
            }

            redirectAttributes.addFlashAttribute(
                    "usuario",
                    usuarioFormulario
            );
            redirectAttributes.addFlashAttribute("error", e.getMessage());

            return "redirect:/usuarios/nuevo";
        }
    }

    /**
     * Muestra el formulario para editar un usuario existente.
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            // Si no hay datos flash, cargamos el usuario desde la base de datos.
            if (!model.containsAttribute("usuario")) {
                model.addAttribute("usuario", usuarioService.buscarPorId(id));
            }

            model.addAttribute("roles", rolRepository.findAll());
            model.addAttribute("modoEdicion", true);

            return "usuarios/formulario";

        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/usuarios";
        }
    }

    /**
     * Actualiza los datos generales de un usuario.
     */
    @PostMapping("/editar/{id}")
    public String actualizar(
            @PathVariable Long id,
            @RequestParam String nombreCompleto,
            @RequestParam String nombreUsuario,
            @RequestParam(required = false) Long rolId,
            RedirectAttributes redirectAttributes) {

        try {
            usuarioService.actualizar(
                    id,
                    nombreCompleto,
                    nombreUsuario,
                    rolId
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Usuario actualizado correctamente."
            );

            return "redirect:/usuarios";

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Conservamos los datos enviados para que puedan corregirse.
            Usuario usuarioFormulario = new Usuario();
            usuarioFormulario.setId(id);
            usuarioFormulario.setNombreCompleto(nombreCompleto);
            usuarioFormulario.setNombreUsuario(nombreUsuario);

            // Conservamos el rol seleccionado si todavía existe.
            if (rolId != null) {
                rolRepository.findById(rolId)
                        .ifPresent(usuarioFormulario::setRol);
            }

            redirectAttributes.addFlashAttribute(
                    "usuario",
                    usuarioFormulario
            );
            redirectAttributes.addFlashAttribute("error", e.getMessage());

            return "redirect:/usuarios/editar/" + id;
        }
    }

    /**
     * Activa o desactiva la cuenta de un usuario.
     */
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo,
            RedirectAttributes redirectAttributes) {

        try {
            usuarioService.cambiarEstado(id, activo);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    activo
                            ? "Usuario activado correctamente."
                            : "Usuario desactivado correctamente."
            );

        } catch (IllegalArgumentException | EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/usuarios";
    }

    /**
     * Elimina un usuario existente.
     */
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            usuarioService.eliminar(id);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Usuario eliminado correctamente."
            );

        } catch (IllegalArgumentException | EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/usuarios";
    }
}
