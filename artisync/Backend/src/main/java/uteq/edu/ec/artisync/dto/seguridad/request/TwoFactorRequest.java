package uteq.edu.ec.artisync.dto.seguridad.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload de autenticacion secundaria que envia el codigo OTP (MFA) durante el login.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorRequest {

    // Â§2.1 (OBS-AUTO-05): el correo YA NO viaja en el body â€” el usuario se
    // resuelve desde el ticket pre-auth (cookie HttpOnly "preAuth2fa") emitido
    // por login() tras validar la contraseÃ±a. Aceptar un correo aquÃ­ era
    // exactamente el bypass: cualquiera que conociera un correo podÃ­a intentar
    // fuerza bruta contra este endpoint sin haber pasado por login().
    @NotBlank(message = "El cÃ³digo 2FA es obligatorio")
    private String codigo;
}



