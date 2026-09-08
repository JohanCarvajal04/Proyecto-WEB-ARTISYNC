package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearBriefingPlantilla;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaBriefing;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.entity.comunicacion.*;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.comunicacion.*;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.service.comunicacion.BriefingService;
import uteq.edu.ec.artisync.util.ValidadorPertenenciaPedido;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de briefing.
 * REQ-F-016 ampliado: la plantilla se asigna a un servicio y sus respuestas
 * se registran al crear el pedido (ver PedidoServicioImpl); este servicio
 * conserva la gestión de plantillas del creador y la lectura de respuestas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BriefingServiceImpl implements BriefingService {

    private static final int MAX_PREGUNTAS = 10;

    private final BriefingPlantillaRepository plantillaRepo;
    private final BriefingEnviadoRepository   enviadoRepo;
    private final BriefingRespuestaRepository respuestaRepo;
    private final PerfilCreadorRepository     perfilRepo;

    // =========================================================================
    // Gestión de plantillas (CREADOR)
    // =========================================================================

    @Override
    @Transactional
    public RespuestaBriefing crearPlantilla(Long idUsuario, PeticionCrearBriefingPlantilla peticion) {
        PerfilCreador perfil = resolverPerfilPropio(idUsuario);

        validarCantidadPreguntas(peticion.getPreguntas().size());

        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .perfilCreador(perfil)
                .nombrePlantilla(peticion.getNombrePlantilla())
                .preguntas(new ArrayList<>())
                .build();
        plantilla = plantillaRepo.save(plantilla);

        agregarPreguntas(plantilla, peticion.getPreguntas());
        plantilla = plantillaRepo.save(plantilla);

        log.info("Plantilla de briefing '{}' creada para perfil {}", peticion.getNombrePlantilla(), perfil.getIdPerfil());
        return mapPlantillaToResponse(plantilla, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaBriefing> obtenerMisPlantillas(Long idUsuario) {
        PerfilCreador perfil = resolverPerfilPropio(idUsuario);
        return plantillaRepo.findByPerfilCreadorIdPerfil(perfil.getIdPerfil())
                .stream()
                .map(p -> mapPlantillaToResponse(p, null))
                .toList();
    }

    @Override
    @Transactional
    public RespuestaBriefing editarPlantilla(Long idPlantilla, Long idUsuario,
                                             PeticionCrearBriefingPlantilla peticion) {
        PerfilCreador perfil = resolverPerfilPropio(idUsuario);
        BriefingPlantilla plantilla = plantillaRepo.findById(idPlantilla)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Plantilla no encontrada: " + idPlantilla));

        if (!plantilla.getPerfilCreador().getIdPerfil().equals(perfil.getIdPerfil())) {
            throw new ExcepcionReglaNegocio("No tienes permiso para editar esta plantilla");
        }

        validarCantidadPreguntas(peticion.getPreguntas().size());

        plantilla.setNombrePlantilla(peticion.getNombrePlantilla());
        // Reemplazar preguntas (cascade orphanRemoval las elimina)
        plantilla.getPreguntas().clear();
        plantillaRepo.flush(); // Asegura el DELETE antes del INSERT
        agregarPreguntas(plantilla, peticion.getPreguntas());
        plantilla = plantillaRepo.save(plantilla);

        log.info("Plantilla {} actualizada", idPlantilla);
        return mapPlantillaToResponse(plantilla, null);
    }

    @Override
    @Transactional
    public RespuestaMensaje eliminarPlantilla(Long idPlantilla, Long idUsuario) {
        PerfilCreador perfil = resolverPerfilPropio(idUsuario);
        BriefingPlantilla plantilla = plantillaRepo.findById(idPlantilla)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Plantilla no encontrada: " + idPlantilla));

        if (!plantilla.getPerfilCreador().getIdPerfil().equals(perfil.getIdPerfil())) {
            throw new ExcepcionReglaNegocio("No tienes permiso para eliminar esta plantilla");
        }

        plantillaRepo.delete(plantilla);
        log.info("Plantilla {} eliminada", idPlantilla);
        return new RespuestaMensaje("Plantilla eliminada correctamente");
    }

    /**
     * id_perfil y id_usuario son secuencias independientes (PerfilCreador.idPerfil
     * es su propio IDENTITY, no comparte clave con Usuario) — resolver el
     * PerfilCreador propio SIEMPRE pasa por esta búsqueda por id_usuario, nunca
     * por un findById(idUsuario) directo sobre PerfilCreadorRepository (ese fue
     * el bug: buscaba una fila de perfil con el id de usuario como si fueran
     * el mismo id, y fallaba con "Perfil creador no encontrado" para cualquier
     * cuenta cuyos ids no coincidieran por casualidad).
     */
    private PerfilCreador resolverPerfilPropio(Long idUsuario) {
        return perfilRepo.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No tienes un perfil de creador configurado"));
    }

    // =========================================================================
    // Lectura del briefing respondido (solo lectura)
    // =========================================================================
    // El envío y la respuesta ya no son endpoints propios: REQ-F-016 ampliado
    // los colapsó en PedidoServicioImpl.crearPedido (validarRespuestasBriefingCompletas
    // + registrarBriefingCompletado), porque el cuestionario ahora cuelga del
    // servicio (Servicio.briefingPlantilla) y se responde al crear el pedido.

    @Override
    @Transactional(readOnly = true)
    public RespuestaBriefing obtenerBriefing(Long idPedido, Long idUsuarioSolicitante) {
        BriefingEnviado enviado = enviadoRepo.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No existe briefing para el pedido " + idPedido));
        // Evita que cualquier autenticado lea el briefing (datos de
        // presupuesto/proyecto) de un pedido ajeno.
        ValidadorPertenenciaPedido.validarPertenenciaOAdmin(enviado.getPedido(), idUsuarioSolicitante);
        return mapEnviadoToResponse(enviado);
    }

    // =========================================================================
    // Helpers de mapeo
    // =========================================================================

    private void validarCantidadPreguntas(int cantidad) {
        if (cantidad > MAX_PREGUNTAS) {
            throw new ExcepcionReglaNegocio("Una plantilla no puede tener más de " + MAX_PREGUNTAS + " preguntas");
        }
    }

    private void agregarPreguntas(BriefingPlantilla plantilla,
                                   List<PeticionCrearBriefingPlantilla.PreguntaRequest> preguntas) {
        preguntas.forEach(p -> {
            BriefingPregunta pregunta = BriefingPregunta.builder()
                    .plantilla(plantilla)
                    .textoPregunta(p.getTextoPregunta())
                    .numeroOrden(p.getNumeroOrden())
                    .build();
            plantilla.getPreguntas().add(pregunta);
        });
    }

    private RespuestaBriefing mapPlantillaToResponse(BriefingPlantilla plantilla,
                                                      BriefingEnviado enviado) {
        List<RespuestaBriefing.PreguntaRespuestaItem> items = plantilla.getPreguntas().stream()
                .map(p -> RespuestaBriefing.PreguntaRespuestaItem.builder()
                        .idPregunta(p.getIdPregunta())
                        .textoPregunta(p.getTextoPregunta())
                        .numeroOrden(p.getNumeroOrden())
                        .textoRespuesta(null)
                        .fechaRespuesta(null)
                        .build())
                .toList();

        return RespuestaBriefing.builder()
                .idBriefingEnviado(enviado != null ? enviado.getIdBriefingEnviado() : null)
                .idPedido(enviado != null ? enviado.getPedido().getIdPedido() : null)
                .idPlantilla(plantilla.getIdBriefingPlantilla())
                .nombrePlantilla(plantilla.getNombrePlantilla())
                .fechaEnvio(enviado != null ? enviado.getFechaEnvio() : null)
                .completado(enviado != null ? enviado.getCompletado() : false)
                .preguntas(items)
                .build();
    }

    private RespuestaBriefing mapEnviadoToResponse(BriefingEnviado enviado) {
        BriefingPlantilla plantilla = enviado.getPlantilla();

        // Construir mapa de respuestas existentes por id de pregunta
        List<BriefingRespuesta> respuestas = respuestaRepo.findByBriefingEnviadoIdBriefingEnviado(
                enviado.getIdBriefingEnviado());
        Map<Long, BriefingRespuesta> respuestaMap = respuestas.stream()
                .collect(Collectors.toMap(r -> r.getPregunta().getIdPregunta(), r -> r));

        List<RespuestaBriefing.PreguntaRespuestaItem> items = plantilla.getPreguntas().stream()
                .map(p -> {
                    BriefingRespuesta r = respuestaMap.get(p.getIdPregunta());
                    return RespuestaBriefing.PreguntaRespuestaItem.builder()
                            .idPregunta(p.getIdPregunta())
                            .textoPregunta(p.getTextoPregunta())
                            .numeroOrden(p.getNumeroOrden())
                            .textoRespuesta(r != null ? r.getTextoRespuesta() : null)
                            .fechaRespuesta(r != null ? r.getFechaRespuesta() : null)
                            .build();
                })
                .toList();

        return RespuestaBriefing.builder()
                .idBriefingEnviado(enviado.getIdBriefingEnviado())
                .idPedido(enviado.getPedido().getIdPedido())
                .idPlantilla(plantilla.getIdBriefingPlantilla())
                .nombrePlantilla(plantilla.getNombrePlantilla())
                .fechaEnvio(enviado.getFechaEnvio())
                .completado(enviado.getCompletado())
                .preguntas(items)
                .build();
    }
}
