package uteq.edu.ec.artisync.dto.respuesta.perfil;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * urlArchivo no es la referencia que se persiste, sino algo que el navegador
 * puede pedir: un SAS firmado con Azure, o la ruta del endpoint que sirve los
 * bytes cuando el proveedor no sabe firmar URLs.
 */
@Builder
public record RespuestaPortafolioItem(
        Long idItemPortafolio,
        Long idPortafolio,
        String tituloObra,
        String descripcionObra,
        String urlArchivo,
        LocalDateTime fechaSubida
) {
}
