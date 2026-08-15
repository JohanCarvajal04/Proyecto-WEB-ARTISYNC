package uteq.edu.ec.artisync.dto.ia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IaClasificacionResponse {
    private String categoriaSugerida;
    private String subcategoriaSugerida;
    private List<String> etiquetasSugeridas;
    private BigDecimal confianza;
}
