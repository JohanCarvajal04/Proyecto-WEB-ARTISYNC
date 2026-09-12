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
import uteq.edu.ec.artisync.dto.peticion.perfil.UpdatePortfolioRequest;
import uteq.edu.ec.artisync.dto.peticion.perfil.CreatePortfolioRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.PortfolioResponse;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.perfil.Portfolio;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.perfil.CreatorProfileRepository;
import uteq.edu.ec.artisync.repository.perfil.PortfolioRepository;

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
class PortfolioServiceImplTest {

    @Mock private PortfolioRepository portafolioRepository;
    @Mock private CreatorProfileRepository perfilRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PortfolioServiceImpl portafolioServicio;

    private static final Long ID_USUARIO_DUENIO = 5L;
    private static final Long ID_USUARIO_AJENO = 99L;

    private CreatorProfile perfil;
    private Portfolio portafolio;

    @BeforeEach
    void setUp() {
        User duenio = User.builder().idUsuario(ID_USUARIO_DUENIO).build();
        perfil = CreatorProfile.builder().idPerfil(1L).usuario(duenio).build();
        portafolio = Portfolio.builder().idPortafolio(10L).perfil(perfil).esPublico(true)
                .totalVisitasAcumuladas(0).build();
    }

    @Test
    @DisplayName("createPortfolio guarda con las opciones de personalizacion por defecto")
    void crearPortafolio_usaOpcionesPorDefecto() {
        CreatePortfolioRequest peticion = new CreatePortfolioRequest(1L, null, null);
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.empty());
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(portafolioRepository.save(any(Portfolio.class))).willAnswer(inv -> inv.getArgument(0));

        PortfolioResponse respuesta = portafolioServicio.createPortfolio(peticion, ID_USUARIO_DUENIO);

