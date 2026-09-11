package uteq.edu.ec.artisync.dto.respuesta.comunicacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Notificacion de actualizacion o nueva obra de un creador seguido.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaCreadorSeguidoNovedad {
    private Long idPerfil;
    private Long idUsuario;
    private String nombreCreador;
    private String handle;
    private String urlFotoPerfil;
    private String tituloProfesional;
    private String resumenNovedad;
    private String tipoNovedad;
    private LocalDateTime fechaNovedad;
}



