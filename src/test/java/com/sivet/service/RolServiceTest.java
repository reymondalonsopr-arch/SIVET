package com.sivet.service;

import com.sivet.model.Rol;
import com.sivet.repository.RolRepository;
import com.sivet.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MensajeService mensajeService;

    @InjectMocks
    private RolService rolService;

    private Rol rol;

    @BeforeEach
    void setUp() {
        // Preparar un rol de ejemplo.
        rol = new Rol();
        rol.setId(1L);
        rol.setNombre("SUPERVISOR");
        rol.setDescripcion("Rol para supervisores");

        // Devolver la clave del mensaje para simplificar las pruebas.
        lenient().when(mensajeService.obtenerMensaje(anyString()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        lenient().when(mensajeService.obtenerMensaje(anyString(), any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void registrarDebeGuardarRolConNombreRecortado() {
        when(rolRepository.existsByNombre("SUPERVISOR")).thenReturn(false);
        when(rolRepository.save(any(Rol.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Rol resultado = rolService.registrar(" SUPERVISOR ", "Rol de supervisión");

        assertEquals("SUPERVISOR", resultado.getNombre());
        assertEquals("Rol de supervisión", resultado.getDescripcion());
        verify(rolRepository).save(any(Rol.class));
    }

    @Test
    void registrarDebeRechazarNombreDuplicado() {
        when(rolRepository.existsByNombre("SUPERVISOR")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                rolService.registrar("SUPERVISOR", "Rol duplicado"));

        verify(rolRepository, never()).save(any());
    }

    @Test
    void listarTodosDebeRetornarRoles() {
        when(rolRepository.findAll()).thenReturn(List.of(rol));

        List<Rol> resultado = rolService.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals(rol, resultado.get(0));
    }

    @Test
    void buscarPorIdDebeRetornarRol() {
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rol));

        Rol resultado = rolService.buscarPorId(1L);

        assertEquals(rol, resultado);
    }

    @Test
    void buscarPorIdDebeLanzarExcepcionSiNoExiste() {
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                rolService.buscarPorId(99L));
    }

    @Test
    void buscarPorNombreDebeRetornarRol() {
        when(rolRepository.findByNombre("SUPERVISOR"))
                .thenReturn(Optional.of(rol));

        Rol resultado = rolService.buscarPorNombre("SUPERVISOR");

        assertEquals(rol, resultado);
    }

    @Test
    void buscarPorNombreDebeLanzarExcepcionSiNoExiste() {
        when(rolRepository.findByNombre("DESCONOCIDO"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                rolService.buscarPorNombre("DESCONOCIDO"));
    }

    @Test
    void eliminarDebeEliminarRolNoProtegidoSinUsuarios() {
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rol));
        when(usuarioRepository.existsByRolId(1L)).thenReturn(false);

        rolService.eliminar(1L);

        verify(rolRepository).delete(rol);
    }

    @Test
    void eliminarDebeRechazarRolAdminProtegido() {
        rol.setNombre("ADMIN");
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rol));

        assertThrows(IllegalArgumentException.class, () ->
                rolService.eliminar(1L));

        verify(usuarioRepository, never()).existsByRolId(anyLong());
        verify(rolRepository, never()).delete(any());
    }

    @Test
    void eliminarDebeRechazarRolVendedorProtegido() {
        rol.setNombre("VENDEDOR");
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rol));

        assertThrows(IllegalArgumentException.class, () ->
                rolService.eliminar(1L));

        verify(rolRepository, never()).delete(any());
    }

    @Test
    void eliminarDebeRechazarRolAlmaceneroProtegido() {
        rol.setNombre("ALMACENERO");
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rol));

        assertThrows(IllegalArgumentException.class, () ->
                rolService.eliminar(1L));

        verify(rolRepository, never()).delete(any());
    }

    @Test
    void eliminarDebeRechazarRolAsignadoAUsuarios() {
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rol));
        when(usuarioRepository.existsByRolId(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                rolService.eliminar(1L));

        verify(rolRepository, never()).delete(any());
    }

    @Test
    void eliminarDebeLanzarExcepcionSiRolNoExiste() {
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                rolService.eliminar(99L));

        verify(rolRepository, never()).delete(any());
    }
}