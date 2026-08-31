package uteq.edu.ec.artisync.dto.respuesta.perfil;

import lombok.Builder;

/**
 * Resumen de la verificación de identidad del usuario autenticado: si ya está
 * aprobada (gatea publicar servicios y crear pedidos) y, si no, en qué estado
 * quedó su última solicitud, si es que hizo alguna.
 */
@Builder
public record RespuestaEstadoIdentidad(
        boolean verificado,
        String estadoActual
) {
}
