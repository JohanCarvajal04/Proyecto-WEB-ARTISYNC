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
import uteq.edu.ec.artisync.dto.peticion.social.UpdateRaffleRequest;
import uteq.edu.ec.artisync.dto.peticion.social.CreateRaffleRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.social.WinnerResponse;
import uteq.edu.ec.artisync.dto.respuesta.social.ParticipantResponse;
import uteq.edu.ec.artisync.dto.respuesta.social.RaffleResponse;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.entity.social.RaffleParticipant;
import uteq.edu.ec.artisync.entity.social.RafflePrize;
import uteq.edu.ec.artisync.entity.social.Raffle;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.comunicacion.FollowerRepository;
import uteq.edu.ec.artisync.repository.perfil.CreatorProfileRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.repository.social.RaffleParticipantRepository;
import uteq.edu.ec.artisync.repository.social.RaffleRepository;

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
 * Pruebas unitarias para RaffleServiceImpl.
 * RF-23: Valida reglas de negocio de sorteos — participación, restricciones de edición y creación.
 */
@ExtendWith(MockitoExtension.class)
class RaffleServiceImplTest {

    @Mock private RaffleRepository sorteoRepository;
    @Mock private RaffleParticipantRepository participanteSorteoRepository;
    @Mock private CreatorProfileRepository perfilCreadorRepository;
    @Mock private UserRepository usuarioRepository;
    @Mock private FollowerRepository seguidorRepository;

    @InjectMocks
    private RaffleServiceImpl sorteoService;

    private User usuarioCreador;
    private CreatorProfile perfilCreador;
    private Raffle sorteoActivo;

    @BeforeEach
    void setUp() {
        usuarioCreador = User.builder()
                .idUsuario(1L).nombres("Maria").apellidos("Lopez")
                .correo("maria@test.com").build();

        perfilCreador = CreatorProfile.builder()
                .idPerfil(10L).usuario(usuarioCreador).build();

        sorteoActivo = Raffle.builder()
                .idSorteo(100L)
                .perfilCreador(perfilCreador)
                .tituloSorteo("Raffle de prueba")
                .cantidadGanadores(2)
                .fechaInicio(LocalDateTime.now().minusHours(1))
                .fechaCierre(LocalDateTime.now().plusDays(1))
                .estadoSorteo("Activo")
                .requiereSeguidor(false)
                .build();
        sorteoActivo.setPremios(List.of(
                RafflePrize.builder().idPremio(1L).sorteo(sorteoActivo).descripcionPremio("Premio A").orden(1).build(),
                RafflePrize.builder().idPremio(2L).sorteo(sorteoActivo).descripcionPremio("Premio B").orden(2).build()
        ));
    }

    // =========================================================================
    // crearSorteo
    // =========================================================================

