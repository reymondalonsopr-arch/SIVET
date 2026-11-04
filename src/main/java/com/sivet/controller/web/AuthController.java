
package com.sivet.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador encargado de mostrar las páginas
 * relacionadas con la autenticación de SIVET.
 */
@Controller
public class AuthController {

    /**
     * Muestra el formulario de inicio de sesión.
     */
    @GetMapping("/login")
    public String mostrarLogin() {

        // Carga templates/auth/login.html.
        return "auth/login";
    }
}
