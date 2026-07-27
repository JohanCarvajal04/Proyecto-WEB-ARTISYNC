package uteq.edu.ec.artisync.service.social.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearSorteo;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaParticipante;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaSorteo;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.social.ParticipanteSorteo;
import uteq.edu.ec.artisync.entity.social.Sorteo;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.comunicacion.SeguidorRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.repository.social.ParticipanteSorteoRepository;
import uteq.edu.ec.artisync.repository.social.SorteoRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Pruebas unitarias para SorteoServiceImpl.
 * RF-23: Valida reglas de negocio de sorteos — participación, restricciones de edición y creación.
 */
@ExtendWith(MockitoExtension.class)
class SorteoServiceImplTest {

    @Mock private SorteoRepository sorteoRepository;
    @Mock private ParticipanteSorteoRepository participanteSorteoRepository;
    @Mock private PerfilCreadorRepository perfilCreadorRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SeguidorRepository seguidorRepository;

    @InjectMocks
    private SorteoServiceImpl sorteoService;

    private Usuario usuarioCreador;
    private PerfilCreador perfilCreador;
    private Sorteo sorteoActivo;

    @BeforeEach
    void setUp() {
        usuarioCreador = Usuario.builder()
                .idUsuario(1L).nombres("Maria").apellidos("Lopez")
                .correo("maria@test.com").build();

        perfilCreador = PerfilCreador.builder()
                .idPerfil(10L).usuario(usuarioCreador).build();

        sorteoActivo = Sorteo.builder()
                .idSorteo(100L)
                .perfilCreador(perfilCreador)
                .tituloSorteo("Sorteo de prueba")
                .descripcionPremios("Un premio especial")
                .cantidadGanadores(2)
                .fechaInicio(LocalDateTime.now().minusHours(1))
                .fechaCierre(LocalDateTime.now().plusDays(1))
                .estadoSorteo("Activo")
                .requiereSeguidor(false)
                .build();
    }

    // =========================================================================
    // crearSorteo
    // =========================================================================

    @Test
    @DisplayName("crearSorteo — crea exitosamente con datos válidos")
    void crearSorteo_datosValidos_creaCorrectamente() {
        PeticionCrearSorteo peticion = PeticionCrearSorteo.builder()
                .tituloSorteo("Sorteo test")
                .descripcionPremios("Premio test")
                .cantidadGanadores(1)
                .fechaInicio(LocalDateTime.now().plusHours(1))
                .fechaCierre(LocalDateTime.now().plusDays(2))
                .requiereSeguidor(false)
                .build();

        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L))
                .willReturn(Optional.of(perfilCreador));
        given(sorteoRepository.save(any(Sorteo.class))).willReturn(sorteoActivo);

        RespuestaSorteo resultado = sorteoService.crearSorteo(1L, peticion);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getTituloSorteo()).isEqualTo("Sorteo de prueba");
        verify(sorteoRepository).save(any(Sorteo.class));
    }

    @Test
    @DisplayName("crearSorteo — lanza ExcepcionReglaNegocio si fechaCierre es antes de fechaInicio")
    void crearSorteo_fechaCierreAntesInicio_lanzaExcepcion() {
        PeticionCrearSorteo peticion = PeticionCrearSorteo.builder()
                .tituloSorteo("Mal sorteo")
                .descripcionPremios("Premio")
                .cantidadGanadores(1)
                .fechaInicio(LocalDateTime.now().plusDays(2))
                .fechaCierre(LocalDateTime.now().plusDays(1)) // cierre ANTES de inicio
                .build();

        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L))
                .willReturn(Optional.of(perfilCreador));

        assertThatThrownBy(() -> sorteoService.crearSorteo(1L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("posterior a la fecha de inicio");
    }

    // =========================================================================
    // participar
    // =========================================================================

    @Test
    @DisplayName("participar — inscripción exitosa en sorteo activo sin requisito de seguidor")
    void participar_sorteoActivoSinRequisito_inscribeCorrectamente() {
        Usuario usuarioParticipante = Usuario.builder()
                .idUsuario(2L).nombres("Juan").apellidos("Perez").build();

        ParticipanteSorteo participante = ParticipanteSorteo.builder()
                .idParticipacion(1L).sorteo(sorteoActivo)
                .usuario(usuarioParticipante).esGanador(false).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(100L, 2L))
                .willReturn(false);
        given(usuarioRepository.getReferenceById(2L)).willReturn(usuarioParticipante);
        given(participanteSorteoRepository.save(any(ParticipanteSorteo.class)))
                .willReturn(participante);

        RespuestaParticipante resultado = sorteoService.participar(100L, 2L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEsGanador()).isFalse();
        verify(participanteSorteoRepository).save(any(ParticipanteSorteo.class));
    }

    @Test
    @DisplayName("participar — lanza ExcepcionRecursoDuplicado si el usuario ya está inscrito")
    void participar_yaInscrito_lanzaExcepcion() {
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(100L, 2L))
                .willReturn(true);

        assertThatThrownBy(() -> sorteoService.participar(100L, 2L))
                .isInstanceOf(ExcepcionRecursoDuplicado.class)
                .hasMessageContaining("Ya estás inscrito");
    }

    @Test
    @DisplayName("participar — lanza ExcepcionReglaNegocio si el sorteo no está activo")
    void participar_sorteoFinalizado_lanzaExcepcion() {
        sorteoActivo.setEstadoSorteo("Finalizado");
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));

        assertThatThrownBy(() -> sorteoService.participar(100L, 2L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("no está activo");
    }

    @Test
    @DisplayName("participar — lanza ExcepcionReglaNegocio si requiere seguidor y no lo es")
    void participar_requiereSeguidor_noEsSeguidor_lanzaExcepcion() {
        sorteoActivo.setRequiereSeguidor(true);
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(100L, 2L))
                .willReturn(false);
        given(seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(2L, 10L))
                .willReturn(false);

        assertThatThrownBy(() -> sorteoService.participar(100L, 2L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("sigas al creador");
    }

    // =========================================================================
    // actualizarSorteo — restricciones con participantes
    // =========================================================================

    @Test
    @DisplayName("actualizarSorteo — lanza ExcepcionReglaNegocio al modificar cantidadGanadores con participantes")
    void actualizarSorteo_cambiarCantidadGanadoresConParticipantes_lanzaExcepcion() {
        var peticion = uteq.edu.ec.artisync.dto.peticion.social.PeticionActualizarSorteo.builder()
                .cantidadGanadores(5) // diferente al actual (2)
                .build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L))
                .willReturn(Optional.of(perfilCreador));
        given(participanteSorteoRepository.existsBySorteoIdSorteo(100L)).willReturn(true);

        assertThatThrownBy(() -> sorteoService.actualizarSorteo(100L, 1L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("No se puede modificar este campo");
    }

    @Test
    @DisplayName("listarSorteosActivos — retorna lista correcta")
    void listarSorteosActivos_existenSorteos_retornaLista() {
        given(sorteoRepository.findByEstadoSorteo("Activo")).willReturn(List.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(Collections.emptyList());

        List<RespuestaSorteo> resultado = sorteoService.listarSorteosActivos(null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstadoSorteo()).isEqualTo("Activo");
    }
}
