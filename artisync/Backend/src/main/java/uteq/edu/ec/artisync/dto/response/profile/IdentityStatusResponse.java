package uteq.edu.ec.artisync.dto.response.profile;

import lombok.Builder;

/**
 * Resumen de la verificación de identidad del usuario autenticado: si ya está
 * aprobada (gatea publicar servicios y crear pedidos) y, si no, en qué estado
 * quedó su última solicitud, si es que hizo alguna.
 *
 * @param verificado si la identidad ya está verificada y aprobada
 * @param estadoActual estado de la última solicitud, {@code null} si nunca solicitó verificación
 */
@Builder
public record IdentityStatusResponse(
        boolean verificado,
        String estadoActual
) {
}
