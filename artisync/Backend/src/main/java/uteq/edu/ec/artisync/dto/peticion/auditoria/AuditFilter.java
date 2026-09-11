package uteq.edu.ec.artisync.dto.peticion.auditoria;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uteq.edu.ec.artisync.dto.peticion.comun.FiltroRangoFechas;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Parametros de busqueda para filtrar los eventos de la bitacora de auditoria.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditFilter extends FiltroRangoFechas {

    private String correoActor;
    private String accion;
    private String modulo;
    private String resultado;
    private String entidad;
    private Long idEntidad;
}


