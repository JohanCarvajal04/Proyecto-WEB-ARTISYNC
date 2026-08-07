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
public class IaResenaResponse {
    private String sentimiento;
    private boolean esCoherenteConEstrellas;
    private boolean esSpam;
    private boolean esInapropiado;
    private BigDecimal confianza;
    private String razon;
}
