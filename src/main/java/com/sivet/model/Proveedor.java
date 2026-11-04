
package com.sivet.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa a los proveedores registrados en SIVET.
 * Un proveedor puede suministrar diferentes productos al negocio.
 */
@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
public class Proveedor {

    // Identificador único del proveedor.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre o razón social del proveedor.
    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
    @Column(nullable = false, length = 120)
    private String nombre;

    // Documento de identidad o RUC del proveedor.
    @Size(max = 20, message = "El documento no puede superar los 20 caracteres")
    @Column(name = "numero_documento", unique = true, length = 20)
    private String numeroDocumento;

    // Número telefónico de contacto.
    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    @Column(length = 20)
    private String telefono;

    // Correo electrónico de contacto.
    @Email(message = "Ingrese un correo electrónico válido")
    @Size(max = 100, message = "El correo no puede superar los 100 caracteres")
    @Column(length = 100)
    private String correo;

    // Dirección del proveedor.
    @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
    @Column(length = 200)
    private String direccion;

    // Indica si el proveedor se encuentra habilitado.
    @Column(nullable = false)
    private boolean activo = true;
}
