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
import uteq.edu.ec.artisync.service.shared.reporte.ColumnaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.IServicioExportacion;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
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
    private final IServicioExportacion servicioExportacion;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param datos parametro requerido para la correcta ejecucion del procedimiento
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void registrar(AuditEventData datos) {
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

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @param pageable configuracion de paginacion y ordenamiento para la capa de datos
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public PagedResponse<AuditEventSummaryResponse> listar(AuditFilter filtro, Pageable pageable) {
        Pageable seguro = paginaSegura(pageable);
        Page<AuditEvent> pagina = eventoAuditoriaRepository.findAll(especificacionDe(filtro), seguro);
        return PagedResponseBuilder.buildAndMap(pagina, this::toResumen);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idEvento identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public AuditEventResponse obtenerPorId(Long idEvento) {
        AuditEvent evento = eventoAuditoriaRepository.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el evento de auditoría con id " + idEvento));
        return toDetalle(evento);
    }

    // Auditar al auditor: exportar la propia bitácora es la operación más
    // sensible del módulo (extrae datos personales del sistema en un
    // archivo), así que queda registrada igual que cualquier otra, con el
    // formato pedido en el detalle.
    @Override
    @Transactional(readOnly = true)
    @Auditable(accion = "AUDITORIA_EXPORTAR", modulo = AuditModule.SEGURIDAD, detalle = "{formato: #formato, page: #page, size: #size}")
    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @param formato parametro requerido para la correcta ejecucion del procedimiento
     * @param page parametro requerido para la correcta ejecucion del procedimiento
     * @param size parametro requerido para la correcta ejecucion del procedimiento
     * @param correoSolicitante direccion de correo electronico del actor o usuario principal
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public DocumentoGenerado exportar(AuditFilter filtro, FormatoReporte formato, Integer page, Integer size, String correoSolicitante) {
        Page<AuditEvent> pagina;
        String titulo = "Auditoría";
        String subtitulo = "Bitácora de eventos del sistema";

        if (page != null) {
            int pageSize = (size != null && size > 0 && size <= formato.topeFilas()) ? size : formato.topeFilas();
            pagina = eventoAuditoriaRepository.findAll(
                    especificacionDe(filtro),
                    PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "fechaEvento")));
            int parte = page + 1;
            int totalPartes = Math.max(1, pagina.getTotalPages());
            titulo = "Auditoría - Parte " + parte;
            subtitulo = "Bitácora de eventos del sistema — Parte " + parte + " de " + totalPartes
                    + " (" + pagina.getTotalElements() + " eventos en total)";
        } else {
            Pageable primeraPaginaConTope =
                    PageRequest.of(0, formato.topeFilas(), Sort.by(Sort.Direction.DESC, "fechaEvento"));
            pagina = eventoAuditoriaRepository.findAll(especificacionDe(filtro), primeraPaginaConTope);

            if (pagina.getTotalElements() > formato.topeFilas()) {
                throw new BusinessRuleException(
                        "El filtro actual devuelve " + pagina.getTotalElements() + " eventos, más de los "
                                + formato.topeFilas() + " que admite una exportación en " + formato
                                + ". Acote el rango de fechas o utilice la opción de exportar por partes.");
            }
        }

        ModeloReporte<AuditEvent> modelo = ModeloReporte.<AuditEvent>builder()
                .titulo(titulo)
                .subtitulo(subtitulo)
                .filtrosAplicados(filtrosLegibles(filtro))
                .columnas(List.of(
                        ColumnaReporte.fechaHora("Fecha", AuditEvent::getFechaEvento),
                        ColumnaReporte.texto("Actor", AuditEvent::getCorreoActor),
                        ColumnaReporte.texto("Módulo", AuditEvent::getModuloAuditoria),
                        ColumnaReporte.texto("Acción", AuditEvent::getAccionAuditoria),
                        ColumnaReporte.texto("Resultado", AuditEvent::getResultadoEvento),
                        ColumnaReporte.texto("Entidad", AuditEvent::getEntidadAfectada),
                        ColumnaReporte.entero("Id. entidad", AuditEvent::getIdEntidadAfectada),
                        ColumnaReporte.texto("IP", AuditEvent::getDireccionIp),
                        ColumnaReporte.texto("Message de error", AuditEvent::getMensajeError)))
                .filas(pagina.getContent())
                .generadoPor(correoSolicitante)
                .build();

        return servicioExportacion.exportar(modelo, formato);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @param formato parametro requerido para la correcta ejecucion del procedimiento
     * @param correoSolicitante direccion de correo electronico del actor o usuario principal
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public DocumentoGenerado exportar(AuditFilter filtro, FormatoReporte formato, String correoSolicitante) {
        return exportar(filtro, formato, null, null, correoSolicitante);
    }

    private Map<String, String> filtrosLegibles(AuditFilter filtro) {
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

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<String> listarAccionesDisponibles() {
        return eventoAuditoriaRepository.listarAccionesDistintas();
    }

    private Specification<AuditEvent> especificacionDe(AuditFilter filtro) {
        return AuditEventSpecification.conFiltros(
                filtro.getCorreoActor(), filtro.getAccion(), filtro.getModulo(), filtro.getResultado(),
                filtro.getEntidad(), filtro.getIdEntidad(), filtro.getDesde(), filtro.getHasta());
    }

    /** Evita que el cliente ordene por una columna sin índice (p. ej. detalle_cambio). */
    private Pageable paginaSegura(Pageable pageable) {
        Sort ordenSeguro = pageable.getSort().stream()
                .filter(orden -> CAMPOS_ORDENABLES.contains(orden.getProperty()))
                .findFirst()
                .map(Sort::by)
                .orElse(Sort.by(Sort.Direction.DESC, "fechaEvento"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ordenSeguro);
    }

    private AuditEventSummaryResponse toResumen(AuditEvent e) {
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

    private AuditEventResponse toDetalle(AuditEvent e) {
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
