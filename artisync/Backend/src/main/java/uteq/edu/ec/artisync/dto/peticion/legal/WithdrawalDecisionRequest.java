package uteq.edu.ec.artisync.dto.peticion.legal;

import lombok.Data;

/** Cuerpo de POST .../rechazar: la nota es obligatoria (validada en el servicio, no aquí, para dar un mensaje de negocio claro). */
@Data
public class WithdrawalDecisionRequest {

    private String notaAdmin;
}
