package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Configuracion especifica (duracion, prerequisitos) para una etapa del flujo.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionEtapaConfig {

    @NotBlank(message = "El nombre de la etapa es obligatorio")
    private String nombreEtapa;

    @NotNull(message = "El numero de orden es obligatorio")
    private Integer numeroOrden;

    private boolean esEtapaFinal;

    private boolean requiereEntregable;
}



