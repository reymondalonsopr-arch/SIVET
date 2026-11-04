
package com.sivet.controller.web;

// Importa los modelos y repositorios necesarios.
import com.sivet.model.Compra;
import com.sivet.model.Venta;
import com.sivet.repository.ProductoRepository;
import com.sivet.repository.CompraRepository;
import com.sivet.repository.VentaRepository;
import com.sivet.repository.MovimientoInventarioRepository;
import com.sivet.repository.DetalleVentaRepository;

// Importaciones para trabajar con Spring MVC.
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Importaciones para trabajar con fechas y colecciones.
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Importaciones de Apache POI para crear archivos Excel.
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// Controlador encargado de mostrar y exportar los reportes.
@Controller
@RequestMapping("/reportes")
public class ReporteController {

    // Repositorios para consultar la información del sistema.
    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;
    private final VentaRepository ventaRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    // Constructor para inyectar los repositorios.
    public ReporteController(
            ProductoRepository productoRepository,
            CompraRepository compraRepository,
            VentaRepository ventaRepository,
            MovimientoInventarioRepository movimientoRepository,
            DetalleVentaRepository detalleVentaRepository) {

        this.productoRepository = productoRepository;
        this.compraRepository = compraRepository;
        this.ventaRepository = ventaRepository;
        this.movimientoRepository = movimientoRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    // Muestra los reportes generales y permite filtrar por fechas.
    @GetMapping
    public String mostrarReportes(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            Model model) {

        // Obtiene las fechas del periodo solicitado.
        LocalDate[] fechas = obtenerFechas(fechaInicio, fechaFin);
        fechaInicio = fechas[0];
        fechaFin = fechas[1];

        // Convierte las fechas al inicio y final del día.
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        // Consulta las compras y ventas del periodo.
        List<Compra> comprasPeriodo =
                compraRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin);

        List<Venta> ventasPeriodo =
                ventaRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin);

        // Calcula el importe total de las compras.
        BigDecimal totalComprasPeriodo = comprasPeriodo.stream()
                .map(compra -> compra.getTotal())
                .filter(total -> total != null)
                .reduce(
                        BigDecimal.ZERO,
                        (acumulado, importe) -> acumulado.add(importe)
                );

        // Calcula el importe total de las ventas.
        BigDecimal totalVentasPeriodo = ventasPeriodo.stream()
                .map(venta -> venta.getTotal())
                .filter(total -> total != null)
                .reduce(
                        BigDecimal.ZERO,
                        (acumulado, importe) -> acumulado.add(importe)
                );

        // Calcula la diferencia entre ventas y compras.
        BigDecimal balancePeriodo =
                totalVentasPeriodo.subtract(totalComprasPeriodo);

        // Prepara los datos mensuales para el gráfico.
        Map<YearMonth, BigDecimal> comprasPorMes = new TreeMap<>();
        Map<YearMonth, BigDecimal> ventasPorMes = new TreeMap<>();

        // Inicializa todos los meses del periodo seleccionado.
        YearMonth mesInicial = YearMonth.from(fechaInicio);
        YearMonth mesFinal = YearMonth.from(fechaFin);

        for (YearMonth mes = mesInicial;
             !mes.isAfter(mesFinal);
             mes = mes.plusMonths(1)) {

            comprasPorMes.put(mes, BigDecimal.ZERO);
            ventasPorMes.put(mes, BigDecimal.ZERO);
        }

        // Agrupa los importes de compras por mes.
        for (Compra compra : comprasPeriodo) {
            YearMonth mes = YearMonth.from(compra.getFecha());

            if (compra.getTotal() != null) {
                comprasPorMes.merge(
                        mes,
                        compra.getTotal(),
                        (acumulado, importe) -> acumulado.add(importe)
                );
            }
        }

        // Agrupa los importes de ventas por mes.
        for (Venta venta : ventasPeriodo) {
            YearMonth mes = YearMonth.from(venta.getFecha());

            if (venta.getTotal() != null) {
                ventasPorMes.merge(
                        mes,
                        venta.getTotal(),
                        (acumulado, importe) -> acumulado.add(importe)
                );
            }
        }

        // Convierte los datos mensuales en listas para los gráficos.
        List<String> mesesGrafico = new ArrayList<>();
        List<BigDecimal> comprasGrafico = new ArrayList<>();
        List<BigDecimal> ventasGrafico = new ArrayList<>();

        DateTimeFormatter formatoMes =
                DateTimeFormatter.ofPattern("MM/yyyy");

        for (YearMonth mes : comprasPorMes.keySet()) {
            mesesGrafico.add(mes.format(formatoMes));
            comprasGrafico.add(comprasPorMes.get(mes));
            ventasGrafico.add(ventasPorMes.get(mes));
        }

        // Consulta los productos más vendidos del periodo.
        List<Object[]> resultadosProductos =
                detalleVentaRepository.obtenerProductosMasVendidos(
                        inicio, fin);

        // Prepara los nombres y cantidades para el gráfico.
        List<String> nombresProductos = new ArrayList<>();
        List<Long> cantidadesProductos = new ArrayList<>();

        // Muestra como máximo los diez productos más vendidos.
        for (int i = 0;
             i < Math.min(resultadosProductos.size(), 10);
             i++) {

            Object[] resultado = resultadosProductos.get(i);

            nombresProductos.add(String.valueOf(resultado[0]));
            cantidadesProductos.add(
                    ((Number) resultado[1]).longValue());
        }

        // Mantiene los indicadores generales del sistema.
        model.addAttribute("totalProductos", productoRepository.count());
        model.addAttribute("totalCompras", compraRepository.count());
        model.addAttribute("totalVentas", ventaRepository.count());
        model.addAttribute("totalMovimientos", movimientoRepository.count());

