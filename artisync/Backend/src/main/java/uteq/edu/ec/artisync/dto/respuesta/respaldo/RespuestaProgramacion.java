package uteq.edu.ec.artisync.dto.respuesta.respaldo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;

import java.time.LocalDateTime;

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
