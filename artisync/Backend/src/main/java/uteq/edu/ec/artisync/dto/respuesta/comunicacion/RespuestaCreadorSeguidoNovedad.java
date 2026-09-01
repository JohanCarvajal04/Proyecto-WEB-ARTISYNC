package uteq.edu.ec.artisync.dto.respuesta.comunicacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaCreadorSeguidoNovedad {
    private Long idPerfil;
    private Long idUsuario;
    private String nombreCreador;
    private String handle;
    private String urlFotoPerfil;
    private String tituloProfesional;
    private String resumenNovedad;
    private String tipoNovedad;
    private LocalDateTime fechaNovedad;
}
