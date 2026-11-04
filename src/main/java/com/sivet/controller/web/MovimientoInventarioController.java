
package com.sivet.controller.web;

import com.sivet.service.MovimientoInventarioService;
import com.sivet.service.ProductoService;
import com.sivet.service.MensajeService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador web encargado de mostrar y registrar
 * los movimientos de inventario de SIVET.
 */
@Controller
@RequestMapping("/movimientos")
public class MovimientoInventarioController {

    // Servicios necesarios para gestionar los movimientos.
    private final MovimientoInventarioService movimientoService;
    private final ProductoService productoService;

    // Servicio centralizado para obtener mensajes personalizados.
    private final MensajeService mensajeService;

    // Constructor para inyectar los servicios.
    public MovimientoInventarioController(
            MovimientoInventarioService movimientoService,
            ProductoService productoService,
            MensajeService mensajeService) {

        this.movimientoService = movimientoService;
        this.productoService = productoService;
        this.mensajeService = mensajeService;
    }

    // Muestra el historial completo de movimientos.
    @GetMapping
    public String listar(Model model) {

        // Obtiene todos los movimientos ordenados por fecha.
        model.addAttribute(
                "movimientos",
                movimientoService.listarTodos()
        );

        return "movimientos/index";
    }

    // Muestra el formulario para registrar una entrada o salida.
    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        // Carga los productos activos.
        model.addAttribute(
                "productos",
                productoService.listarActivos()
        );

        return "movimientos/formulario";
    }

    // Registra manualmente una entrada o salida.
    @PostMapping("/guardar")
    public String guardar(
            @RequestParam("productoId") Long productoId,
            @RequestParam("tipo") String tipo,
            @RequestParam("cantidad") Integer cantidad,
            @RequestParam("motivo") String motivo,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            // Comprueba el tipo de movimiento solicitado.
            if ("ENTRADA".equals(tipo)) {

                movimientoService.registrarEntrada(
                        productoId,
                        cantidad,
                        motivo
                );

                // Mensaje personalizado para una entrada.
                redirectAttributes.addFlashAttribute(
                        "mensajeExito",
                        mensajeService.obtenerMensaje("movimiento.entrada")
                );

            } else if ("SALIDA".equals(tipo)) {

                movimientoService.registrarSalida(
                        productoId,
                        cantidad,
                        motivo
                );

                // Mensaje personalizado para una salida.
                redirectAttributes.addFlashAttribute(
                        "mensajeExito",
                        mensajeService.obtenerMensaje("movimiento.salida")
                );

            } else {

                throw new IllegalArgumentException(
                        "Seleccione un tipo de movimiento válido"
                );
            }

            return "redirect:/movimientos";

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Conserva los productos para volver a mostrar el formulario.
            model.addAttribute(
                    "productos",
                    productoService.listarActivos()
            );

            // Muestra el error encontrado.
            model.addAttribute("mensajeError", e.getMessage());

            return "movimientos/formulario";
        }
    }

    // Muestra el historial de movimientos de un producto.
    @GetMapping("/producto/{id}")
    public String listarPorProducto(
            @PathVariable Long id,
            Model model) {

        // Obtiene el historial del producto seleccionado.
        model.addAttribute(
                "movimientos",
                movimientoService.listarPorProducto(id)
        );

        // Obtiene la información del producto.
        model.addAttribute(
                "producto",
                productoService.buscarPorId(id)
        );

        return "movimientos/producto";
    }
}
