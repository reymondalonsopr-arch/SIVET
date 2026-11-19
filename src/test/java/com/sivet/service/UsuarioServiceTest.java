package com.sivet.service;

import com.sivet.model.Rol;
import com.sivet.model.Usuario;
import com.sivet.repository.RolRepository;
import com.sivet.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MensajeService mensajeService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Rol rolAdmin;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        // Preparar un rol válido para las pruebas.
        rolAdmin = new Rol();
        rolAdmin.setId(1L);
        rolAdmin.setNombre("ADMIN");

        // Preparar un usuario de ejemplo.
        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNombreCompleto("Ana Pérez");
        usuario.setNombreUsuario("ana");
        usuario.setPassword("hash-seguro");
        usuario.setRol(rolAdmin);
        usuario.setActivo(true);

        // Devolver la clave como mensaje para simplificar las pruebas.
        lenient().when(mensajeService.obtenerMensaje(anyString()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        lenient().when(mensajeService.obtenerMensaje(anyString(), any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void registrarDebeGuardarUsuarioConPasswordEncriptado() {
        when(usuarioRepository.existsByNombreUsuario("ana")).thenReturn(false);
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolAdmin));
        when(passwordEncoder.encode("clave123")).thenReturn("hash-seguro");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario resultado = usuarioService.registrar(
                " Ana Pérez ",
                " ana ",
                "clave123",
                1L
        );

        assertEquals("Ana Pérez", resultado.getNombreCompleto());
        assertEquals("ana", resultado.getNombreUsuario());
        assertEquals("hash-seguro", resultado.getPassword());
        assertEquals(rolAdmin, resultado.getRol());
        assertTrue(resultado.isActivo());

        verify(passwordEncoder).encode("clave123");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void registrarDebeRechazarNombreCompletoVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                usuarioService.registrar(" ", "ana", "clave123", 1L));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarDebeRechazarNombreUsuarioVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                usuarioService.registrar("Ana Pérez", " ", "clave123", 1L));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarDebeRechazarPasswordVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                usuarioService.registrar("Ana Pérez", "ana", " ", 1L));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarDebeRechazarNombreUsuarioDuplicado() {
        when(usuarioRepository.existsByNombreUsuario("ana")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                usuarioService.registrar("Ana Pérez", "ana", "clave123", 1L));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarDebeRechazarRolNulo() {
        when(usuarioRepository.existsByNombreUsuario("ana")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                usuarioService.registrar("Ana Pérez", "ana", "clave123", null));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarDebeRechazarRolInexistente() {
        when(usuarioRepository.existsByNombreUsuario("ana")).thenReturn(false);
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                usuarioService.registrar("Ana Pérez", "ana", "clave123", 99L));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarDebeRechazarRolNoPermitido() {
        Rol rolInvalido = new Rol();
        rolInvalido.setId(2L);
        rolInvalido.setNombre("INVITADO");

        when(usuarioRepository.existsByNombreUsuario("ana")).thenReturn(false);
        when(rolRepository.findById(2L)).thenReturn(Optional.of(rolInvalido));

        assertThrows(IllegalArgumentException.class, () ->
                usuarioService.registrar("Ana Pérez", "ana", "clave123", 2L));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void listarTodosDebeRetornarUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        List<Usuario> resultado = usuarioService.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals(usuario, resultado.get(0));
    }

    @Test
    void buscarPorIdDebeRetornarUsuario() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        Usuario resultado = usuarioService.buscarPorId(10L);

        assertEquals(usuario, resultado);
    }

    @Test
    void buscarPorIdDebeLanzarExcepcionSiNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                usuarioService.buscarPorId(99L));
    }

    @Test
    void buscarPorNombreUsuarioDebeRetornarUsuario() {
        when(usuarioRepository.findByNombreUsuario("ana"))
                .thenReturn(Optional.of(usuario));

        Usuario resultado = usuarioService.buscarPorNombreUsuario("ana");

        assertEquals(usuario, resultado);
    }

    @Test
    void buscarPorNombreUsuarioDebeLanzarExcepcionSiNoExiste() {
        when(usuarioRepository.findByNombreUsuario("desconocido"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                usuarioService.buscarPorNombreUsuario("desconocido"));
    }

    @Test
    void actualizarDebeModificarDatosDelUsuario() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByNombreUsuario("ana.actualizada"))
                .thenReturn(Optional.empty());
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolAdmin));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario resultado = usuarioService.actualizar(
                10L,
                "Ana Actualizada",
                "ana.actualizada",
                1L
        );

        assertEquals("Ana Actualizada", resultado.getNombreCompleto());
        assertEquals("ana.actualizada", resultado.getNombreUsuario());
        assertEquals(rolAdmin, resultado.getRol());

        // La actualización general no debe cambiar la contraseña.
        assertEquals("hash-seguro", resultado.getPassword());
    }

    @Test
    void actualizarDebePermitirConservarElMismoNombreUsuario() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByNombreUsuario("ana"))
                .thenReturn(Optional.of(usuario));
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolAdmin));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario resultado = usuarioService.actualizar(
                10L,
                "Ana Pérez Actualizada",
                "ana",
                1L
        );

        assertEquals("ana", resultado.getNombreUsuario());
    }

    @Test
    void actualizarDebeRechazarNombreUsuarioDeOtroUsuario() {
        Usuario otroUsuario = new Usuario();
        otroUsuario.setId(20L);
        otroUsuario.setNombreUsuario("otro");

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByNombreUsuario("otro"))
                .thenReturn(Optional.of(otroUsuario));

        assertThrows(IllegalArgumentException.class, () ->
                usuarioService.actualizar(10L, "Ana Pérez", "otro", 1L));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizarDebeRechazarDatosObligatoriosVacios() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class, () ->
                usuarioService.actualizar(10L, "", "ana", 1L));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoDebeActualizarEstadoDelUsuario() {
        // Usamos un usuario que no sea administrador para probar el cambio de estado.
        Rol rolUsuario = new Rol();
        rolUsuario.setId(2L);
        rolUsuario.setNombre("USUARIO");
        usuario.setRol(rolUsuario);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario resultado = usuarioService.cambiarEstado(10L, false);

        assertFalse(resultado.isActivo());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void eliminarDebeBuscarYEliminarUsuario() {
        // Usamos un usuario que no sea administrador para probar la eliminación.
        Rol rolUsuario = new Rol();
        rolUsuario.setId(2L);
        rolUsuario.setNombre("USUARIO");
        usuario.setRol(rolUsuario);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        usuarioService.eliminar(10L);

        verify(usuarioRepository).delete(usuario);
    }

    @Test
    void eliminarDebeLanzarExcepcionSiUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                usuarioService.eliminar(99L));

        verify(usuarioRepository, never()).delete(any());
    }
}