    @Test
    @DisplayName("crearSorteo — crea exitosamente con datos válidos")
    void crearSorteo_datosValidos_creaCorrectamente() {
        CreateRaffleRequest peticion = CreateRaffleRequest.builder()
                .tituloSorteo("Raffle test")
                .premios(List.of("Premio test"))
                .cantidadGanadores(1)
                .fechaInicio(LocalDateTime.now().plusHours(1))
                .fechaCierre(LocalDateTime.now().plusDays(2))
                .requiereSeguidor(false)
                .build();

        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L))
                .willReturn(Optional.of(perfilCreador));
        given(sorteoRepository.save(any(Raffle.class))).willReturn(sorteoActivo);

        RaffleResponse resultado = sorteoService.crearSorteo(1L, peticion);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getTituloSorteo()).isEqualTo("Raffle de prueba");
        verify(sorteoRepository).save(any(Raffle.class));
    }

    @Test
    @DisplayName("crearSorteo — lanza BusinessRuleException si fechaCierre es antes de fechaInicio")
    void crearSorteo_fechaCierreAntesInicio_lanzaExcepcion() {
        CreateRaffleRequest peticion = CreateRaffleRequest.builder()
                .tituloSorteo("Mal sorteo")
                .premios(List.of("Premio"))
                .cantidadGanadores(1)
                .fechaInicio(LocalDateTime.now().plusDays(2))
                .fechaCierre(LocalDateTime.now().plusDays(1)) // cierre ANTES de inicio
                .build();

        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L))
                .willReturn(Optional.of(perfilCreador));

        assertThatThrownBy(() -> sorteoService.crearSorteo(1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("posterior a la fecha de inicio");
    }

    @Test
    @DisplayName("crearSorteo — lanza BusinessRuleException si la cantidad de premios no coincide con cantidadGanadores")
    void crearSorteo_premiosNoCoincidenConCantidadGanadores_lanzaExcepcion() {
        CreateRaffleRequest peticion = CreateRaffleRequest.builder()
                .tituloSorteo("Raffle desalineado")
                .premios(List.of("Premio único"))
                .cantidadGanadores(3) // 1 premio, pero pide 3 ganadores
                .fechaInicio(LocalDateTime.now().plusHours(1))
                .fechaCierre(LocalDateTime.now().plusDays(2))
                .requiereSeguidor(false)
                .build();

        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L))
                .willReturn(Optional.of(perfilCreador));

        assertThatThrownBy(() -> sorteoService.crearSorteo(1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("debe coincidir con la cantidad de premios");
        verify(sorteoRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarSorteo — lanza BusinessRuleException si los premios enviados no coinciden con cantidadGanadores")
    void actualizarSorteo_premiosNoCoincidenConCantidadGanadores_lanzaExcepcion() {
        var peticion = UpdateRaffleRequest.builder()
                .premios(List.of("Solo un premio")) // sorteoActivo tiene cantidadGanadores=2
                .build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfilCreador));
        given(participanteSorteoRepository.existsBySorteoIdSorteo(100L)).willReturn(false);

        assertThatThrownBy(() -> sorteoService.actualizarSorteo(100L, 1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("debe coincidir con la cantidad de premios");
    }

    @Test
    @DisplayName("actualizarSorteo — rechaza cambiar los premios una vez iniciadas las inscripciones")
    void actualizarSorteo_cambiarPremiosConParticipantes_lanzaExcepcion() {
        var peticion = UpdateRaffleRequest.builder()
                .premios(List.of("Premio A", "Premio B"))
                .build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfilCreador));
        given(participanteSorteoRepository.existsBySorteoIdSorteo(100L)).willReturn(true);

        assertThatThrownBy(() -> sorteoService.actualizarSorteo(100L, 1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No se puede modificar este campo");
    }

    @Test
    @DisplayName("crearSorteo — lanza recurso no encontrado si el usuario no tiene perfil de creador")
    void crearSorteo_sinPerfilCreador_lanzaExcepcion() {
        CreateRaffleRequest peticion = CreateRaffleRequest.builder()
                .fechaInicio(LocalDateTime.now().plusHours(1))
                .fechaCierre(LocalDateTime.now().plusDays(2))
                .build();
        given(perfilCreadorRepository.findByUsuarioIdUsuario(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sorteoService.crearSorteo(99L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("actualizarSorteo — aplica la nueva fecha de cierre cuando no hay participantes")
    void actualizarSorteo_sinParticipantes_aplicaFechaCierre() {
        LocalDateTime nuevaFecha = sorteoActivo.getFechaCierre().plusDays(5);
        var peticion = UpdateRaffleRequest.builder().fechaCierre(nuevaFecha).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfilCreador));
        given(participanteSorteoRepository.existsBySorteoIdSorteo(100L)).willReturn(false);
        given(sorteoRepository.save(any(Raffle.class))).willReturn(sorteoActivo);
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of());

        sorteoService.actualizarSorteo(100L, 1L, peticion);

        assertThat(sorteoActivo.getFechaCierre()).isEqualTo(nuevaFecha);
    }

    @Test
    @DisplayName("actualizarSorteo — lanza recurso no encontrado si el usuario no tiene perfil de creador")
    void actualizarSorteo_sinPerfilCreador_lanzaExcepcion() {
        var peticion = UpdateRaffleRequest.builder().tituloSorteo("X").build();
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sorteoService.actualizarSorteo(100L, 99L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listarSorteosPorCreador — marca yoParticipo cuando el usuario actual esta inscrito")
    void listarSorteosPorCreador_conUsuarioActual_marcaParticipacion() {
        given(sorteoRepository.findByPerfilCreadorIdPerfil(10L)).willReturn(List.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of());
        given(participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(100L, 2L)).willReturn(true);

        List<RaffleResponse> resultado = sorteoService.listarSorteosPorCreador(10L, 2L);

        assertThat(resultado.get(0).isYoParticipo()).isTrue();
    }

    @Test
    @DisplayName("listarSorteosActivos — marca yoParticipo=false cuando el usuario actual no esta inscrito")
    void listarSorteosActivos_conUsuarioActual_sinParticipar() {
        given(sorteoRepository.findByEstadoSorteo("Activo")).willReturn(List.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of());
        given(participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(100L, 2L)).willReturn(false);

        List<RaffleResponse> resultado = sorteoService.listarSorteosActivos(2L);

        assertThat(resultado.get(0).isYoParticipo()).isFalse();
    }

    // =========================================================================
    // participar
    // =========================================================================

    @Test
    @DisplayName("participar — inscripción exitosa en sorteo activo sin requisito de seguidor")
    void participar_sorteoActivoSinRequisito_inscribeCorrectamente() {
        User usuarioParticipante = User.builder()
                .idUsuario(2L).nombres("Juan").apellidos("Perez").build();

        RaffleParticipant participante = RaffleParticipant.builder()
                .idParticipacion(1L).sorteo(sorteoActivo)
                .usuario(usuarioParticipante).esGanador(false).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(100L, 2L))
                .willReturn(false);
        given(usuarioRepository.getReferenceById(2L)).willReturn(usuarioParticipante);
        given(participanteSorteoRepository.save(any(RaffleParticipant.class)))
                .willReturn(participante);

        ParticipantResponse resultado = sorteoService.participar(100L, 2L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEsGanador()).isFalse();
        verify(participanteSorteoRepository).save(any(RaffleParticipant.class));
    }

    @Test
    @DisplayName("participar — lanza DuplicateResourceException si el usuario ya está inscrito")
    void participar_yaInscrito_lanzaExcepcion() {
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(100L, 2L))
                .willReturn(true);

        assertThatThrownBy(() -> sorteoService.participar(100L, 2L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Ya estás inscrito");
    }

    @Test
    @DisplayName("participar — lanza BusinessRuleException si el sorteo no está activo")
    void participar_sorteoFinalizado_lanzaExcepcion() {
        sorteoActivo.setEstadoSorteo("Finalizado");
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));

        assertThatThrownBy(() -> sorteoService.participar(100L, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no está activo");
    }

    @Test
    @DisplayName("participar — lanza BusinessRuleException si requiere seguidor y no lo es")
    void participar_requiereSeguidor_noEsSeguidor_lanzaExcepcion() {
        sorteoActivo.setRequiereSeguidor(true);
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(100L, 2L))
                .willReturn(false);
        given(seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(2L, 10L))
                .willReturn(false);

        assertThatThrownBy(() -> sorteoService.participar(100L, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("sigas al creador");
    }

    // =========================================================================
    // actualizarSorteo — restricciones con participantes
    // =========================================================================

    @Test
    @DisplayName("actualizarSorteo — lanza BusinessRuleException al modificar cantidadGanadores con participantes")
    void actualizarSorteo_cambiarCantidadGanadoresConParticipantes_lanzaExcepcion() {
        var peticion = uteq.edu.ec.artisync.dto.peticion.social.UpdateRaffleRequest.builder()
                .cantidadGanadores(5) // diferente al actual (2)
                .build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L))
                .willReturn(Optional.of(perfilCreador));
        given(participanteSorteoRepository.existsBySorteoIdSorteo(100L)).willReturn(true);

        assertThatThrownBy(() -> sorteoService.actualizarSorteo(100L, 1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No se puede modificar este campo");
    }

    @Test
    @DisplayName("listarSorteosActivos — retorna lista correcta")
    void listarSorteosActivos_existenSorteos_retornaLista() {
        given(sorteoRepository.findByEstadoSorteo("Activo")).willReturn(List.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(Collections.emptyList());

        List<RaffleResponse> resultado = sorteoService.listarSorteosActivos(null);

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
        User ganador = User.builder().idUsuario(2L).nombres("Juan").apellidos("Perez").build();
        RaffleParticipant participanteGanador = RaffleParticipant.builder()
                .idParticipacion(1L).sorteo(sorteoActivo).usuario(ganador).esGanador(true).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of(participanteGanador));
        given(participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(100L, 2L)).willReturn(true);
        given(participanteSorteoRepository.findBySorteoIdSorteoAndEsGanadorTrue(100L)).willReturn(List.of(participanteGanador));

        RaffleResponse resultado = sorteoService.obtenerSorteo(100L, 2L);

        assertThat(resultado.getGanadores()).hasSize(1);
        assertThat(resultado.isYoParticipo()).isTrue();
    }

    @Test
    @DisplayName("obtenerSorteo — cada premio queda con su propio ganador, no todos agrupados")
    void obtenerSorteo_finalizado_cadaPremioConSuGanador() {
        sorteoActivo.setEstadoSorteo("Finalizado");
        User ganador1 = User.builder().idUsuario(2L).nombres("Juan").apellidos("Perez").build();
        User ganador2 = User.builder().idUsuario(3L).nombres("Ana").apellidos("Diaz").build();
        RafflePrize premioA = sorteoActivo.getPremios().get(0);
        RafflePrize premioB = sorteoActivo.getPremios().get(1);
        RaffleParticipant p1 = RaffleParticipant.builder()
                .idParticipacion(1L).sorteo(sorteoActivo).usuario(ganador1).esGanador(true).premio(premioA).build();
        RaffleParticipant p2 = RaffleParticipant.builder()
                .idParticipacion(2L).sorteo(sorteoActivo).usuario(ganador2).esGanador(true).premio(premioB).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of(p1, p2));
        given(participanteSorteoRepository.findBySorteoIdSorteoAndEsGanadorTrue(100L)).willReturn(List.of(p1, p2));

        RaffleResponse resultado = sorteoService.obtenerSorteo(100L, null);

        assertThat(resultado.getPremios()).hasSize(2);
        assertThat(resultado.getPremios().get(0).getGanador().getNombreUsuario()).isEqualTo("Juan Perez");
        assertThat(resultado.getPremios().get(1).getGanador().getNombreUsuario()).isEqualTo("Ana Diaz");
    }

    @Test
    @DisplayName("obtenerSorteo — no incluye ganadores mientras el sorteo sigue activo")
    void obtenerSorteo_activo_sinGanadores() {
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of());

        RaffleResponse resultado = sorteoService.obtenerSorteo(100L, null);

        assertThat(resultado.getGanadores()).isNull();
        assertThat(resultado.isYoParticipo()).isFalse();
    }

    @Test
    @DisplayName("obtenerSorteo — lanza recurso no encontrado si el sorteo no existe")
    void obtenerSorteo_inexistente() {
        given(sorteoRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sorteoService.obtenerSorteo(999L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // actualizarSorteo — resto de ramas
    // =========================================================================

    @Test
    @DisplayName("actualizarSorteo — aplica cambios permitidos sin participantes")
    void actualizarSorteo_sinParticipantes_aplicaCambios() {
        var peticion = UpdateRaffleRequest.builder()
                .tituloSorteo("Nuevo titulo").cantidadGanadores(5).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfilCreador));
        given(participanteSorteoRepository.existsBySorteoIdSorteo(100L)).willReturn(false);
        given(sorteoRepository.save(any(Raffle.class))).willReturn(sorteoActivo);
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of());

        RaffleResponse resultado = sorteoService.actualizarSorteo(100L, 1L, peticion);

        assertThat(resultado.getTituloSorteo()).isEqualTo("Nuevo titulo");
        assertThat(sorteoActivo.getCantidadGanadores()).isEqualTo(5);
    }

    @Test
    @DisplayName("actualizarSorteo — rechaza cambiar fecha de cierre con participantes inscritos")
    void actualizarSorteo_cambiarFechaCierreConParticipantes_lanzaExcepcion() {
        var peticion = UpdateRaffleRequest.builder()
                .fechaCierre(sorteoActivo.getFechaCierre().plusDays(3)).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfilCreador));
        given(participanteSorteoRepository.existsBySorteoIdSorteo(100L)).willReturn(true);

        assertThatThrownBy(() -> sorteoService.actualizarSorteo(100L, 1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("fecha de cierre");
    }

    @Test
    @DisplayName("actualizarSorteo — rechaza fechaCierre anterior a la fechaInicio original, sin participantes")
    void actualizarSorteo_fechaCierreAntesDeInicio_lanzaExcepcion() {
        var peticion = UpdateRaffleRequest.builder()
                .fechaCierre(sorteoActivo.getFechaInicio().minusHours(1)) // antes de fechaInicio
                .build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfilCreador));
        given(participanteSorteoRepository.existsBySorteoIdSorteo(100L)).willReturn(false);

        assertThatThrownBy(() -> sorteoService.actualizarSorteo(100L, 1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("posterior a la fecha de inicio");
    }

    @Test
    @DisplayName("actualizarSorteo — rechaza a un usuario que no es el propietario del sorteo")
    void actualizarSorteo_rechazaNoPropietario() {
        CreatorProfile otroPerfil = CreatorProfile.builder().idPerfil(20L).build();
        var peticion = UpdateRaffleRequest.builder().tituloSorteo("x").build();

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
                .isInstanceOf(BusinessRuleException.class);
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
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("aún no ha comenzado");
    }

    @Test
    @DisplayName("participar — rechaza si el periodo de inscripcion ya cerro")
    void participar_rechazaDespuesDeCierre() {
        sorteoActivo.setFechaCierre(LocalDateTime.now().minusHours(1));
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));

        assertThatThrownBy(() -> sorteoService.participar(100L, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("finalizado");
    }

    @Test
    @DisplayName("participar — inscribe cuando requiere seguidor y el usuario ya lo es")
    void participar_requiereSeguidor_esSeguidor() {
        sorteoActivo.setRequiereSeguidor(true);
        User usuarioParticipante = User.builder().idUsuario(2L).nombres("Juan").apellidos("Perez").build();
        RaffleParticipant participante = RaffleParticipant.builder()
                .idParticipacion(1L).sorteo(sorteoActivo).usuario(usuarioParticipante).esGanador(false).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(100L, 2L)).willReturn(false);
        given(seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(2L, 10L)).willReturn(true);
        given(usuarioRepository.getReferenceById(2L)).willReturn(usuarioParticipante);
        given(participanteSorteoRepository.save(any(RaffleParticipant.class))).willReturn(participante);

        assertThat(sorteoService.participar(100L, 2L)).isNotNull();
    }

    // =========================================================================
    // cancelarParticipacion
    // =========================================================================

    @Test
    @DisplayName("cancelarParticipacion — cancela la inscripcion existente")
    void cancelarParticipacion_cancela() {
        User usuarioParticipante = User.builder().idUsuario(2L).build();
        RaffleParticipant participante = RaffleParticipant.builder()
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
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("cancelarParticipacion — lanza recurso no encontrado si no esta inscrito")
    void cancelarParticipacion_noInscrito() {
        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of());

        assertThatThrownBy(() -> sorteoService.cancelarParticipacion(100L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // listarParticipantes / listarGanadores
    // =========================================================================

    @Test
    @DisplayName("listarParticipantes — mapea los participantes del sorteo")
    void listarParticipantes_mapea() {
        User usuarioParticipante = User.builder().idUsuario(2L).nombres("Juan").apellidos("Perez").build();
        RaffleParticipant participante = RaffleParticipant.builder()
                .idParticipacion(1L).sorteo(sorteoActivo).usuario(usuarioParticipante).esGanador(false).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteo(100L)).willReturn(List.of(participante));

        assertThat(sorteoService.listarParticipantes(100L)).hasSize(1);
    }

    @Test
    @DisplayName("listarGanadores — devuelve ganadores cuando el sorteo esta finalizado")
    void listarGanadores_finalizado_devuelveLista() {
        sorteoActivo.setEstadoSorteo("Finalizado");
        User ganador = User.builder().idUsuario(2L).nombres("Juan").apellidos("Perez").build();
        RaffleParticipant participanteGanador = RaffleParticipant.builder()
                .idParticipacion(1L).sorteo(sorteoActivo).usuario(ganador).esGanador(true).build();

        given(sorteoRepository.findById(100L)).willReturn(Optional.of(sorteoActivo));
        given(participanteSorteoRepository.findBySorteoIdSorteoAndEsGanadorTrue(100L)).willReturn(List.of(participanteGanador));

        List<WinnerResponse> resultado = sorteoService.listarGanadores(100L);

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
