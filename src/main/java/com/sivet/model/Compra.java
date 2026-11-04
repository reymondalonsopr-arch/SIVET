
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
 * Entidad que representa las compras realizadas por el negocio.
 * Cada compra está asociada a un proveedor y posteriormente
 * tendrá sus respectivos detalles de productos.
 */
@Entity
@Table(name = "compras")
@Getter
@Setter
@NoArgsConstructor
public class Compra {

    // Identificador único de la compra.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Proveedor al que se realiza la compra.
    @NotNull(message = "Debe seleccionar un proveedor")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    // Fecha y hora en que se registra la compra.
    @NotNull(message = "La fecha de compra es obligatoria")
    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    // Importe total de la compra.
    @NotNull(message = "El total de la compra es obligatorio")
    @DecimalMin(value = "0.00", message = "El total no puede ser negativo")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // Observaciones adicionales sobre la compra.
    @Column(length = 250)
    private String observacion;

    // Estado de la compra: registrada o anulada.
    @Column(nullable = false, length = 20)
    private String estado = "REGISTRADA";
}
