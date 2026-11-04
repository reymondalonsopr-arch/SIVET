
package com.sivet.controller.web;

import com.sivet.model.Producto;
import com.sivet.model.Venta;

import com.sivet.repository.ClienteRepository;
import com.sivet.repository.DetalleVentaRepository;
import com.sivet.repository.ProductoRepository;
import com.sivet.repository.UsuarioRepository;
import com.sivet.repository.VentaRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador encargado de mostrar el panel principal de SIVET.
 * Obtiene indicadores, ventas y estadísticas desde la base de datos.
 */
@Controller
public class DashboardController {

    // Repositorios para consultar los registros del sistema.
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    // Constructor para inyectar los repositorios necesarios.
    public DashboardController(
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            ProductoRepository productoRepository,
            VentaRepository ventaRepository,
            DetalleVentaRepository detalleVentaRepository) {

        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    // Muestra el dashboard con los indicadores principales.
    @GetMapping("/")
    public String mostrarDashboard(Model model) {

        // Obtiene la fecha actual del sistema.
        LocalDate hoy = LocalDate.now();

        // Define el inicio y el final del mes actual.
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = hoy.plusDays(1).atStartOfDay().minusNanos(1);

        // Define el periodo de los últimos siete días, incluyendo hoy.
        LocalDate inicioSemana = hoy.minusDays(6);
        LocalDateTime inicioSemanaHora = inicioSemana.atStartOfDay();
        LocalDateTime finSemanaHora = hoy.plusDays(1).atStartOfDay().minusNanos(1);

        // Consulta las ventas del mes actual.
        List<Venta> ventasMes =
                ventaRepository.findByFechaBetweenOrderByFechaDesc(
                        inicioMes,
                        finMes
                );

        // Calcula los ingresos únicamente con ventas registradas.
        BigDecimal ingresosMes = ventasMes.stream()
                .filter(venta -> "REGISTRADA".equals(venta.getEstado()))
                .map(venta -> venta.getTotal())
                .filter(total -> total != null)
                .reduce(
                        BigDecimal.ZERO,
                        (acumulado, importe) -> acumulado.add(importe)
                );

        // Consulta las ventas de los últimos siete días.
        List<Venta> ventasSemana =
                ventaRepository.findByFechaBetweenOrderByFechaDesc(
                        inicioSemanaHora,
                        finSemanaHora
                );

        // Prepara las etiquetas y cantidades para el gráfico semanal.
        List<String> diasGrafico = new ArrayList<>();
        List<BigDecimal> ventasGrafico = new ArrayList<>();

        DateTimeFormatter formatoDia = DateTimeFormatter.ofPattern("dd/MM");

        // Recorre los siete días para preparar los datos del gráfico.
        for (int i = 0; i < 7; i++) {

            LocalDate dia = inicioSemana.plusDays(i);

            // Suma las ventas registradas correspondientes a ese día.
            BigDecimal totalDia = ventasSemana.stream()
                    .filter(venta -> "REGISTRADA".equals(venta.getEstado()))
                    .filter(venta -> venta.getFecha().toLocalDate().equals(dia))
                    .map(venta -> venta.getTotal())
                    .filter(total -> total != null)
                    .reduce(
                            BigDecimal.ZERO,
                            (acumulado, importe) -> acumulado.add(importe)
                    );

            diasGrafico.add(dia.format(formatoDia));
            ventasGrafico.add(totalDia);
        }

        // Obtiene productos con stock bajo y conserva solamente los activos.
        List<Producto> productosStockBajo =
                productoRepository.buscarProductosConStockBajo()
                        .stream()
                        .filter(producto -> producto.isEstado())
                        .limit(5)
                        .toList();

        // Consulta los cinco productos más vendidos de los últimos treinta días.
        LocalDateTime inicioTopProductos =
                hoy.minusDays(29).atStartOfDay();

        List<Object[]> resultadoProductos =
                detalleVentaRepository.obtenerProductosMasVendidos(
                        inicioTopProductos,
                        finSemanaHora
                );

        // Prepara los nombres y cantidades para el gráfico de productos.
        List<String> nombresProductos = new ArrayList<>();
        List<Long> cantidadesProductos = new ArrayList<>();

        for (Object[] fila : resultadoProductos.stream().limit(5).toList()) {

            nombresProductos.add((String) fila[0]);
            cantidadesProductos.add(((Number) fila[1]).longValue());
        }

        // Consulta las últimas cinco ventas registradas.
        List<Venta> ultimasVentas = ventaRepository
                .findAllByOrderByFechaDesc()
                .stream()
                .filter(venta -> "REGISTRADA".equals(venta.getEstado()))
                .limit(5)
                .toList();

        // Cantidad total de usuarios registrados.
        model.addAttribute("totalUsuarios", usuarioRepository.count());

        // Cantidad total de clientes registrados.
        model.addAttribute("totalClientes", clienteRepository.count());

        // Cantidad total de productos registrados.
        model.addAttribute("totalProductos", productoRepository.count());

        // Cantidad de productos que necesitan reposición.
        model.addAttribute(
                "productosStockBajo",
                productoRepository.buscarProductosConStockBajo()
                        .stream()
                        .filter(producto -> producto.isEstado())
                        .count()
        );

        // Indicadores económicos y estadísticas del Dashboard.
        model.addAttribute("ingresosMes", ingresosMes);
        model.addAttribute("diasGrafico", diasGrafico);
        model.addAttribute("ventasGrafico", ventasGrafico);
        model.addAttribute("listaStockBajo", productosStockBajo);
        model.addAttribute("nombresProductos", nombresProductos);
        model.addAttribute("cantidadesProductos", cantidadesProductos);
        model.addAttribute("ultimasVentas", ultimasVentas);

        // Carga la vista principal.
        return "dashboard/index";
    }
}