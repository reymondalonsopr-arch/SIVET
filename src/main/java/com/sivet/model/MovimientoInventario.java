
package com.sivet.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad que registra las entradas y salidas de productos
 * del inventario de SIVET.
 */
@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoInventario {

    // Identificador único del movimiento.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Producto al que corresponde el movimiento.
    @NotNull(message = "Debe seleccionar un producto")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    // Compra que originó el movimiento, si corresponde.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    // Tipo de movimiento: ENTRADA o SALIDA.
    @NotBlank(message = "El tipo de movimiento es obligatorio")
    @Column(nullable = false, length = 20)
    private String tipo;

    // Cantidad de unidades que ingresan o salen.
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor que cero")
    @Column(nullable = false)
    private Integer cantidad;

    // Fecha y hora en que se registra el movimiento.
    @NotNull(message = "La fecha del movimiento es obligatoria")
    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    // Motivo por el que se realiza el movimiento.
    @NotBlank(message = "El motivo del movimiento es obligatorio")
    @Size(max = 250, message = "El motivo no puede superar los 250 caracteres")
    @Column(nullable = false, length = 250)
    private String motivo;
}
