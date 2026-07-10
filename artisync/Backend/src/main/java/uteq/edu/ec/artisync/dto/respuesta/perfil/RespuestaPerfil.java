package uteq.edu.ec.artisync.dto.respuesta.perfil;

import lombok.Builder;

@Builder
public record RespuestaPerfil(
        Long idPerfil,
        Long idUsuario,
        String nombresUsuario,
        String apellidosUsuario,
        String biografia,
        String urlRedSocial
) {
}
