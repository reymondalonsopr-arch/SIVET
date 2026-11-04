package com.sivet.config;

import com.sivet.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que autentica las solicitudes de la API mediante JWT.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Leemos el token enviado en la cabecera Authorization.
        String authorizationHeader = request.getHeader("Authorization");

        // Si no hay token Bearer, continuamos sin autenticar.
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            // Extraemos el nombre de usuario desde el token.
            String nombreUsuario = jwtService.obtenerUsuario(token);

            if (nombreUsuario != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Consultamos los datos y permisos del usuario.
                UserDetails usuario =
                        userDetailsService.loadUserByUsername(nombreUsuario);

                // Creamos la autenticación con sus autoridades.
                UsernamePasswordAuthenticationToken autenticacion =
                        new UsernamePasswordAuthenticationToken(
                                usuario,
                                null,
                                usuario.getAuthorities()
                        );

                autenticacion.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                // Guardamos la autenticación en el contexto de seguridad.
                SecurityContextHolder.getContext()
                        .setAuthentication(autenticacion);
            }

        } catch (RuntimeException excepcion) {
            // Un token inválido o vencido no autentica la solicitud.
            SecurityContextHolder.clearContext();
        }

        // Continuamos con la cadena de filtros.
        filterChain.doFilter(request, response);
    }
}