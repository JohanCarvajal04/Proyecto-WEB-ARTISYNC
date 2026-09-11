package uteq.edu.ec.artisync.repository.legal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractReportRow;

import java.time.LocalDateTime;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link ContratoCustom}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Implementa consultas con soporte para paginación dinámica y ordenamiento estructurado.
 */
public interface ContractRepositoryCustom {

    /**
     * ProyecciÃ³n para el reporte de contratos (service/legal/impl/ContractReportServiceImpl).
     * "Firmado" se deriva de que el hash de firma no sea nulo â€” igual criterio
     * que ContratoVistaComponent.yaFirme en el frontend.
     */
    Page<ContractReportRow> buscarParaReporte(LocalDateTime desde, LocalDateTime hasta,
                                                 Long idPerfilCreador, Boolean soloFirmados,
                                                 Pageable pageable);
}

