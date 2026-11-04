
package com.sivet.service;

import com.sivet.model.Categoria;
import com.sivet.repository.CategoriaRepository;
import com.sivet.repository.ProductoRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de gestionar la lógica de negocio
 * relacionada con las categorías de productos.
 */
@Service
@Transactional
public class CategoriaService {

    // Repositorio utilizado para acceder a las categorías.
    private final CategoriaRepository categoriaRepository;

    // Repositorio utilizado para comprobar productos asociados.
    private final ProductoRepository productoRepository;

    // Servicio encargado de recuperar mensajes personalizados.
    private final MensajeService mensajeService;

    // Inyección de dependencias mediante constructor.
    public CategoriaService(
            CategoriaRepository categoriaRepository,
            ProductoRepository productoRepository,
            MensajeService mensajeService) {

        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.mensajeService = mensajeService;
    }

    // Registra una nueva categoría.
    public Categoria registrar(String nombre, String descripcion) {

        // Limpiamos los espacios innecesarios.
        String nombreLimpio = nombre.trim();

        // Evitamos registrar categorías duplicadas.
        if (categoriaRepository.existsByNombre(nombreLimpio)) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje(
                            "categoria.duplicada",
                            nombreLimpio
                    )
            );
        }

        // Creamos la nueva categoría.
        Categoria categoria = new Categoria();
        categoria.setNombre(nombreLimpio);
        categoria.setDescripcion(descripcion);

        // Guardamos la categoría en PostgreSQL.
        return categoriaRepository.save(categoria);
    }

    // Obtiene todas las categorías registradas.
    @Transactional(readOnly = true)
    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    // Obtiene solamente las categorías activas.
    @Transactional(readOnly = true)
    public List<Categoria> listarActivas() {
        return categoriaRepository.findByActivoTrue();
    }

    // Busca una categoría por su identificador.
    @Transactional(readOnly = true)
    public Categoria buscarPorId(Long id) {

        return categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        mensajeService.obtenerMensaje(
                                "categoria.no.encontrada.id",
                                id
                        )
                ));
    }

    // Actualiza los datos de una categoría.
    public Categoria actualizar(Long id, String nombre, String descripcion) {

        // Buscamos la categoría que se actualizará.
        Categoria categoria = buscarPorId(id);

        // Limpiamos el nombre recibido.
        String nombreLimpio = nombre.trim();

        // Comprobamos que el nombre no pertenezca a otra categoría.
        Categoria existente = categoriaRepository.findByNombre(nombreLimpio)
                .orElse(null);

        if (existente != null && !existente.getId().equals(id)) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje(
                            "categoria.duplicada",
                            nombreLimpio
                    )
            );
        }

        // Actualizamos los datos.
        categoria.setNombre(nombreLimpio);
        categoria.setDescripcion(descripcion);

        // Guardamos los cambios.
        return categoriaRepository.save(categoria);
    }

    // Activa o desactiva una categoría.
    public Categoria cambiarEstado(Long id, boolean activo) {

        // Buscamos la categoría.
        Categoria categoria = buscarPorId(id);

        // Actualizamos su estado.
        categoria.setActivo(activo);

        // Guardamos el cambio.
        return categoriaRepository.save(categoria);
    }

    // Elimina definitivamente una categoría si no tiene productos asociados.
    public void eliminar(Long id) {

        // Buscamos la categoría y comprobamos que exista.
        Categoria categoria = buscarPorId(id);

        // No permitimos eliminar categorías que todavía tengan productos.
        if (!productoRepository.findByCategoriaId(id).isEmpty()) {
            throw new IllegalArgumentException(
                    mensajeService.obtenerMensaje("categoria.tiene.productos")
            );
        }

        // Eliminamos la categoría de la base de datos.
        categoriaRepository.delete(categoria);
    }
}