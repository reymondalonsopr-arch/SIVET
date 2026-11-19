
package com.sivet.service;

import com.sivet.model.Categoria;
import com.sivet.repository.CategoriaRepository;
import com.sivet.repository.ProductoRepository;

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
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio CategoriaService.
 * Se utilizan mocks para evitar acceder a la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private MensajeService mensajeService;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Bebidas");
        categoria.setDescripcion("Bebidas en general");
        categoria.setActivo(true);
    }

    @Test
    void registrarCategoriaValidaDebeGuardarCategoria() {
        // Arrange
        when(categoriaRepository.existsByNombre("Bebidas")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Categoria resultado = categoriaService.registrar(" Bebidas ", "Bebidas en general");

        // Assert
        assertNotNull(resultado);
        assertEquals("Bebidas", resultado.getNombre());
        assertEquals("Bebidas en general", resultado.getDescripcion());

        verify(categoriaRepository).existsByNombre("Bebidas");
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void registrarCategoriaDuplicadaDebeLanzarExcepcion() {
        // Arrange
        when(categoriaRepository.existsByNombre("Bebidas")).thenReturn(true);
        when(mensajeService.obtenerMensaje("categoria.duplicada", "Bebidas"))
                .thenReturn("La categoría ya existe");

        // Act y Assert
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.registrar(" Bebidas ", "Descripción")
        );

        assertEquals("La categoría ya existe", excepcion.getMessage());
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void listarTodasDebeRetornarCategoriasDelRepositorio() {
        // Arrange
        List<Categoria> categorias = List.of(categoria);
        when(categoriaRepository.findAll()).thenReturn(categorias);

        // Act
        List<Categoria> resultado = categoriaService.listarTodas();

        // Assert
        assertEquals(1, resultado.size());
        assertSame(categorias, resultado);
        verify(categoriaRepository).findAll();
    }

    @Test
    void listarActivasDebeRetornarCategoriasActivas() {
        // Arrange
        List<Categoria> categoriasActivas = List.of(categoria);
        when(categoriaRepository.findByActivoTrue()).thenReturn(categoriasActivas);

        // Act
        List<Categoria> resultado = categoriaService.listarActivas();

        // Assert
        assertEquals(1, resultado.size());
        assertSame(categoriasActivas, resultado);
        verify(categoriaRepository).findByActivoTrue();
    }

    @Test
    void buscarPorIdExistenteDebeRetornarCategoria() {
        // Arrange
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Act
        Categoria resultado = categoriaService.buscarPorId(1L);

        // Assert
        assertSame(categoria, resultado);
        verify(categoriaRepository).findById(1L);
    }

    @Test
    void buscarPorIdInexistenteDebeLanzarExcepcion() {
        // Arrange
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());
        when(mensajeService.obtenerMensaje("categoria.no.encontrada.id", 99L))
                .thenReturn("Categoría no encontrada");

        // Act y Assert
        EntityNotFoundException excepcion = assertThrows(
                EntityNotFoundException.class,
                () -> categoriaService.buscarPorId(99L)
        );

        assertEquals("Categoría no encontrada", excepcion.getMessage());
    }

    @Test
    void actualizarCategoriaDebeGuardarCambios() {
        // Arrange
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.findByNombre("Lácteos")).thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Categoria resultado = categoriaService.actualizar(
                1L, " Lácteos ", "Productos lácteos"
        );

        // Assert
        assertEquals("Lácteos", resultado.getNombre());
        assertEquals("Productos lácteos", resultado.getDescripcion());
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void actualizarConNombreDeOtraCategoriaDebeLanzarExcepcion() {
        // Arrange
        Categoria otraCategoria = new Categoria();
        otraCategoria.setId(2L);
        otraCategoria.setNombre("Lácteos");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.findByNombre("Lácteos"))
                .thenReturn(Optional.of(otraCategoria));
        when(mensajeService.obtenerMensaje("categoria.duplicada", "Lácteos"))
                .thenReturn("La categoría ya existe");

        // Act y Assert
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.actualizar(1L, "Lácteos", "Descripción")
        );

        assertEquals("La categoría ya existe", excepcion.getMessage());
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void actualizarConMismoNombreDebePermitirGuardar() {
        // Arrange
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.findByNombre("Bebidas")).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Categoria resultado = categoriaService.actualizar(
                1L, " Bebidas ", "Descripción actualizada"
        );

        // Assert
        assertEquals("Bebidas", resultado.getNombre());
        assertEquals("Descripción actualizada", resultado.getDescripcion());
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void cambiarEstadoDebeActualizarCategoria() {
        // Arrange
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Categoria resultado = categoriaService.cambiarEstado(1L, false);

        // Assert
        assertFalse(resultado.isActivo());
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void eliminarCategoriaSinProductosDebeEliminarla() {
        // Arrange
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.findByCategoriaId(1L)).thenReturn(List.of());

        // Act
        categoriaService.eliminar(1L);

        // Assert
        verify(categoriaRepository).delete(categoria);
    }

    @Test
    void eliminarCategoriaConProductosDebeLanzarExcepcion() {
        // Arrange
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.findByCategoriaId(1L))
                .thenReturn(List.of(mock(com.sivet.model.Producto.class)));
        when(mensajeService.obtenerMensaje("categoria.tiene.productos"))
                .thenReturn("La categoría tiene productos asociados");

        // Act y Assert
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> categoriaService.eliminar(1L)
        );

        assertEquals("La categoría tiene productos asociados", excepcion.getMessage());
        verify(categoriaRepository, never()).delete(any(Categoria.class));
    }
}