        assertThat(respuesta.esPublico()).isTrue();
        assertThat(respuesta.opcionesPersonalizacion()).containsEntry("primary", "#0d6efd");
    }

    @Test
    @DisplayName("createPortfolio respeta las opciones y visibilidad indicadas")
    void crearPortafolio_respetaValoresIndicados() {
        Map<String, String> opciones = Map.of("primary", "#000000");
        CreatePortfolioRequest peticion = new CreatePortfolioRequest(1L, false, opciones);
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.empty());
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(portafolioRepository.save(any(Portfolio.class))).willAnswer(inv -> inv.getArgument(0));

        PortfolioResponse respuesta = portafolioServicio.createPortfolio(peticion, ID_USUARIO_DUENIO);

        assertThat(respuesta.esPublico()).isFalse();
        assertThat(respuesta.opcionesPersonalizacion()).isEqualTo(opciones);
    }

    @Test
    @DisplayName("createPortfolio rechaza si el perfil ya tiene portafolio")
    void crearPortafolio_rechazaDuplicado() {
        CreatePortfolioRequest peticion = new CreatePortfolioRequest(1L, null, null);
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.of(portafolio));

        assertThatThrownBy(() -> portafolioServicio.createPortfolio(peticion, ID_USUARIO_DUENIO))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("createPortfolio lanza recurso no encontrado si el perfil no existe")
    void crearPortafolio_perfilInexistente() {
        CreatePortfolioRequest peticion = new CreatePortfolioRequest(1L, null, null);
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.empty());
        given(perfilRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> portafolioServicio.createPortfolio(peticion, ID_USUARIO_DUENIO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createPortfolio rechaza si el usuario logueado no es el dueño del perfil (IDOR)")
    void crearPortafolio_usuarioAjeno_lanzaExcepcion() {
        CreatePortfolioRequest peticion = new CreatePortfolioRequest(1L, null, null);
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.empty());
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));

        assertThatThrownBy(() -> portafolioServicio.createPortfolio(peticion, ID_USUARIO_AJENO))
                .isInstanceOf(BusinessRuleException.class);

        verify(portafolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("getPortfolioById lanza recurso no encontrado si no existe")
    void obtenerPortafolioPorId_inexistente() {
        given(portafolioRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> portafolioServicio.getPortfolioById(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getPortfolioByProfile devuelve el portafolio existente")
    void obtenerPortafolioPorPerfil_devuelve() {
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.of(portafolio));

        assertThat(portafolioServicio.getPortfolioByProfile(1L).idPortafolio()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getPortfolioByProfile lanza recurso no encontrado si no existe")
    void obtenerPortafolioPorPerfil_inexistente() {
        given(portafolioRepository.findByPerfilIdPerfil(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> portafolioServicio.getPortfolioByProfile(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("REQ-NF-018: getPortfolioById oculta el portafolio de un dueño con la cuenta desactivada/suprimida")
    void obtenerPortafolioPorId_ocultaCuentaDesactivada() {
        User duenioDesactivado = User.builder().idUsuario(ID_USUARIO_DUENIO).estadoCuenta(false).build();
        CreatorProfile perfilDesactivado = CreatorProfile.builder().idPerfil(1L).usuario(duenioDesactivado).build();
        Portfolio portafolioDesactivado = Portfolio.builder().idPortafolio(10L).perfil(perfilDesactivado).esPublico(true)
                .totalVisitasAcumuladas(0).build();
        given(portafolioRepository.findById(10L)).willReturn(Optional.of(portafolioDesactivado));

        assertThatThrownBy(() -> portafolioServicio.getPortfolioById(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listPortfolios mapea todos los registros")
    void listarPortafolios_mapea() {
        given(portafolioRepository.findAll()).willReturn(List.of(portafolio));

        assertThat(portafolioServicio.listPortfolios()).hasSize(1);
    }

    @Test
    @DisplayName("updatePortfolio cambia visibilidad y opciones cuando se indican")
    void actualizarPortafolio_cambiaDatos() {
        UpdatePortfolioRequest peticion = new UpdatePortfolioRequest(false, Map.of("bg", "#000"));
        given(portafolioRepository.findById(10L)).willReturn(Optional.of(portafolio));
        given(portafolioRepository.save(any(Portfolio.class))).willAnswer(inv -> inv.getArgument(0));

        PortfolioResponse respuesta = portafolioServicio.updatePortfolio(10L, peticion, ID_USUARIO_DUENIO);

        assertThat(respuesta.esPublico()).isFalse();
        assertThat(respuesta.opcionesPersonalizacion()).containsEntry("bg", "#000");
    }

    @Test
    @DisplayName("updatePortfolio no modifica nada cuando ningun campo viene informado")
    void actualizarPortafolio_sinCambios_mantieneValoresOriginales() {
        given(portafolioRepository.findById(10L)).willReturn(Optional.of(portafolio));
        given(portafolioRepository.save(any(Portfolio.class))).willAnswer(inv -> inv.getArgument(0));

        PortfolioResponse respuesta = portafolioServicio.updatePortfolio(
                10L, new UpdatePortfolioRequest(null, null), ID_USUARIO_DUENIO);

        assertThat(respuesta.esPublico()).isTrue();
    }

    @Test
    @DisplayName("getPortfolioById usa idPerfil nulo cuando el portafolio no tiene perfil asociado")
    void obtenerPortafolioPorId_sinPerfilAsociado_idPerfilNulo() {
        Portfolio sinPerfil = Portfolio.builder().idPortafolio(11L).perfil(null).esPublico(true)
                .totalVisitasAcumuladas(0).build();
        given(portafolioRepository.findById(11L)).willReturn(Optional.of(sinPerfil));

        PortfolioResponse respuesta = portafolioServicio.getPortfolioById(11L);

        assertThat(respuesta.idPerfil()).isNull();
    }

    @Test
    @DisplayName("updatePortfolio lanza recurso no encontrado si no existe")
    void actualizarPortafolio_inexistente() {
        given(portafolioRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> portafolioServicio.updatePortfolio(10L, new UpdatePortfolioRequest(null, null), ID_USUARIO_DUENIO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updatePortfolio rechaza si el usuario logueado no es el dueño (IDOR, H-01)")
    void actualizarPortafolio_usuarioAjeno_lanzaExcepcion() {
        given(portafolioRepository.findById(10L)).willReturn(Optional.of(portafolio));

        assertThatThrownBy(() -> portafolioServicio.updatePortfolio(
                10L, new UpdatePortfolioRequest(true, null), ID_USUARIO_AJENO))
                .isInstanceOf(BusinessRuleException.class);

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
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deletePortfolio borra cuando existe")
    void eliminarPortafolio_borraCuandoExiste() {
        given(portafolioRepository.existsById(10L)).willReturn(true);

        portafolioServicio.deletePortfolio(10L);

        verify(portafolioRepository).deleteById(10L);
    }

    @Test
    @DisplayName("deletePortfolio lanza recurso no encontrado si no existe")
    void eliminarPortafolio_inexistente() {
        given(portafolioRepository.existsById(10L)).willReturn(false);

        assertThatThrownBy(() -> portafolioServicio.deletePortfolio(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
