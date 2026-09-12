package uteq.edu.ec.artisync.service.auditoria.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditEventData;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.auditoria.AuditFilter;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.AuditEventResponse;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.AuditEventSummaryResponse;
import uteq.edu.ec.artisync.entity.auditoria.AuditEvent;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.auditoria.AuditEventRepository;
import uteq.edu.ec.artisync.service.auditoria.IAuditService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportColumn;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.service.shared.reporte.IExportService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportModel;
import uteq.edu.ec.artisync.specification.auditoria.AuditEventSpecification;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.util.PagedResponseBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements IAuditService {

    /** Campos que el cliente puede pedir para ordenar: todos los demás caerían
     *  en un sort sin índice. Ver los índices de V15__modulo_auditoria.sql. */
    private static final Set<String> CAMPOS_ORDENABLES = Set.of("fechaEvento", "accionAuditoria", "correoActor");

    private final AuditEventRepository eventoAuditoriaRepository;
    private final IExportService servicioExportacion;

    /**
     * Persiste un evento de auditoría. Se llama siempre en una transacción
     * propia ({@code REQUIRES_NEW}) para que el evento sobreviva aunque la
     * operación de negocio auditada haya hecho rollback.
     *
     * @param datos datos ya resueltos del evento (actor, módulo, acción, resultado, detalle, etc.)
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEventData datos) {
        AuditEvent evento = AuditEvent.builder()
                .fechaEvento(datos.fechaEvento())
                .idUsuarioActor(datos.idUsuarioActor())
                .correoActor(datos.correoActor())
                .moduloAuditoria(datos.modulo().name())
                .accionAuditoria(datos.accion())
                .resultadoEvento(datos.resultado().name())
                .entidadAfectada(datos.entidadAfectada())
                .idEntidadAfectada(datos.idEntidadAfectada())
                .detalleCambio(datos.detalleCambio())
                .mensajeError(datos.mensajeError())
                .direccionIp(datos.direccionIp())
                .agenteUsuario(datos.agenteUsuario())
                .metodoHttp(datos.metodoHttp())
                .rutaSolicitud(datos.rutaSolicitud())
                .duracionMs(datos.duracionMs())
                .build();
        eventoAuditoriaRepository.save(evento);
    }

    /**
     * Lista el historial de auditoría filtrado, en forma resumida.
     *
     * @param filtro criterios de búsqueda y filtrado dinámico a aplicar
     * @param pageable paginación y ordenamiento solicitados; si el campo de
     *                 orden no está en {@link #CAMPOS_ORDENABLES} (sin índice),
     *                 se ignora y se ordena por fecha de evento descendente
     * @return la página de eventos resumidos que cumplen el filtro
     */
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditEventSummaryResponse> list(AuditFilter filtro, Pageable pageable) {
        Pageable seguro = safePage(pageable);
        Page<AuditEvent> pagina = eventoAuditoriaRepository.findAll(specificationFor(filtro), seguro);
        return PagedResponseBuilder.buildAndMap(pagina, this::toSummary);
    }

    /**
     * Obtiene el detalle completo de un evento de auditoría.
     *
     * @param idEvento identificador del evento
     * @return el evento con todos sus campos (incluido el detalle del cambio)
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el evento no existe
     */
    @Override
    @Transactional(readOnly = true)
    public AuditEventResponse getById(Long idEvento) {
        AuditEvent evento = eventoAuditoriaRepository.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el evento de auditoría con id " + idEvento));
        return toDetail(evento);
    }

    // Auditar al auditor: export la propia bitácora es la operación más
    // sensible del módulo (extrae datos personales del sistema en un
    // archivo), así que queda registrada igual que cualquier otra, con el
    // formato pedido en el detalle.
    /**
     * Exporta una página del historial de auditoría filtrado, en el formato solicitado.
     *
     * @param filtro criterios de búsqueda y filtrado dinámico a aplicar sobre los eventos
     * @param formato formato del documento a generar
     * @param page número de página a export (0-index); {@code null} exporta la primera página completa
     * @param size tamaño de página deseado, acotado al tope de filas del formato
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con la página de eventos de auditoría solicitada
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si {@code page} es {@code null} y el total de eventos filtrados supera el tope de filas del formato
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(accion = "AUDITORIA_EXPORTAR", modulo = AuditModule.SEGURIDAD, detalle = "{formato: #formato, page: #page, size: #size}")
    public GeneratedDocument export(AuditFilter filtro, ReportFormat formato, Integer page, Integer size, String correoSolicitante) {
        Page<AuditEvent> pagina;
        String titulo = "Auditoría";
        String subtitulo = "Bitácora de eventos del sistema";

        if (page != null) {
            int pageSize = (size != null && size > 0 && size <= formato.topeFilas()) ? size : formato.topeFilas();
            pagina = eventoAuditoriaRepository.findAll(
                    specificationFor(filtro),
                    PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "fechaEvento")));
            int parte = page + 1;
            int totalPartes = Math.max(1, pagina.getTotalPages());
            titulo = "Auditoría - Parte " + parte;
            subtitulo = "Bitácora de eventos del sistema — Parte " + parte + " de " + totalPartes
                    + " (" + pagina.getTotalElements() + " eventos en total)";
        } else {
            Pageable primeraPaginaConTope =
                    PageRequest.of(0, formato.topeFilas(), Sort.by(Sort.Direction.DESC, "fechaEvento"));
            pagina = eventoAuditoriaRepository.findAll(specificationFor(filtro), primeraPaginaConTope);

            if (pagina.getTotalElements() > formato.topeFilas()) {
                throw new BusinessRuleException(
                        "El filtro actual devuelve " + pagina.getTotalElements() + " eventos, más de los "
                                + formato.topeFilas() + " que admite una exportación en " + formato
                                + ". Acote el rango de fechas o utilice la opción de export por partes.");
            }
        }

        ReportModel<AuditEvent> modelo = ReportModel.<AuditEvent>builder()
                .titulo(titulo)
                .subtitulo(subtitulo)
                .filtrosAplicados(readableFilters(filtro))
                .columnas(List.of(
                        ReportColumn.fechaHora("Fecha", AuditEvent::getFechaEvento),
                        ReportColumn.texto("Actor", AuditEvent::getCorreoActor),
                        ReportColumn.texto("Módulo", AuditEvent::getModuloAuditoria),
                        ReportColumn.texto("Acción", AuditEvent::getAccionAuditoria),
                        ReportColumn.texto("Resultado", AuditEvent::getResultadoEvento),
                        ReportColumn.texto("Entidad", AuditEvent::getEntidadAfectada),
                        ReportColumn.entero("Id. entidad", AuditEvent::getIdEntidadAfectada),
                        ReportColumn.texto("IP", AuditEvent::getDireccionIp),
                        ReportColumn.texto("Message de error", AuditEvent::getMensajeError)))
                .filas(pagina.getContent())
                .generadoPor(correoSolicitante)
                .build();

        return servicioExportacion.exportar(modelo, formato);
    }

    /**
     * Exporta el historial de auditoría filtrado completo (sin paginar), en el formato solicitado.
     *
     * @param filtro criterios de búsqueda y filtrado dinámico a aplicar sobre los eventos
     * @param formato formato del documento a generar
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con el listado de eventos de auditoría
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el total de eventos filtrados supera el tope de filas admitido por el formato
     */
    @Override
    @Transactional(readOnly = true)
    public GeneratedDocument export(AuditFilter filtro, ReportFormat formato, String correoSolicitante) {
        return export(filtro, formato, null, null, correoSolicitante);
    }

    private Map<String, String> readableFilters(AuditFilter filtro) {
        Map<String, String> filtros = new LinkedHashMap<>();
        if (filtro.getCorreoActor() != null) {
            filtros.put("Actor", filtro.getCorreoActor());
        }
        if (filtro.getAccion() != null) {
            filtros.put("Acción", filtro.getAccion());
        }
        if (filtro.getModulo() != null) {
            filtros.put("Módulo", filtro.getModulo());
        }
        if (filtro.getResultado() != null) {
            filtros.put("Resultado", filtro.getResultado());
        }
        if (filtro.getEntidad() != null) {
            filtros.put("Entidad", filtro.getEntidad());
        }
        if (filtro.getDesde() != null) {
            filtros.put("Desde", filtro.getDesde().toString());
        }
        if (filtro.getHasta() != null) {
            filtros.put("Hasta", filtro.getHasta().toString());
        }
        return filtros;
    }

    /**
     * @return las acciones de auditoría distintas registradas, para poblar el filtro del panel
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> listAvailableActions() {
        return eventoAuditoriaRepository.listDistinctActions();
    }

    private Specification<AuditEvent> specificationFor(AuditFilter filtro) {
        return AuditEventSpecification.conFiltros(
                filtro.getCorreoActor(), filtro.getAccion(), filtro.getModulo(), filtro.getResultado(),
                filtro.getEntidad(), filtro.getIdEntidad(), filtro.getDesde(), filtro.getHasta());
    }

    /** Evita que el cliente ordene por una columna sin índice (p. ej. detalle_cambio). */
    private Pageable safePage(Pageable pageable) {
        Sort ordenSeguro = pageable.getSort().stream()
                .filter(orden -> CAMPOS_ORDENABLES.contains(orden.getProperty()))
                .findFirst()
                .map(Sort::by)
                .orElse(Sort.by(Sort.Direction.DESC, "fechaEvento"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ordenSeguro);
    }

    private AuditEventSummaryResponse toSummary(AuditEvent e) {
        return AuditEventSummaryResponse.builder()
                .idEventoAuditoria(e.getIdEventoAuditoria())
                .fechaEvento(e.getFechaEvento())
                .idUsuarioActor(e.getIdUsuarioActor())
                .correoActor(e.getCorreoActor())
                .moduloAuditoria(e.getModuloAuditoria())
                .accionAuditoria(e.getAccionAuditoria())
                .resultadoEvento(e.getResultadoEvento())
                .entidadAfectada(e.getEntidadAfectada())
                .idEntidadAfectada(e.getIdEntidadAfectada())
                .direccionIp(e.getDireccionIp())
                .build();
    }

    private AuditEventResponse toDetail(AuditEvent e) {
        return AuditEventResponse.builder()
                .idEventoAuditoria(e.getIdEventoAuditoria())
                .fechaEvento(e.getFechaEvento())
                .idUsuarioActor(e.getIdUsuarioActor())
                .correoActor(e.getCorreoActor())
                .moduloAuditoria(e.getModuloAuditoria())
                .accionAuditoria(e.getAccionAuditoria())
                .resultadoEvento(e.getResultadoEvento())
                .entidadAfectada(e.getEntidadAfectada())
                .idEntidadAfectada(e.getIdEntidadAfectada())
                .detalleCambio(e.getDetalleCambio())
                .mensajeError(e.getMensajeError())
                .direccionIp(e.getDireccionIp())
                .agenteUsuario(e.getAgenteUsuario())
                .metodoHttp(e.getMetodoHttp())
                .rutaSolicitud(e.getRutaSolicitud())
                .duracionMs(e.getDuracionMs())
                .build();
    }
}
