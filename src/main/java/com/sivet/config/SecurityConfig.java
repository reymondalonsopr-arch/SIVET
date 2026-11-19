
package com.sivet.config;

import com.sivet.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad de SIVET.
 *
 * La API REST utiliza JWT y no mantiene sesiones.
 * La aplicación web utiliza formularios y sesiones de Spring Security.
 */
@Configuration
public class SecurityConfig {

    /**
     * Define BCrypt para proteger las contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expone el AuthenticationManager para autenticar credenciales
     * desde el endpoint de login de la API.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Cadena de seguridad para la API REST.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserDetailsService userDetailsService) throws Exception {

        // Creamos el filtro que valida los tokens JWT.
        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
            // Esta cadena se aplica únicamente a las rutas de la API.
            .securityMatcher("/api/**")

            // La API no utiliza sesiones ni protección CSRF basada en cookies.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // La API responde 401 en vez de redirigir al login web.
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                    )
            )

            // Definimos los permisos de la API según el rol del usuario.
            .authorizeHttpRequests(auth -> auth
                    // El login es público.
                    .requestMatchers("/api/auth/login").permitAll()

                    // Administración de usuarios: solo ADMIN.
                    .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                    // Inventario, compras y proveedores.
                    .requestMatchers(
                            "/api/productos/**",
                            "/api/categorias/**",
                            "/api/compras/**",
                            "/api/proveedores/**",
                            "/api/movimientos/**"
                    ).hasAnyRole("ADMIN", "ALMACENERO")

                    // Clientes y ventas.
                    .requestMatchers(
                            "/api/clientes/**",
                            "/api/ventas/**"
                    ).hasAnyRole("ADMIN", "VENDEDOR")

                    // Reportes: solo ADMIN.
                    .requestMatchers("/api/reportes/**").hasRole("ADMIN")

                    // Cualquier otra ruta requiere ADMIN.
                    .anyRequest().hasRole("ADMIN")
            )

            // Validamos el JWT antes del filtro de autenticación estándar.
            .addFilterBefore(
                    jwtFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /**
     * Cadena de seguridad para la aplicación web Thymeleaf.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // Esta cadena procesa las rutas que no pertenecen a la API.
            .securityMatcher("/**")

            .authorizeHttpRequests(auth -> auth
                    // Recursos públicos, página de login y página de error.
                    .requestMatchers(
                            "/login",
                            "/error",
                            "/css/**",
                            "/js/**",
                            "/images/**"
                    ).permitAll()

                    // Panel principal.
                    .requestMatchers("/").authenticated()

                    // Administración exclusiva del administrador.
                    .requestMatchers(
                            "/usuarios/**",
                            "/roles/**",
                            "/reportes/**",
                            "/configuracion/**"
                    ).hasRole("ADMIN")

                    // Inventario y compras.
                    .requestMatchers(
                            "/productos/**",
                            "/categorias/**",
                            "/compras/**",
                            "/proveedores/**",
                            "/movimientos/**"
                    ).hasAnyRole("ADMIN", "ALMACENERO")

                    // Clientes y ventas.
                    .requestMatchers(
                            "/clientes/**",
                            "/ventas/**"
                    ).hasAnyRole("ADMIN", "VENDEDOR")

                    // El resto requiere administrador.
                    .anyRequest().hasRole("ADMIN")
            )

            // Inicio de sesión web.
            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/", true)
                    .permitAll()
            )

            // Cierre de sesión web.
            .logout(logout -> logout
                    .logoutSuccessUrl("/login?logout")
                    .permitAll()
            );

        return http.build();
    }
}

