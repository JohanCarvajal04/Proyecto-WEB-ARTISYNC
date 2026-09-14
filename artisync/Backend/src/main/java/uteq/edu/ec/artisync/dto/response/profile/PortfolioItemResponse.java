package uteq.edu.ec.artisync.dto.response.profile;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * urlArchivo no es la referencia que se persiste, sino algo que el navegador
 * puede pedir: un SAS firmado con Azure, o la ruta del endpoint que sirve los
 * bytes cuando el proveedor no sabe firmar URLs.
 *
 * @param idItemPortafolio id del ítem de portafolio
 * @param idPortafolio id del portafolio al que pertenece
 * @param tituloObra título de la obra
 * @param descripcionObra descripción de la obra
 * @param urlArchivo URL desde la que el navegador puede pedir el archivo
 * @param fechaSubida fecha en que se subió la obra
 */
@Builder
public record PortfolioItemResponse(
        Long idItemPortafolio,
        Long idPortafolio,
        String tituloObra,
        String descripcionObra,
        String urlArchivo,
        LocalDateTime fechaSubida
) {
}
