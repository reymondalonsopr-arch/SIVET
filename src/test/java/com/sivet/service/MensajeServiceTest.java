package com.sivet.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MensajeServiceTest {

    private MessageSource messageSource;
    private MensajeService mensajeService;

    @BeforeEach
    void setUp() {
        // Configurar el idioma predeterminado de las pruebas.
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es-PE"));

        // Crear el mock del administrador de mensajes.
        messageSource = mock(MessageSource.class);

        // Crear el servicio con el mock.
        mensajeService = new MensajeService(messageSource);
    }

    @AfterEach
    void limpiarLocale() {
        // Evitar que el idioma de una prueba afecte a las siguientes.
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void obtenerMensajeDebeRetornarMensajeTraducido() {
        String clave = "usuario.no.encontrado";
        String mensajeEsperado = "Usuario no encontrado";
        Locale locale = Locale.forLanguageTag("es-PE");

        when(messageSource.getMessage(
                eq(clave),
                isNull(),
                eq("[" + clave + "]"),
                eq(locale)
        )).thenReturn(mensajeEsperado);

        String resultado = mensajeService.obtenerMensaje(clave);

        assertEquals(mensajeEsperado, resultado);
    }

    @Test
    void obtenerMensajeDebeRetornarTextoAlternativoSiNoExisteTraduccion() {
        String clave = "clave.inexistente";
        String mensajeAlternativo = "[" + clave + "]";
        Locale locale = Locale.forLanguageTag("es-PE");

        when(messageSource.getMessage(
                eq(clave),
                isNull(),
                eq(mensajeAlternativo),
                eq(locale)
        )).thenReturn(mensajeAlternativo);

        String resultado = mensajeService.obtenerMensaje(clave);

        assertEquals(mensajeAlternativo, resultado);
    }

    @Test
    void obtenerMensajeConParametrosDebeRetornarMensajeFormateado() {
        String clave = "venta.stock.insuficiente";
        Object[] parametros = {"Producto A", 5};
        String mensajeEsperado = "Stock insuficiente para Producto A. Disponible: 5";
        Locale locale = Locale.forLanguageTag("es-PE");

        when(messageSource.getMessage(
                eq(clave),
                any(Object[].class),
                eq("[" + clave + "]"),
                eq(locale)
        )).thenReturn(mensajeEsperado);

        String resultado = mensajeService.obtenerMensaje(clave, parametros);

        assertEquals(mensajeEsperado, resultado);
        verify(messageSource).getMessage(
                eq(clave),
                any(Object[].class),
                eq("[" + clave + "]"),
                eq(locale)
        );
    }

    @Test
    void obtenerMensajeConParametrosDebeRetornarTextoAlternativoSiNoExisteTraduccion() {
        String clave = "clave.inexistente";
        Object[] parametros = {"Dato"};
        String mensajeAlternativo = "[" + clave + "]";
        Locale locale = Locale.forLanguageTag("es-PE");

        when(messageSource.getMessage(
                eq(clave),
                any(Object[].class),
                eq(mensajeAlternativo),
                eq(locale)
        )).thenReturn(mensajeAlternativo);

        String resultado = mensajeService.obtenerMensaje(clave, parametros);

        assertEquals(mensajeAlternativo, resultado);
    }

    @Test
    void obtenerMensajeDebeUtilizarElLocaleActual() {
        Locale locale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(locale);

        String clave = "usuario.no.encontrado";
        String mensajeEsperado = "User not found";

        when(messageSource.getMessage(
                eq(clave),
                isNull(),
                eq("[" + clave + "]"),
                eq(locale)
        )).thenReturn(mensajeEsperado);

        String resultado = mensajeService.obtenerMensaje(clave);

        assertEquals(mensajeEsperado, resultado);
    }

    @Test
    void obtenerMensajeConParametrosDebeUtilizarElLocaleActual() {
        Locale locale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(locale);

        String clave = "venta.stock.insuficiente";
        Object[] parametros = {"Product A", 3};
        String mensajeEsperado = "Insufficient stock for Product A. Available: 3";

        when(messageSource.getMessage(
                eq(clave),
                any(Object[].class),
                eq("[" + clave + "]"),
                eq(locale)
        )).thenReturn(mensajeEsperado);

        String resultado = mensajeService.obtenerMensaje(clave, parametros);

        assertEquals(mensajeEsperado, resultado);
    }
}