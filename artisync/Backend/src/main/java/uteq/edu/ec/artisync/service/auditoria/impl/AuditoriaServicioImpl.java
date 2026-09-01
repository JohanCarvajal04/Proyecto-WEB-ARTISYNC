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
import uteq.edu.ec.artisync.audit.DatosEventoAuditoria;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.auditoria.FiltroAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.RespuestaEventoAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.auditoria.RespuestaEventoAuditoriaResumen;
import uteq.edu.ec.artisync.entity.auditoria.EventoAuditoria;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.auditoria.EventoAuditoriaRepository;
import uteq.edu.ec.artisync.service.auditoria.IAuditoriaServicio;
import uteq.edu.ec.artisync.service.shared.reporte.ColumnaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.IServicioExportacion;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
import uteq.edu.ec.artisync.specification.auditoria.EventoAuditoriaSpecification;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.util.PagedResponseBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaServicioImpl implements IAuditoriaServicio {

    /** Campos que el cliente puede pedir para ordenar: todos los demás caerían
     *  en un sort sin índice. Ver los índices de V15__modulo_auditoria.sql. */
    private static final Set<String> CAMPOS_ORDENABLES = Set.of("fechaEvento", "accionAuditoria", "correoActor");

    private final EventoAuditoriaRepository eventoAuditoriaRepository;
    private final IServicioExportacion servicioExportacion;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(DatosEventoAuditoria datos) {
        EventoAuditoria evento = EventoAuditoria.builder()
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
    public PagedResponse<RespuestaEventoAuditoriaResumen> listar(FiltroAuditoria filtro, Pageable pageable) {
        Pageable seguro = paginaSegura(pageable);
        Page<EventoAuditoria> pagina = eventoAuditoriaRepository.findAll(especificacionDe(filtro), seguro);
        return PagedResponseBuilder.buildAndMap(pagina, this::toResumen);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaEventoAuditoria obtenerPorId(Long idEvento) {
        EventoAuditoria evento = eventoAuditoriaRepository.findById(idEvento)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No existe el evento de auditoría con id " + idEvento));
        return toDetalle(evento);
    }

    @Override
    @Transactional(readOnly = true)
    // Auditar al auditor: exportar la propia bitácora es la operación más
    // sensible del módulo (extrae datos personales del sistema en un
    // archivo), así que queda registrada igual que cualquier otra, con el
    // formato pedido en el detalle.
    @Auditable(accion = "AUDITORIA_EXPORTAR", modulo = ModuloAuditoria.SEGURIDAD, detalle = "{formato: #formato}")
    public DocumentoGenerado exportar(FiltroAuditoria filtro, FormatoReporte formato, String correoSolicitante) {
        Pageable primeraPaginaConTope =
                PageRequest.of(0, formato.topeFilas(), Sort.by(Sort.Direction.DESC, "fechaEvento"));
        Page<EventoAuditoria> pagina = eventoAuditoriaRepository.findAll(especificacionDe(filtro), primeraPaginaConTope);

        if (pagina.getTotalElements() > formato.topeFilas()) {
            throw new ExcepcionReglaNegocio(
                    "El filtro actual devuelve " + pagina.getTotalElements() + " eventos, más de los "
                            + formato.topeFilas() + " que admite una exportación en " + formato
                            + ". Acote el rango de fechas.");
        }

        ModeloReporte<EventoAuditoria> modelo = ModeloReporte.<EventoAuditoria>builder()
                .titulo("Auditoría")
                .subtitulo("Bitácora de eventos del sistema")
                .filtrosAplicados(filtrosLegibles(filtro))
                .columnas(List.of(
                        ColumnaReporte.fechaHora("Fecha", EventoAuditoria::getFechaEvento),
                        ColumnaReporte.texto("Actor", EventoAuditoria::getCorreoActor),
                        ColumnaReporte.texto("Módulo", EventoAuditoria::getModuloAuditoria),
                        ColumnaReporte.texto("Acción", EventoAuditoria::getAccionAuditoria),
                        ColumnaReporte.texto("Resultado", EventoAuditoria::getResultadoEvento),
                        ColumnaReporte.texto("Entidad", EventoAuditoria::getEntidadAfectada),
                        ColumnaReporte.entero("Id. entidad", EventoAuditoria::getIdEntidadAfectada),
                        ColumnaReporte.texto("IP", EventoAuditoria::getDireccionIp),
                        ColumnaReporte.texto("Mensaje de error", EventoAuditoria::getMensajeError)))
                .filas(pagina.getContent())
                .generadoPor(correoSolicitante)
                .build();

        return servicioExportacion.exportar(modelo, formato);
    }

    private Map<String, String> filtrosLegibles(FiltroAuditoria filtro) {
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
    public List<String> listarAccionesDisponibles() {
        return eventoAuditoriaRepository.listarAccionesDistintas();
    }

    private Specification<EventoAuditoria> especificacionDe(FiltroAuditoria filtro) {
        return EventoAuditoriaSpecification.conFiltros(
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

    private RespuestaEventoAuditoriaResumen toResumen(EventoAuditoria e) {
        return RespuestaEventoAuditoriaResumen.builder()
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

    private RespuestaEventoAuditoria toDetalle(EventoAuditoria e) {
        return RespuestaEventoAuditoria.builder()
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
