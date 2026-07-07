package uteq.edu.ec.artisync.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermisoResponse {

    private Long idPermiso;
    private String nombrePermiso;
    private String moduloAplicacion;
}
