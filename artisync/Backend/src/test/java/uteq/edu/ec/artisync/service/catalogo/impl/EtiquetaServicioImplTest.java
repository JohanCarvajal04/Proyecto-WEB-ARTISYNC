package uteq.edu.ec.artisync.service.catalogo.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearEtiqueta;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaEtiqueta;
import uteq.edu.ec.artisync.entity.catalogo.Etiqueta;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.EtiquetaRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EtiquetaServicioImplTest {

    @Mock private EtiquetaRepository etiquetaRepository;

    @InjectMocks
    private EtiquetaServicioImpl etiquetaServicio;

    @Test
    @DisplayName("listarEtiquetas mapea todas las etiquetas")
    void listarEtiquetas_mapea() {
        Etiqueta et = Etiqueta.builder().idEtiqueta(1L).nombreEtiqueta("Digital").build();
        given(etiquetaRepository.findAll()).willReturn(List.of(et));

        assertThat(etiquetaServicio.listarEtiquetas()).hasSize(1);
    }

    @Test
    @DisplayName("obtenerPorId devuelve la etiqueta cuando existe")
    void obtenerPorId_devuelveEtiqueta() {
        Etiqueta et = Etiqueta.builder().idEtiqueta(1L).nombreEtiqueta("Digital").build();
        given(etiquetaRepository.findById(1L)).willReturn(Optional.of(et));

        RespuestaEtiqueta respuesta = etiquetaServicio.obtenerPorId(1L);

        assertThat(respuesta.getNombreEtiqueta()).isEqualTo("Digital");
    }

    @Test
    @DisplayName("obtenerPorId lanza recurso no encontrado si no existe")
    void obtenerPorId_inexistente() {
        given(etiquetaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> etiquetaServicio.obtenerPorId(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("crearEtiqueta guarda cuando el nombre no esta repetido")
    void crearEtiqueta_guarda() {
        PeticionCrearEtiqueta peticion = PeticionCrearEtiqueta.builder().nombreEtiqueta("Retro").build();
        given(etiquetaRepository.existsByNombreEtiquetaIgnoreCase("Retro")).willReturn(false);
        given(etiquetaRepository.save(any(Etiqueta.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaEtiqueta respuesta = etiquetaServicio.crearEtiqueta(peticion);

        assertThat(respuesta.getNombreEtiqueta()).isEqualTo("Retro");
    }

    @Test
    @DisplayName("crearEtiqueta rechaza un nombre duplicado")
    void crearEtiqueta_rechazaDuplicado() {
        PeticionCrearEtiqueta peticion = PeticionCrearEtiqueta.builder().nombreEtiqueta("Digital").build();
        given(etiquetaRepository.existsByNombreEtiquetaIgnoreCase("Digital")).willReturn(true);

        assertThatThrownBy(() -> etiquetaServicio.crearEtiqueta(peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
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
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }
}
