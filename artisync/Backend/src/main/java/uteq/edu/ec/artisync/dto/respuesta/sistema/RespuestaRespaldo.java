package uteq.edu.ec.artisync.dto.respuesta.sistema;

public record RespuestaRespaldo(
    Long idRespaldo,
    String nombreArchivo,
    String tipo,
    String categoria,
    long tamanoBytes,
    String tamanoFormateado,
    String estado,
    String mensajeError,
    String creadoPor,
    String fechaCreacion,
    String fechaExpiracion,
    String hashSha256
) {}
