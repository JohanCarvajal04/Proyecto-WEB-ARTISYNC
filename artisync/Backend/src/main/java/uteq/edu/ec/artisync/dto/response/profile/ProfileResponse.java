package uteq.edu.ec.artisync.dto.response.profile;

import lombok.Builder;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion de los datos publicos del creador (bio, especialidad, tarifas).
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 *
 * @param idPerfil id del perfil de creador
 * @param idUsuario id del usuario dueño del perfil
 * @param nombresUsuario nombres del usuario
 * @param apellidosUsuario apellidos del usuario
 * @param biografia biografía pública del perfil
 * @param urlRedSocial URL de una red social del creador
 * @param urlFotoPerfil URL de la foto de perfil
 * @param tituloProfesional título profesional que se muestra en el perfil
 * @param identidadVerificada si el creador tiene un certificado de identidad aprobado
 */
@Builder
public record ProfileResponse(
        Long idPerfil,
        Long idUsuario,
        String nombresUsuario,
        String apellidosUsuario,
        String biografia,
        String urlRedSocial,
        String urlFotoPerfil,
        // La entidad CreatorProfile ya tenía esta columna (titulo_profesional);
        // solo faltaba exponerla en la respuesta y permitir editarla.
        String tituloProfesional,
        // Identidad verificada de verdad (AiCertificate tipo IDENTIDAD en estado
        // APROBADO), no un badge fijo mostrado igual para todos los creadores.
        boolean identidadVerificada
) {
}



