package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.CreateBriefingTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.BriefingResponse;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.entity.comunicacion.*;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.comunicacion.*;
import uteq.edu.ec.artisync.repository.perfil.CreatorProfileRepository;
import uteq.edu.ec.artisync.service.comunicacion.BriefingService;
import uteq.edu.ec.artisync.util.OrderOwnershipValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de briefing.
 * REQ-F-016 ampliado: la plantilla se asigna a un servicio y sus respuestas
 * se registran al crear el pedido (ver OrderServiceImpl); este servicio
 * conserva la gestión de plantillas del creador y la lectura de respuestas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BriefingServiceImpl implements BriefingService {

    private static final int MAX_PREGUNTAS = 10;

    private final BriefingTemplateRepository plantillaRepo;
    private final SentBriefingRepository   enviadoRepo;
    private final BriefingAnswerRepository respuestaRepo;
    private final CreatorProfileRepository     perfilRepo;

    // =========================================================================
    // Gestión de plantillas (CREADOR)
    // =========================================================================

    @Override
    @Transactional
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public BriefingResponse createTemplate(Long idUsuario, CreateBriefingTemplateRequest peticion) {
        CreatorProfile perfil = resolveOwnProfile(idUsuario);

        validateQuestionCount(peticion.getPreguntas().size());

        BriefingTemplate plantilla = BriefingTemplate.builder()
                .perfilCreador(perfil)
                .nombrePlantilla(peticion.getNombrePlantilla())
                .preguntas(new ArrayList<>())
                .build();
        plantilla = plantillaRepo.save(plantilla);

        addQuestions(plantilla, peticion.getPreguntas());
        plantilla = plantillaRepo.save(plantilla);

        log.info("Plantilla de briefing '{}' creada para perfil {}", peticion.getNombrePlantilla(), perfil.getIdPerfil());
        return mapTemplateToResponse(plantilla, null);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<BriefingResponse> getMyTemplates(Long idUsuario) {
        CreatorProfile perfil = resolveOwnProfile(idUsuario);
        return plantillaRepo.findByPerfilCreadorIdPerfil(perfil.getIdPerfil())
                .stream()
                .map(p -> mapTemplateToResponse(p, null))
                .toList();
    }

    /**
     * Aplica modificaciones y validaciones de negocio sobre los datos de un registro existente.
     * @param idPlantilla identificador de la plantilla
     * @param idUsuario identificador del usuario
     * @param peticion datos de la peticion
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     */
    @Override
    @Transactional
    public BriefingResponse updateTemplate(Long idPlantilla, Long idUsuario,
                                             CreateBriefingTemplateRequest peticion) {
        CreatorProfile perfil = resolveOwnProfile(idUsuario);
        BriefingTemplate plantilla = plantillaRepo.findById(idPlantilla)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada: " + idPlantilla));

        if (!plantilla.getPerfilCreador().getIdPerfil().equals(perfil.getIdPerfil())) {
            throw new BusinessRuleException("No tienes permiso para editar esta plantilla");
        }

        validateQuestionCount(peticion.getPreguntas().size());

        plantilla.setNombrePlantilla(peticion.getNombrePlantilla());
        // Reemplazar preguntas (cascade orphanRemoval las elimina)
        plantilla.getPreguntas().clear();
        plantillaRepo.flush(); // Asegura el DELETE antes del INSERT
        addQuestions(plantilla, peticion.getPreguntas());
        plantilla = plantillaRepo.save(plantilla);

        log.info("Plantilla {} actualizada", idPlantilla);
        return mapTemplateToResponse(plantilla, null);
    }

    @Override
    @Transactional
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idPlantilla identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaMensaje deleteTemplate(Long idPlantilla, Long idUsuario) {
        CreatorProfile perfil = resolveOwnProfile(idUsuario);
        BriefingTemplate plantilla = plantillaRepo.findById(idPlantilla)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada: " + idPlantilla));

        if (!plantilla.getPerfilCreador().getIdPerfil().equals(perfil.getIdPerfil())) {
            throw new BusinessRuleException("No tienes permiso para eliminar esta plantilla");
        }

        plantillaRepo.delete(plantilla);
        log.info("Plantilla {} eliminada", idPlantilla);
        return new RespuestaMensaje("Plantilla eliminada correctamente");
    }

    /**
     * id_perfil y id_usuario son secuencias independientes (CreatorProfile.idPerfil
     * es su propio IDENTITY, no comparte clave con User) — resolver el
     * CreatorProfile propio SIEMPRE pasa por esta búsqueda por id_usuario, nunca
     * por un findById(idUsuario) directo sobre CreatorProfileRepository (ese fue
     * el bug: buscaba una fila de perfil con el id de usuario como si fueran
     * el mismo id, y fallaba con "Perfil creador no encontrado" para cualquier
     * cuenta cuyos ids no coincidieran por casualidad).
     */
    private CreatorProfile resolveOwnProfile(Long idUsuario) {
        return perfilRepo.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No tienes un perfil de creador configurado"));
    }

    // =========================================================================
    // Lectura del briefing respondido (solo lectura)
    // =========================================================================
    // El envío y la respuesta ya no son endpoints propios: REQ-F-016 ampliado
    // los colapsó en OrderServiceImpl.createOrder (validateBriefingAnswersComplete
    // + recordBriefingCompleted), porque el cuestionario ahora cuelga del
    // servicio (Offering.briefingPlantilla) y se responde al crear el pedido.

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idUsuarioSolicitante identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public BriefingResponse getBriefing(Long idPedido, Long idUsuarioSolicitante) {
        SentBriefing enviado = enviadoRepo.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe briefing para el pedido " + idPedido));
        // Evita que cualquier autenticado lea el briefing (datos de
        // presupuesto/proyecto) de un pedido ajeno.
        OrderOwnershipValidator.validarPertenenciaOAdmin(enviado.getPedido(), idUsuarioSolicitante);
        return mapEnviadoToResponse(enviado);
    }

    // =========================================================================
    // Helpers de mapeo
    // =========================================================================

    private void validateQuestionCount(int cantidad) {
        if (cantidad > MAX_PREGUNTAS) {
            throw new BusinessRuleException("Una plantilla no puede tener más de " + MAX_PREGUNTAS + " preguntas");
        }
    }

    private void addQuestions(BriefingTemplate plantilla,
                                   List<CreateBriefingTemplateRequest.PreguntaRequest> preguntas) {
        preguntas.forEach(p -> {
            BriefingQuestion pregunta = BriefingQuestion.builder()
                    .plantilla(plantilla)
                    .textoPregunta(p.getTextoPregunta())
                    .numeroOrden(p.getNumeroOrden())
                    .build();
            plantilla.getPreguntas().add(pregunta);
        });
    }

    private BriefingResponse mapTemplateToResponse(BriefingTemplate plantilla,
                                                      SentBriefing enviado) {
        List<BriefingResponse.PreguntaRespuestaItem> items = plantilla.getPreguntas().stream()
                .map(p -> BriefingResponse.PreguntaRespuestaItem.builder()
                        .idPregunta(p.getIdPregunta())
                        .textoPregunta(p.getTextoPregunta())
                        .numeroOrden(p.getNumeroOrden())
                        .textoRespuesta(null)
                        .fechaRespuesta(null)
                        .build())
                .toList();

        return BriefingResponse.builder()
                .idBriefingEnviado(enviado != null ? enviado.getIdBriefingEnviado() : null)
                .idPedido(enviado != null ? enviado.getPedido().getIdPedido() : null)
                .idPlantilla(plantilla.getIdBriefingPlantilla())
                .nombrePlantilla(plantilla.getNombrePlantilla())
                .fechaEnvio(enviado != null ? enviado.getFechaEnvio() : null)
                .completado(enviado != null ? enviado.getCompletado() : false)
                .preguntas(items)
                .build();
    }

    private BriefingResponse mapEnviadoToResponse(SentBriefing enviado) {
        BriefingTemplate plantilla = enviado.getPlantilla();

        // Construir mapa de respuestas existentes por id de pregunta
        List<BriefingAnswer> respuestas = respuestaRepo.findByBriefingEnviadoIdBriefingEnviado(
                enviado.getIdBriefingEnviado());
        Map<Long, BriefingAnswer> respuestaMap = respuestas.stream()
                .collect(Collectors.toMap(r -> r.getPregunta().getIdPregunta(), r -> r));

        List<BriefingResponse.PreguntaRespuestaItem> items = plantilla.getPreguntas().stream()
                .map(p -> {
                    BriefingAnswer r = respuestaMap.get(p.getIdPregunta());
                    return BriefingResponse.PreguntaRespuestaItem.builder()
                            .idPregunta(p.getIdPregunta())
                            .textoPregunta(p.getTextoPregunta())
                            .numeroOrden(p.getNumeroOrden())
                            .textoRespuesta(r != null ? r.getTextoRespuesta() : null)
                            .fechaRespuesta(r != null ? r.getFechaRespuesta() : null)
                            .build();
                })
                .toList();

        return BriefingResponse.builder()
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
