package uteq.edu.ec.artisync.dto.ia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Validacion automatizada de documentos de identidad (OCR/KYC) via IA.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IaVerificacionResponse {
    private boolean aprobado;
    private BigDecimal confianza;
    private String tipoDocumento;
    private String nombreDetectado;
    private Boolean mayorEdad;       // solo identidad
    private String fechaNacimiento;  // solo identidad; "YYYY-MM-DD" o null
    private String paisEmision;      // solo identidad
    private String institucionEmisora; // solo certificado
    private String campoEstudio;       // solo certificado
    private String fechaEmision;       // solo certificado; "YYYY-MM-DD" o null
    private String razonRechazo;
}



