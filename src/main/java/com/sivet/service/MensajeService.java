
package com.sivet.service;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Servicio centralizado para recuperar los mensajes de SIVET.
 * Los textos se obtienen desde messages.properties.
 */
@Service
public class MensajeService {

    // Componente de Spring que permite recuperar mensajes.
    private final MessageSource messageSource;

    /**
     * Constructor para inyectar el administrador de mensajes.
     */
    public MensajeService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Recupera un mensaje utilizando su clave.
     * Si no encuentra la traducción, devuelve un texto alternativo.
     */
    public String obtenerMensaje(String clave) {

        return messageSource.getMessage(
                clave,
                null,
                "[" + clave + "]",
                LocaleContextHolder.getLocale()
        );
    }

    /**
     * Recupera un mensaje que contiene parámetros dinámicos.
     */
    public String obtenerMensaje(String clave, Object... parametros) {

        return messageSource.getMessage(
                clave,
                parametros,
                "[" + clave + "]",
                LocaleContextHolder.getLocale()
        );
    }
}
