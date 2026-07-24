package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearBriefingPlantilla;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionEnviarBriefing;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionResponderBriefing;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaBriefing;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.entity.comunicacion.*;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.comunicacion.*;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.service.comunicacion.BriefingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de briefing.
 * RF-16: Formulario interactivo de hasta 10 preguntas; respuestas inmutables.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BriefingServiceImpl implements BriefingService {

    private static final int MAX_PREGUNTAS = 10;

    private final BriefingPlantillaRepository plantillaRepo;
    private final BriefingPreguntaRepository  preguntaRepo;
    private final BriefingEnviadoRepository   enviadoRepo;
    private final BriefingRespuestaRepository respuestaRepo;
    private final PerfilCreadorRepository     perfilRepo;
    private final PedidoRepository            pedidoRepo;

    // =========================================================================
    // Gestión de plantillas (CREADOR)
    // =========================================================================

    @Override
    @Transactional
    public RespuestaBriefing crearPlantilla(Long idPerfilCreador, PeticionCrearBriefingPlantilla peticion) {
        PerfilCreador perfil = perfilRepo.findById(idPerfilCreador)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Perfil creador no encontrado: " + idPerfilCreador));

        validarCantidadPreguntas(peticion.getPreguntas().size());

        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .perfilCreador(perfil)
                .nombrePlantilla(peticion.getNombrePlantilla())
                .preguntas(new ArrayList<>())
                .build();
        plantilla = plantillaRepo.save(plantilla);

        agregarPreguntas(plantilla, peticion.getPreguntas());
        plantilla = plantillaRepo.save(plantilla);

        log.info("Plantilla de briefing '{}' creada para perfil {}", peticion.getNombrePlantilla(), idPerfilCreador);
        return mapPlantillaToResponse(plantilla, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaBriefing> obtenerMisPlantillas(Long idPerfilCreador) {
        return plantillaRepo.findByPerfilCreadorIdPerfil(idPerfilCreador)
                .stream()
                .map(p -> mapPlantillaToResponse(p, null))
                .toList();
    }

    @Override
    @Transactional
    public RespuestaBriefing editarPlantilla(Long idPlantilla, Long idPerfilCreador,
                                             PeticionCrearBriefingPlantilla peticion) {
        BriefingPlantilla plantilla = plantillaRepo.findById(idPlantilla)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Plantilla no encontrada: " + idPlantilla));

        if (!plantilla.getPerfilCreador().getIdPerfil().equals(idPerfilCreador)) {
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
    public RespuestaMensaje eliminarPlantilla(Long idPlantilla, Long idPerfilCreador) {
        BriefingPlantilla plantilla = plantillaRepo.findById(idPlantilla)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Plantilla no encontrada: " + idPlantilla));

        if (!plantilla.getPerfilCreador().getIdPerfil().equals(idPerfilCreador)) {
            throw new ExcepcionReglaNegocio("No tienes permiso para eliminar esta plantilla");
        }

        plantillaRepo.delete(plantilla);
        log.info("Plantilla {} eliminada", idPlantilla);
        return new RespuestaMensaje("Plantilla eliminada correctamente");
    }

    // =========================================================================
    // Envío y respuesta de briefing
    // =========================================================================

    @Override
    @Transactional
    public RespuestaBriefing enviarBriefing(Long idPedido, PeticionEnviarBriefing peticion, Long idCreador) {
        if (enviadoRepo.existsByPedidoIdPedido(idPedido)) {
            throw new ExcepcionReglaNegocio("Ya se envió un briefing para este pedido");
        }

        Pedido pedido = pedidoRepo.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado: " + idPedido));

        BriefingPlantilla plantilla = plantillaRepo.findById(peticion.getIdBriefingPlantilla())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Plantilla no encontrada: " + peticion.getIdBriefingPlantilla()));

        // Verificar que la plantilla pertenece al creador del servicio
        if (!plantilla.getPerfilCreador().getUsuario().getIdUsuario().equals(idCreador)) {
            throw new ExcepcionReglaNegocio("No tienes permiso para usar esta plantilla");
        }

        BriefingEnviado enviado = BriefingEnviado.builder()
                .pedido(pedido)
                .plantilla(plantilla)
                .completado(false)
                .build();
        enviado = enviadoRepo.save(enviado);

        log.info("Briefing enviado al pedido {} con plantilla '{}'", idPedido, plantilla.getNombrePlantilla());
        return mapEnviadoToResponse(enviado);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaBriefing obtenerBriefing(Long idPedido) {
        BriefingEnviado enviado = enviadoRepo.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No existe briefing para el pedido " + idPedido));
        return mapEnviadoToResponse(enviado);
    }

    @Override
    @Transactional
    public RespuestaBriefing responderBriefing(Long idPedido, PeticionResponderBriefing peticion, Long idCliente) {
        BriefingEnviado enviado = enviadoRepo.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No existe briefing para el pedido " + idPedido));

        // RF-16: Inmutabilidad — no se puede responder dos veces
        if (Boolean.TRUE.equals(enviado.getCompletado())) {
            throw new ExcepcionReglaNegocio(
                    "Las respuestas del briefing no pueden modificarse una vez enviadas");
        }

        // Verificar que el cliente es el propietario del pedido
        if (!enviado.getPedido().getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new ExcepcionReglaNegocio("No tienes permiso para responder este briefing");
        }

        // Persistir cada respuesta
        for (PeticionResponderBriefing.RespuestaItem item : peticion.getRespuestas()) {
            BriefingPregunta pregunta = preguntaRepo.findById(item.getIdPregunta())
                    .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                            "Pregunta no encontrada: " + item.getIdPregunta()));

            BriefingRespuesta respuesta = BriefingRespuesta.builder()
                    .briefingEnviado(enviado)
                    .pregunta(pregunta)
                    .textoRespuesta(item.getTextoRespuesta())
                    .build();
            respuestaRepo.save(respuesta);
        }

        enviado.setCompletado(true);
        enviado = enviadoRepo.save(enviado);

        log.info("Briefing del pedido {} completado por el cliente {}", idPedido, idCliente);

        // TODO: Llamar a contratoService.generarContrato(idPedido) cuando M5 esté listo (RF-17)

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
