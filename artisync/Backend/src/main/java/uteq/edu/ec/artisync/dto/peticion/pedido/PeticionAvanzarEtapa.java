package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Comando para transicionar un pedido hacia su siguiente etapa en el flujo de trabajo.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionAvanzarEtapa {

    @Size(max = 2000, message = "La observaciÃ³n no puede superar los 2000 caracteres")
    private String observacion;
}



