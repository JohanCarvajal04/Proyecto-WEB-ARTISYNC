package uteq.edu.ec.artisync.service.catalogo.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateTagRequest;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.TagResponse;
import uteq.edu.ec.artisync.entity.catalogo.Tag;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.TagRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock private TagRepository etiquetaRepository;

    @InjectMocks
    private TagServiceImpl etiquetaServicio;

    @Test
    @DisplayName("listarEtiquetas mapea todas las etiquetas")
    void listarEtiquetas_mapea() {
        Tag et = Tag.builder().idEtiqueta(1L).nombreEtiqueta("Digital").build();
        given(etiquetaRepository.findAll()).willReturn(List.of(et));

        assertThat(etiquetaServicio.listarEtiquetas()).hasSize(1);
    }

    @Test
    @DisplayName("obtenerPorId devuelve la etiqueta cuando existe")
    void obtenerPorId_devuelveEtiqueta() {
        Tag et = Tag.builder().idEtiqueta(1L).nombreEtiqueta("Digital").build();
        given(etiquetaRepository.findById(1L)).willReturn(Optional.of(et));

        TagResponse respuesta = etiquetaServicio.obtenerPorId(1L);

        assertThat(respuesta.getNombreEtiqueta()).isEqualTo("Digital");
    }

    @Test
    @DisplayName("obtenerPorId lanza recurso no encontrado si no existe")
    void obtenerPorId_inexistente() {
        given(etiquetaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> etiquetaServicio.obtenerPorId(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crearEtiqueta guarda cuando el nombre no esta repetido")
    void crearEtiqueta_guarda() {
        CreateTagRequest peticion = CreateTagRequest.builder().nombreEtiqueta("Retro").build();
        given(etiquetaRepository.existsByNombreEtiquetaIgnoreCase("Retro")).willReturn(false);
        given(etiquetaRepository.save(any(Tag.class))).willAnswer(inv -> inv.getArgument(0));

        TagResponse respuesta = etiquetaServicio.crearEtiqueta(peticion);

        assertThat(respuesta.getNombreEtiqueta()).isEqualTo("Retro");
    }

    @Test
    @DisplayName("crearEtiqueta rechaza un nombre duplicado")
    void crearEtiqueta_rechazaDuplicado() {
        CreateTagRequest peticion = CreateTagRequest.builder().nombreEtiqueta("Digital").build();
        given(etiquetaRepository.existsByNombreEtiquetaIgnoreCase("Digital")).willReturn(true);

        assertThatThrownBy(() -> etiquetaServicio.crearEtiqueta(peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("eliminarEtiqueta borra cuando existe")
    void eliminarEtiqueta_borraCuandoExiste() {
        given(etiquetaRepository.existsById(1L)).willReturn(true);

        etiquetaServicio.eliminarEtiqueta(1L);

        verify(etiquetaRepository).deleteById(1L);
    }

    @Test
    @DisplayName("eliminarEtiqueta lanza recurso no encontrado si no existe")
    void eliminarEtiqueta_inexistente() {
        given(etiquetaRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> etiquetaServicio.eliminarEtiqueta(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
