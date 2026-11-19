package com.sivet.controller.web;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Controlador encargado de mostrar la página de autenticación.
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(CsrfToken csrfToken) {
        // Genera el token antes de que Thymeleaf comience a escribir la respuesta.
        csrfToken.getToken();

        return "auth/login";
    }
}