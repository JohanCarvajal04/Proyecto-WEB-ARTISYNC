package uteq.edu.ec.artisync.dto.ia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Respuesta de la IA procesando o resumiendo resenas de usuarios.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IaResenaResponse {
    private String sentimiento;
    private boolean esCoherenteConEstrellas;
    private boolean esSpam;
    private boolean esInapropiado;
    private BigDecimal confianza;
    private String razon;
}



