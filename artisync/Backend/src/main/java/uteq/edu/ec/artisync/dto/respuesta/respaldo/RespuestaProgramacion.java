package uteq.edu.ec.artisync.dto.respuesta.respaldo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;

import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion de la configuracion de un job de respaldo automatizado (CRON).
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaProgramacion {

    private Long idProgramacion;
    private String nombre;
    private TipoRespaldo tipoRespaldo;
    private String expresionCron;
    private Integer retencionDias;
    private Boolean activo;
    private LocalDateTime proximaEjecucion;
    private LocalDateTime ultimaEjecucion;
    private String creadoPor;
    private LocalDateTime fechaCreacion;
}



