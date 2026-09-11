package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion completa de un acuerdo legal formalizado entre cliente y creador.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaContrato {

    private Long idContrato;
    private Long idPedido;
    private String tituloServicio;
    private Long idCreador;
    private String nombreCreador;
    private Long idCliente;
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



