package com.sivet.controller.web;

import com.sivet.model.Compra;
import com.sivet.model.DetalleCompra;
import com.sivet.model.Producto;
import com.sivet.service.CompraService;
import com.sivet.service.ProductoService;
import com.sivet.service.ProveedorService;
import com.sivet.service.MensajeService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador web encargado de gestionar el módulo de compras.
 * Permite mostrar, registrar, consultar y anular compras.
 */
@Controller
@RequestMapping("/compras")
public class CompraController {

    // Servicios necesarios para gestionar las compras.
    private final CompraService compraService;
    private final ProductoService productoService;
    private final ProveedorService proveedorService;

    // Servicio centralizado para obtener mensajes personalizados.
    private final MensajeService mensajeService;

    // Constructor para inyectar los servicios.
    public CompraController(
            CompraService compraService,
            ProductoService productoService,
            ProveedorService proveedorService,
            MensajeService mensajeService) {

        this.compraService = compraService;
        this.productoService = productoService;
        this.proveedorService = proveedorService;
        this.mensajeService = mensajeService;
    }

    // Muestra el listado de compras registradas.
    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "compras",
                compraService.listarTodas()
        );

        return "compras/index";
    }

    // Muestra el formulario para registrar una nueva compra.
    @GetMapping("/nuevo")
    public String nuevo(Model model) {

        // Prepara una compra vacía para el formulario.
        model.addAttribute("compra", new Compra());

        // Carga los proveedores habilitados.
        model.addAttribute(
                "proveedores",
                proveedorService.listarActivos()
        );

        // Carga los productos habilitados.
        model.addAttribute(
                "productos",
                productoService.listarActivos()
        );

        return "compras/formulario";
    }

    // Recibe y registra una compra con sus respectivos detalles.
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("compra") Compra compra,
            @RequestParam(value = "productoIds", required = false)
            List<Long> productoIds,
            @RequestParam(value = "cantidades", required = false)
            List<Integer> cantidades,
            @RequestParam(value = "precios", required = false)
            List<BigDecimal> precios,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {

            // Comprueba que las listas de detalles tengan la misma longitud.
            if (productoIds == null
                    || cantidades == null
                    || precios == null
                    || productoIds.isEmpty()
                    || productoIds.size() != cantidades.size()
                    || productoIds.size() != precios.size()) {

                throw new IllegalArgumentException(
                        "Agrega al menos un producto y completa sus datos"
                );
            }

            // Prepara la lista de detalles de la compra.
            List<DetalleCompra> detalles = new ArrayList<>();

            // Construye cada detalle con los datos recibidos.
            for (int i = 0; i < productoIds.size(); i++) {

                DetalleCompra detalle = new DetalleCompra();

                // Asocia el identificador del producto seleccionado.
                Producto producto = new Producto();
                producto.setId(productoIds.get(i));

                detalle.setProducto(producto);
                detalle.setCantidad(cantidades.get(i));
                detalle.setPrecioUnitario(precios.get(i));

                detalles.add(detalle);
            }

            // Registra la compra y actualiza el inventario.
            compraService.registrar(compra, detalles);

            // Envía el mensaje personalizado de confirmación.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("compra.registrada")
            );

            return "redirect:/compras";

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Conserva los datos necesarios para volver a mostrar el formulario.
            model.addAttribute("mensajeError", e.getMessage());
            model.addAttribute("compra", compra);

            model.addAttribute(
                    "proveedores",
                    proveedorService.listarActivos()
            );

            model.addAttribute(
                    "productos",
                    productoService.listarActivos()
            );

            return "compras/formulario";
        }
    }

    // Anula una compra y revierte sus entradas de inventario.
    @PostMapping("/anular/{id}")
    public String anular(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            // Ejecuta la anulación desde el servicio.
            compraService.anular(id);

            // Informa que la compra fue anulada correctamente.
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    mensajeService.obtenerMensaje("compra.anulada")
            );

        } catch (IllegalArgumentException | EntityNotFoundException e) {

            // Informa por qué no se pudo anular la compra.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    e.getMessage()
            );
        }

        return "redirect:/compras";
    }

    // Muestra los detalles de una compra determinada.
    @GetMapping("/detalle/{id}")
    public String verDetalle(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            // Carga la información general de la compra.
            model.addAttribute(
                    "compra",
                    compraService.buscarPorId(id)
            );

            // Carga los productos incluidos en esa compra.
            model.addAttribute(
                    "detalles",
                    compraService.listarDetalles(id)
            );

            return "compras/detalle";

        } catch (EntityNotFoundException e) {

            // Informa cuando no se encuentra la compra.
            redirectAttributes.addFlashAttribute(
                    "mensajeError",
                    e.getMessage()
            );

            return "redirect:/compras";
        }
    }
}