        // Envía los resultados del periodo a la vista.
        model.addAttribute("comprasPeriodo", comprasPeriodo);
        model.addAttribute("ventasPeriodo", ventasPeriodo);
        model.addAttribute("totalComprasPeriodo", totalComprasPeriodo);
        model.addAttribute("totalVentasPeriodo", totalVentasPeriodo);
        model.addAttribute("balancePeriodo", balancePeriodo);

        // Envía los datos de los gráficos a Thymeleaf.
        model.addAttribute("mesesGrafico", mesesGrafico);
        model.addAttribute("comprasGrafico", comprasGrafico);
        model.addAttribute("ventasGrafico", ventasGrafico);
        model.addAttribute("nombresProductos", nombresProductos);
        model.addAttribute("cantidadesProductos", cantidadesProductos);

        // Conserva las fechas seleccionadas en el formulario.
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);

        // Abre la página de reportes.
        return "reportes/index";
    }

    // Descarga un archivo Excel con las compras y ventas del periodo.
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin) throws IOException {

        // Obtiene y valida el periodo solicitado.
        LocalDate[] fechas = obtenerFechas(fechaInicio, fechaFin);
        fechaInicio = fechas[0];
        fechaFin = fechas[1];

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        // Consulta las compras y ventas del periodo.
        List<Compra> compras =
                compraRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin);

        List<Venta> ventas =
                ventaRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin);

        // Crea el libro de Excel y sus dos hojas.
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            // Prepara los estilos de los encabezados.
            Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);

            CellStyle estiloEncabezado = workbook.createCellStyle();
            estiloEncabezado.setFont(fuenteEncabezado);

            // Crea la hoja de compras.
            Sheet hojaCompras = workbook.createSheet("Compras");

            String[] encabezadosCompras = {
                    "ID", "Fecha", "Proveedor", "Estado", "Total (S/)"
            };

            crearEncabezado(hojaCompras, encabezadosCompras, estiloEncabezado);

            int filaCompra = 1;

            // Escribe cada compra en una fila de Excel.
            for (Compra compra : compras) {
                Row fila = hojaCompras.createRow(filaCompra++);

                fila.createCell(0).setCellValue(compra.getId());
                fila.createCell(1).setCellValue(
                        formatearFecha(compra.getFecha()));

                fila.createCell(2).setCellValue(
                        compra.getProveedor() != null
                                ? compra.getProveedor().getNombre()
                                : "Sin proveedor");

                fila.createCell(3).setCellValue(compra.getEstado());

                fila.createCell(4).setCellValue(
                        compra.getTotal() != null
                                ? compra.getTotal().doubleValue()
                                : 0);
            }

            // Ajusta el ancho de las columnas de compras.
            ajustarColumnas(hojaCompras, encabezadosCompras.length);

            // Crea la hoja de ventas.
            Sheet hojaVentas = workbook.createSheet("Ventas");

            String[] encabezadosVentas = {
                    "ID", "Fecha", "Cliente", "Estado", "Total (S/)"
            };

            crearEncabezado(hojaVentas, encabezadosVentas, estiloEncabezado);

            int filaVenta = 1;

            // Escribe cada venta en una fila de Excel.
            for (Venta venta : ventas) {
                Row fila = hojaVentas.createRow(filaVenta++);

                fila.createCell(0).setCellValue(venta.getId());
                fila.createCell(1).setCellValue(
                        formatearFecha(venta.getFecha()));

                fila.createCell(2).setCellValue(
                        venta.getCliente() != null
                                ? venta.getCliente().getNombre()
                                : "Cliente general");

                fila.createCell(3).setCellValue(venta.getEstado());

                fila.createCell(4).setCellValue(
                        venta.getTotal() != null
                                ? venta.getTotal().doubleValue()
                                : 0);
            }

            // Ajusta el ancho de las columnas de ventas.
            ajustarColumnas(hojaVentas, encabezadosVentas.length);

            // Convierte el libro de Excel en bytes.
            workbook.write(salida);

            // Prepara la descarga del archivo.
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            headers.set(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=reporte_sivet.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(salida.toByteArray());
        }
    }

    // Establece las fechas predeterminadas y corrige rangos invertidos.
    private LocalDate[] obtenerFechas(
            LocalDate fechaInicio,
            LocalDate fechaFin) {

        LocalDate hoy = LocalDate.now();

        if (fechaInicio == null) {
            fechaInicio = hoy.withDayOfMonth(1);
        }

        if (fechaFin == null) {
            fechaFin = hoy;
        }

        // Si el rango está invertido, se utiliza el mes actual.
        if (fechaInicio.isAfter(fechaFin)) {
            fechaInicio = hoy.withDayOfMonth(1);
            fechaFin = hoy;
        }

        return new LocalDate[]{fechaInicio, fechaFin};
    }

    // Crea la primera fila de una hoja con sus encabezados.
    private void crearEncabezado(
            Sheet hoja,
            String[] encabezados,
            CellStyle estilo) {

        Row fila = hoja.createRow(0);

        for (int i = 0; i < encabezados.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(encabezados[i]);
            celda.setCellStyle(estilo);
        }
    }

    // Ajusta automáticamente el ancho de las columnas.
    private void ajustarColumnas(Sheet hoja, int cantidadColumnas) {
        for (int i = 0; i < cantidadColumnas; i++) {
            hoja.autoSizeColumn(i);
        }
    }

    // Convierte la fecha y hora a un texto legible para Excel.
    private String formatearFecha(LocalDateTime fecha) {
        if (fecha == null) {
            return "";
        }

        return fecha.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}