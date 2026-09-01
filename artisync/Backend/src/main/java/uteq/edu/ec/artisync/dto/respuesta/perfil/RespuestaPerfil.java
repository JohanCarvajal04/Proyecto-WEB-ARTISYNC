package uteq.edu.ec.artisync.dto.respuesta.perfil;

import lombok.Builder;

@Builder
public record RespuestaPerfil(
        Long idPerfil,
        Long idUsuario,
        String nombresUsuario,
        String apellidosUsuario,
        String biografia,
        String urlRedSocial,
        String urlFotoPerfil,
        // La entidad PerfilCreador ya tenía esta columna (titulo_profesional);
        // solo faltaba exponerla en la respuesta y permitir editarla.
        String tituloProfesional,
        // Identidad verificada de verdad (CertificadoIa tipo IDENTIDAD en estado
        // APROBADO), no un badge fijo mostrado igual para todos los creadores.
        boolean identidadVerificada
) {
}
