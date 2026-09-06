package uteq.edu.ec.artisync.dto.peticion.perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PeticionDatosPago {

    @NotBlank(message = "El correo de PayPal es obligatorio")
    @Email(message = "El correo de PayPal no tiene un formato válido")
    @Size(max = 150, message = "El correo de PayPal no puede superar los 150 caracteres")
    private String correoPaypal;
}
