package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload para configurar la plantilla de hitos de un servicio.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCrearFlujoTrabajo {

    @NotBlank(message = "El nombre del flujo de trabajo es obligatorio")
    private String nombreFlujo;

    private String descripcionFlujo;

    @Valid
    private List<PeticionEtapaConfig> etapas;
}



