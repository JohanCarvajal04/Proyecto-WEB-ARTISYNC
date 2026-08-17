package uteq.edu.ec.artisync.service.social.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionActualizarSorteo;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearSorteo;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaGanador;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaParticipante;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaSorteo;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
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
import static org.mockito.Mockito.never;
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

    // =========================================================================
    // obtenerSorteo
    // =========================================================================

    @Test
    @DisplayName("obtenerSorteo — incluye ganadores cuando el sorteo esta finalizado")
    void obtenerSorteo_finalizado_incluyeGanadores() {
        sorteoActivo.setEstadoSorteo("Finalizado");
        Usuario ganador = Usuario.builder().idUsuario(2L).nombres("Juan").apellidos("Perez").build();
        ParticipanteSorteo participanteGanador = ParticipanteSorteo.builder()
                .idParticipacion(1L).sorteo(sorteoActivo).usuario(ganador).esGanador(true).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of(participanteGanador));
        given(participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(100L, 2L)).willReturn(true);
        given(participanteSorteoRepository.findBySorteoIdSorteoAndEsGanadorTrue(100L)).willReturn(List.of(participanteGanador));

        RespuestaSorteo resultado = sorteoService.obtenerSorteo(100L, 2L);

        assertThat(resultado.getGanadores()).hasSize(1);
        assertThat(resultado.isYoParticipo()).isTrue();
    }

    @Test
    @DisplayName("obtenerSorteo — no incluye ganadores mientras el sorteo sigue activo")
    void obtenerSorteo_activo_sinGanadores() {
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of());

        RespuestaSorteo resultado = sorteoService.obtenerSorteo(100L, null);

        assertThat(resultado.getGanadores()).isNull();
        assertThat(resultado.isYoParticipo()).isFalse();
    }

    @Test
    @DisplayName("obtenerSorteo — lanza recurso no encontrado si el sorteo no existe")
    void obtenerSorteo_inexistente() {
        given(sorteoRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sorteoService.obtenerSorteo(999L, null))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    // =========================================================================
    // actualizarSorteo — resto de ramas
    // =========================================================================

    @Test
    @DisplayName("actualizarSorteo — aplica cambios permitidos sin participantes")
    void actualizarSorteo_sinParticipantes_aplicaCambios() {
        var peticion = PeticionActualizarSorteo.builder()
                .tituloSorteo("Nuevo titulo").cantidadGanadores(5).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfilCreador));
        given(participanteSorteoRepository.existsBySorteoIdSorteo(100L)).willReturn(false);
        given(sorteoRepository.save(any(Sorteo.class))).willReturn(sorteoActivo);
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of());

        RespuestaSorteo resultado = sorteoService.actualizarSorteo(100L, 1L, peticion);

        assertThat(resultado.getTituloSorteo()).isEqualTo("Nuevo titulo");
        assertThat(sorteoActivo.getCantidadGanadores()).isEqualTo(5);
    }

    @Test
    @DisplayName("actualizarSorteo — rechaza cambiar fecha de cierre con participantes inscritos")
    void actualizarSorteo_cambiarFechaCierreConParticipantes_lanzaExcepcion() {
        var peticion = PeticionActualizarSorteo.builder()
                .fechaCierre(sorteoActivo.getFechaCierre().plusDays(3)).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfilCreador));
        given(participanteSorteoRepository.existsBySorteoIdSorteo(100L)).willReturn(true);

        assertThatThrownBy(() -> sorteoService.actualizarSorteo(100L, 1L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("fecha de cierre");
    }

    @Test
    @DisplayName("actualizarSorteo — rechaza a un usuario que no es el propietario del sorteo")
    void actualizarSorteo_rechazaNoPropietario() {
        PerfilCreador otroPerfil = PerfilCreador.builder().idPerfil(20L).build();
        var peticion = PeticionActualizarSorteo.builder().tituloSorteo("x").build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(99L)).willReturn(Optional.of(otroPerfil));

        assertThatThrownBy(() -> sorteoService.actualizarSorteo(100L, 99L, peticion))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // eliminarSorteo
    // =========================================================================

    @Test
    @DisplayName("eliminarSorteo — elimina cuando no tiene participantes")
    void eliminarSorteo_sinParticipantes_elimina() {
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfilCreador));
        given(participanteSorteoRepository.existsBySorteoIdSorteo(100L)).willReturn(false);

        RespuestaMensaje respuesta = sorteoService.eliminarSorteo(100L, 1L);

        assertThat(respuesta).isNotNull();
        verify(sorteoRepository).delete(sorteoActivo);
    }

    @Test
    @DisplayName("eliminarSorteo — rechaza si tiene participantes inscritos")
    void eliminarSorteo_conParticipantes_rechaza() {
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfilCreador));
        given(participanteSorteoRepository.existsBySorteoIdSorteo(100L)).willReturn(true);

        assertThatThrownBy(() -> sorteoService.eliminarSorteo(100L, 1L))
                .isInstanceOf(ExcepcionReglaNegocio.class);
        verify(sorteoRepository, never()).delete(any());
    }

    // =========================================================================
    // listarSorteosPorCreador
    // =========================================================================

    @Test
    @DisplayName("listarSorteosPorCreador — mapea los sorteos del perfil")
    void listarSorteosPorCreador_mapea() {
        given(sorteoRepository.findByPerfilCreadorIdPerfil(10L)).willReturn(List.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of());

        assertThat(sorteoService.listarSorteosPorCreador(10L, null)).hasSize(1);
    }

    // =========================================================================
    // participar — ramas adicionales
    // =========================================================================

    @Test
    @DisplayName("participar — rechaza si el sorteo aun no ha comenzado")
    void participar_rechazaAntesDeInicio() {
        sorteoActivo.setFechaInicio(LocalDateTime.now().plusDays(1));
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));

        assertThatThrownBy(() -> sorteoService.participar(100L, 2L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("aún no ha comenzado");
    }

    @Test
    @DisplayName("participar — rechaza si el periodo de inscripcion ya cerro")
    void participar_rechazaDespuesDeCierre() {
        sorteoActivo.setFechaCierre(LocalDateTime.now().minusHours(1));
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));

        assertThatThrownBy(() -> sorteoService.participar(100L, 2L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("finalizado");
    }

    @Test
    @DisplayName("participar — inscribe cuando requiere seguidor y el usuario ya lo es")
    void participar_requiereSeguidor_esSeguidor() {
        sorteoActivo.setRequiereSeguidor(true);
        Usuario usuarioParticipante = Usuario.builder().idUsuario(2L).nombres("Juan").apellidos("Perez").build();
        ParticipanteSorteo participante = ParticipanteSorteo.builder()
                .idParticipacion(1L).sorteo(sorteoActivo).usuario(usuarioParticipante).esGanador(false).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(100L, 2L)).willReturn(false);
        given(seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(2L, 10L)).willReturn(true);
        given(usuarioRepository.getReferenceById(2L)).willReturn(usuarioParticipante);
        given(participanteSorteoRepository.save(any(ParticipanteSorteo.class))).willReturn(participante);

        assertThat(sorteoService.participar(100L, 2L)).isNotNull();
    }

    // =========================================================================
    // cancelarParticipacion
    // =========================================================================

    @Test
    @DisplayName("cancelarParticipacion — cancela la inscripcion existente")
    void cancelarParticipacion_cancela() {
        Usuario usuarioParticipante = Usuario.builder().idUsuario(2L).build();
        ParticipanteSorteo participante = ParticipanteSorteo.builder()
                .idParticipacion(1L).sorteo(sorteoActivo).usuario(usuarioParticipante).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of(participante));

        RespuestaMensaje respuesta = sorteoService.cancelarParticipacion(100L, 2L);

        assertThat(respuesta).isNotNull();
        verify(participanteSorteoRepository).delete(participante);
    }

    @Test
    @DisplayName("cancelarParticipacion — rechaza si el sorteo ya finalizo")
    void cancelarParticipacion_rechazaSorteoFinalizado() {
        sorteoActivo.setEstadoSorteo("Finalizado");
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));

        assertThatThrownBy(() -> sorteoService.cancelarParticipacion(100L, 2L))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("cancelarParticipacion — lanza recurso no encontrado si no esta inscrito")
    void cancelarParticipacion_noInscrito() {
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of());

        assertThatThrownBy(() -> sorteoService.cancelarParticipacion(100L, 2L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    // =========================================================================
    // listarParticipantes / listarGanadores
    // =========================================================================

    @Test
    @DisplayName("listarParticipantes — mapea los participantes del sorteo")
    void listarParticipantes_mapea() {
        Usuario usuarioParticipante = Usuario.builder().idUsuario(2L).nombres("Juan").apellidos("Perez").build();
        ParticipanteSorteo participante = ParticipanteSorteo.builder()
                .idParticipacion(1L).sorteo(sorteoActivo).usuario(usuarioParticipante).esGanador(false).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of(participante));

        assertThat(sorteoService.listarParticipantes(100L)).hasSize(1);
    }

    @Test
    @DisplayName("listarGanadores — devuelve ganadores cuando el sorteo esta finalizado")
    void listarGanadores_finalizado_devuelveLista() {
        sorteoActivo.setEstadoSorteo("Finalizado");
        Usuario ganador = Usuario.builder().idUsuario(2L).nombres("Juan").apellidos("Perez").build();
        ParticipanteSorteo participanteGanador = ParticipanteSorteo.builder()
                .idParticipacion(1L).sorteo(sorteoActivo).usuario(ganador).esGanador(true).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteoAndEsGanadorTrue(100L)).willReturn(List.of(participanteGanador));

        List<RespuestaGanador> resultado = sorteoService.listarGanadores(100L);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("listarGanadores — rechaza si el sorteo aun no ha finalizado")
    void listarGanadores_rechazaSorteoActivo() {
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));

        assertThatThrownBy(() -> sorteoService.listarGanadores(100L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.CONFLICT);
    }
}
