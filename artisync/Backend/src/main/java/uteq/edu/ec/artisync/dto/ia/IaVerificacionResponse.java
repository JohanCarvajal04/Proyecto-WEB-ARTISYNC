package uteq.edu.ec.artisync.dto.ia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
