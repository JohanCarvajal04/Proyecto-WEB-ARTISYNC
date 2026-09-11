package uteq.edu.ec.artisync.dto.respuesta.respaldo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uteq.edu.ec.artisync.entity.respaldo.EstadoRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.OrigenRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;

import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion de los metadatos de un backup ejecutado (tamano, fecha, estado).
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaRespaldo {

    private Long idRespaldo;
    private TipoRespaldo tipoRespaldo;
    private EstadoRespaldo estadoRespaldo;
    private OrigenRespaldo origen;
    private Long idProgramacion;
    private Long idRespaldoFullBase;
    private String nombreArchivo;
    private Long tamanoBytes;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Integer duracionMs;
    private String mensajeError;
    private String correoSolicitante;
}



