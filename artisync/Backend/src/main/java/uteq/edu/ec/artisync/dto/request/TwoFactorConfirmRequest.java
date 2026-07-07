package uteq.edu.ec.artisync.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorConfirmRequest {

    @NotBlank(message = "El código TOTP es obligatorio")
    private String codigo;
}
