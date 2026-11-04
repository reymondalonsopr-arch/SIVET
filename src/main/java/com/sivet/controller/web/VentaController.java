package com.sivet.controller.web;

import com.sivet.model.DetalleVenta;
import com.sivet.model.Producto;
import com.sivet.model.Venta;

import com.sivet.service.ClienteService;
import com.sivet.service.ProductoService;
import com.sivet.service.VentaService;
import com.sivet.service.MensajeService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador web encargado de gestionar el módulo de ventas.
 * Permite registrar, consultar y anular ventas.
 */
@Controller
@RequestMapping("/ventas")
public class VentaController {

    // Servicios necesarios para gestionar las ventas.
    private final VentaService ventaService;
    private final ProductoService productoService;
    private final ClienteService clienteService;

    // Servicio centralizado para obtener mensajes personalizados.
    private final MensajeService mensajeService;

    // Constructor para inyectar los servicios.
    public VentaController(
            VentaService ventaService,
            ProductoService productoService,
            ClienteService clienteService,
            MensajeService mensajeService) {

        this.ventaService = ventaService;
        this.productoService = productoService;
        this.clienteService = clienteService;
        this.mensajeService = mensajeService;
    }

    // Muestra el listado de ventas registradas.
    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "ventas",
                ventaService.listarTodas()
        );

        return "ventas/index";
    }

    // Muestra el formulario para registrar una nueva venta.
    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        // Prepara una venta vacía.
        model.addAttribute("venta", new Venta());

        // Carga los clientes activos.
        model.addAttribute(
                "clientes",
                clienteService.listarActivos()
        );

        // Carga los productos activos.
        model.addAttribute(
                "productos",
                productoService.listarActivos()
        );

        return "ventas/formulario";
    }

    // Recibe y registra una venta con sus respectivos detalles.
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("venta") Venta venta,
            @RequestParam(value = "productoIds", required = false)
            List<Long> productoIds,
            @RequestParam(value = "cantidades", required = false)
            List<Integer> cantidades,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {

            // Comprueba que se hayan enviado productos y cantidades.
            if (productoIds == null
                    || cantidades == null
                    || productoIds.isEmpty()
                    || productoIds.size() != cantidades.size()) {

                throw new IllegalArgumentException(
                        "Agrega al menos un producto y completa sus cantidades"
                );
            }

            // Prepara los detalles de la venta.
            List<DetalleVenta> detalles = new ArrayList<>();

            // Construye cada detalle con los datos recibidos.
            for (int i = 0; i < productoIds.size(); i++) {

                // Crea una referencia al producto seleccionado.
                Producto producto = new Producto();
                producto.setId(productoIds.get(i));

                // Crea el detalle de la venta.
                DetalleVenta detalle = new DetalleVenta();
                detalle.setProducto(producto);
                detalle.setCantidad(cantidades.get(i));

                detalles.add(detalle);
            }

            // Registra la venta y descuenta el inventario.
            ventaService.registrar(venta, detalles);

            // Envía el mensaje personalizado de confirmación.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("venta.registrada")
            );

            return "redirect:/ventas";

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Conserva los datos necesarios para mostrar nuevamente el formulario.
            model.addAttribute("mensajeError", e.getMessage());
            model.addAttribute("venta", venta);

            model.addAttribute(
                    "clientes",
                    clienteService.listarActivos()
            );

            model.addAttribute(
                    "productos",
                    productoService.listarActivos()
            );

            return "ventas/formulario";
        }
    }

    /**
     * Anula una venta y devuelve sus productos al inventario.
     */
    @PostMapping("/anular/{id}")
    public String anular(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            // Ejecuta la anulación desde el servicio.
            ventaService.anular(id);

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "La venta VEN-" + id + " fue anulada y el stock fue actualizado."
            );

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Muestra el motivo por el que no se pudo anular.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    e.getMessage()
            );
        }

        return "redirect:/ventas";
    }

    // Muestra los detalles de una venta determinada.
    @GetMapping("/detalle/{id}")
    public String verDetalle(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            // Carga la información general de la venta.
            model.addAttribute(
                    "venta",
                    ventaService.buscarPorId(id)
            );

            // Carga los productos incluidos en la venta.
            model.addAttribute(
                    "detalles",
                    ventaService.listarDetalles(id)
            );

            return "ventas/detalle";

        } catch (EntityNotFoundException e) {

            // Informa cuando no se encuentra la venta.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    e.getMessage()
            );

            return "redirect:/ventas";
        }
    }
}
