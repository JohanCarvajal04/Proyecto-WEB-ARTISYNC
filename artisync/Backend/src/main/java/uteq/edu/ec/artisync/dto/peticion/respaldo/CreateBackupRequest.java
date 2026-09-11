package uteq.edu.ec.artisync.dto.peticion.respaldo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uteq.edu.ec.artisync.entity.respaldo.BackupType;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Comando para disparar manualmente un respaldo inmediato de la base de datos.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
public class CreateBackupRequest {

    @NotNull(message = "El tipo de respaldo es obligatorio")
    private BackupType tipoRespaldo;
}



