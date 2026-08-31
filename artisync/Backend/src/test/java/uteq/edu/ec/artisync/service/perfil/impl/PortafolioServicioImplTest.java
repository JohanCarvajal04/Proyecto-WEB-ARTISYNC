package uteq.edu.ec.artisync.service.perfil.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPortafolio;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolio;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolio;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.perfil.Portafolio;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.perfil.PortafolioRepository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortafolioServicioImplTest {

    @Mock private PortafolioRepository portafolioRepository;
    @Mock private PerfilCreadorRepository perfilRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PortafolioServicioImpl portafolioServicio;

    private static final Long ID_USUARIO_DUENIO = 5L;
    private static final Long ID_USUARIO_AJENO = 99L;

    private PerfilCreador perfil;
    private Portafolio portafolio;

    @BeforeEach
    void setUp() {
        Usuario duenio = Usuario.builder().idUsuario(ID_USUARIO_DUENIO).build();
        perfil = PerfilCreador.builder().idPerfil(1L).usuario(duenio).build();
        portafolio = Portafolio.builder().idPortafolio(10L).perfil(perfil).esPublico(true)
                .totalVisitasAcumuladas(0).build();
    }

    @Test
    @DisplayName("crearPortafolio guarda con las opciones de personalizacion por defecto")
    void crearPortafolio_usaOpcionesPorDefecto() {
        PeticionCrearPortafolio peticion = new PeticionCrearPortafolio(1L, null, null);
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.empty());
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(portafolioRepository.save(any(Portafolio.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaPortafolio respuesta = portafolioServicio.crearPortafolio(peticion, ID_USUARIO_DUENIO);

        assertThat(respuesta.esPublico()).isTrue();
        assertThat(respuesta.opcionesPersonalizacion()).containsEntry("primary", "#0d6efd");
    }

    @Test
    @DisplayName("crearPortafolio respeta las opciones y visibilidad indicadas")
    void crearPortafolio_respetaValoresIndicados() {
        Map<String, String> opciones = Map.of("primary", "#000000");
        PeticionCrearPortafolio peticion = new PeticionCrearPortafolio(1L, false, opciones);
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.empty());
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(portafolioRepository.save(any(Portafolio.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaPortafolio respuesta = portafolioServicio.crearPortafolio(peticion, ID_USUARIO_DUENIO);

        assertThat(respuesta.esPublico()).isFalse();
        assertThat(respuesta.opcionesPersonalizacion()).isEqualTo(opciones);
    }

    @Test
    @DisplayName("crearPortafolio rechaza si el perfil ya tiene portafolio")
    void crearPortafolio_rechazaDuplicado() {
        PeticionCrearPortafolio peticion = new PeticionCrearPortafolio(1L, null, null);
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.of(portafolio));

        assertThatThrownBy(() -> portafolioServicio.crearPortafolio(peticion, ID_USUARIO_DUENIO))
                .isInstanceOf(ExcepcionRecursoDuplicado.class);
    }

    @Test
    @DisplayName("crearPortafolio lanza recurso no encontrado si el perfil no existe")
    void crearPortafolio_perfilInexistente() {
        PeticionCrearPortafolio peticion = new PeticionCrearPortafolio(1L, null, null);
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.empty());
        given(perfilRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> portafolioServicio.crearPortafolio(peticion, ID_USUARIO_DUENIO))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("crearPortafolio rechaza si el usuario logueado no es el dueño del perfil (IDOR)")
    void crearPortafolio_usuarioAjeno_lanzaExcepcion() {
        PeticionCrearPortafolio peticion = new PeticionCrearPortafolio(1L, null, null);
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.empty());
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));

        assertThatThrownBy(() -> portafolioServicio.crearPortafolio(peticion, ID_USUARIO_AJENO))
                .isInstanceOf(ExcepcionReglaNegocio.class);

        verify(portafolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("obtenerPortafolioPorId lanza recurso no encontrado si no existe")
    void obtenerPortafolioPorId_inexistente() {
        given(portafolioRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> portafolioServicio.obtenerPortafolioPorId(10L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerPortafolioPorPerfil devuelve el portafolio existente")
    void obtenerPortafolioPorPerfil_devuelve() {
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.of(portafolio));

        assertThat(portafolioServicio.obtenerPortafolioPorPerfil(1L).idPortafolio()).isEqualTo(10L);
    }

    @Test
    @DisplayName("obtenerPortafolioPorPerfil lanza recurso no encontrado si no existe")
    void obtenerPortafolioPorPerfil_inexistente() {
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> portafolioServicio.obtenerPortafolioPorPerfil(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("listarPortafolios mapea todos los registros")
    void listarPortafolios_mapea() {
        given(portafolioRepository.findAll()).willReturn(List.of(portafolio));

        assertThat(portafolioServicio.listarPortafolios()).hasSize(1);
    }

    @Test
    @DisplayName("actualizarPortafolio cambia visibilidad y opciones cuando se indican")
    void actualizarPortafolio_cambiaDatos() {
        PeticionActualizarPortafolio peticion = new PeticionActualizarPortafolio(false, Map.of("bg", "#000"));
        given(portafolioRepository.findById(10L)).willReturn(Optional.of(portafolio));
        given(portafolioRepository.save(any(Portafolio.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaPortafolio respuesta = portafolioServicio.actualizarPortafolio(10L, peticion, ID_USUARIO_DUENIO);

        assertThat(respuesta.esPublico()).isFalse();
        assertThat(respuesta.opcionesPersonalizacion()).containsEntry("bg", "#000");
    }

    @Test
    @DisplayName("actualizarPortafolio lanza recurso no encontrado si no existe")
    void actualizarPortafolio_inexistente() {
        given(portafolioRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> portafolioServicio.actualizarPortafolio(10L, new PeticionActualizarPortafolio(null, null), ID_USUARIO_DUENIO))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("actualizarPortafolio rechaza si el usuario logueado no es el dueño (IDOR, H-01)")
    void actualizarPortafolio_usuarioAjeno_lanzaExcepcion() {
        given(portafolioRepository.findById(10L)).willReturn(Optional.of(portafolio));

        assertThatThrownBy(() -> portafolioServicio.actualizarPortafolio(
                10L, new PeticionActualizarPortafolio(true, null), ID_USUARIO_AJENO))
                .isInstanceOf(ExcepcionReglaNegocio.class);

        verify(portafolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("incrementarVisitas suma una visita al total acumulado")
    void incrementarVisitas_suma() {
        given(portafolioRepository.findById(10L)).willReturn(Optional.of(portafolio));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(any(String.class), any(String.class), any(Duration.class))).willReturn(true);

        portafolioServicio.incrementarVisitas(10L, 1L);

        assertThat(portafolio.getTotalVisitasAcumuladas()).isEqualTo(1);
        verify(portafolioRepository).save(portafolio);
    }

    @Test
    @DisplayName("incrementarVisitas no repite una visita ya contada del mismo usuario en la ventana de dedup")
    void incrementarVisitas_deduplicaPorUsuario() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(any(String.class), any(String.class), any(Duration.class))).willReturn(false);

        portafolioServicio.incrementarVisitas(10L, 1L);

        assertThat(portafolio.getTotalVisitasAcumuladas()).isEqualTo(0);
        verify(portafolioRepository, never()).findById(any());
        verify(portafolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("incrementarVisitas lanza recurso no encontrado si no existe")
    void incrementarVisitas_inexistente() {
        given(portafolioRepository.findById(10L)).willReturn(Optional.empty());
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(any(String.class), any(String.class), any(Duration.class))).willReturn(true);

        assertThatThrownBy(() -> portafolioServicio.incrementarVisitas(10L, 1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("eliminarPortafolio borra cuando existe")
    void eliminarPortafolio_borraCuandoExiste() {
        given(portafolioRepository.existsById(10L)).willReturn(true);

        portafolioServicio.eliminarPortafolio(10L);

        verify(portafolioRepository).deleteById(10L);
    }

    @Test
    @DisplayName("eliminarPortafolio lanza recurso no encontrado si no existe")
    void eliminarPortafolio_inexistente() {
        given(portafolioRepository.existsById(10L)).willReturn(false);

        assertThatThrownBy(() -> portafolioServicio.eliminarPortafolio(10L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }
}
