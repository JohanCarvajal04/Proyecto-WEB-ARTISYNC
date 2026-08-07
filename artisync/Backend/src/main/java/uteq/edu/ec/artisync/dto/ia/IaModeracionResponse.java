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
public class IaModeracionResponse {
    private boolean esApropiado;
    private String categoriaInfraccion;
    private BigDecimal confianza;
    private String razon;
}
