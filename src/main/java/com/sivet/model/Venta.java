
package com.sivet.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa las ventas realizadas en SIVET.
 * Cada venta puede contener varios productos y detalles.
 */
@Entity
@Table(name = "ventas")
@Getter
@Setter
@NoArgsConstructor
public class Venta {

    // Identificador único de la venta.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cliente al que se realiza la venta.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // Fecha y hora en que se registra la venta.
    @NotNull(message = "La fecha de venta es obligatoria")
    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    // Importe total de los productos vendidos.
    @NotNull(message = "El total de la venta es obligatorio")
    @DecimalMin(value = "0.00", message = "El total no puede ser negativo")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // Estado de la venta: REGISTRADA o ANULADA.
    @Column(nullable = false, length = 20)
    private String estado = "REGISTRADA";

    // Observaciones adicionales de la venta.
    @Column(length = 250)
    private String observacion;
}
