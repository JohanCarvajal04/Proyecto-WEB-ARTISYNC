package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** REQ-NF-020: resultado de recalcular el hash de contenido de un contrato y compararlo con el guardado. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrityVerificationResponse {

    private Long idContrato;
    private boolean integro;
    private String hashAlmacenado;
    private String hashRecalculado;
    private LocalDateTime fechaHashOriginal;
    private LocalDateTime fechaVerificacion;
}
