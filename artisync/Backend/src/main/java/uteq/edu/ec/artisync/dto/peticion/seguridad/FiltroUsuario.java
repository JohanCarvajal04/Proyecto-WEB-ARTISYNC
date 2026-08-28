package uteq.edu.ec.artisync.dto.peticion.seguridad;

import lombok.Data;

/**
 * Filtros de GET /api/v1/admin/usuarios y su exportación (hallazgo 1.3,
 * INFORME-REVISION-COMPLETA.md). Se bindean como query params sin anotar
 * ({@code @ModelAttribute} implícito), igual estilo que {@code FiltroReporteContrato}.
 */
@Data
public class FiltroUsuario {

    /** Coincidencia parcial (contains, case-insensitive) contra nombres, apellidos o correo. */
    private String busqueda;

    /** Nombre exacto del rol (p. ej. "ADMIN"); se compara sin distinguir mayúsculas. */
    private String rol;

    private Boolean estadoCuenta;
}
