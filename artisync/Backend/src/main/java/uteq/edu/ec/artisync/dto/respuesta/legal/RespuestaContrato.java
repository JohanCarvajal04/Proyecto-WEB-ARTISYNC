package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaContrato {

    private Long idContrato;
    private Long idPedido;
    private String tituloServicio;
    private String nombreCreador;
    private String nombreCliente;
    private String versionLegal;
    private String contenidoHtml;
    private String hashFirmaCreador;
    private String hashFirmaCliente;
    private Integer limiteRevisiones;
    private LocalDateTime fechaFormalizacion;
    private String urlDocumentoPdf;
    private Boolean ambasFirmasCompletas;
}
