package uteq.edu.ec.artisync.dto.respuesta.perfil;

import lombok.Builder;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion de los datos publicos del creador (bio, especialidad, tarifas).
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Builder
public record RespuestaPerfil(
        Long idPerfil,
        Long idUsuario,
        String nombresUsuario,
        String apellidosUsuario,
        String biografia,
        String urlRedSocial,
        String urlFotoPerfil,
        // La entidad PerfilCreador ya tenÃ­a esta columna (titulo_profesional);
        // solo faltaba exponerla en la respuesta y permitir editarla.
        String tituloProfesional,
        // Identidad verificada de verdad (CertificadoIa tipo IDENTIDAD en estado
        // APROBADO), no un badge fijo mostrado igual para todos los creadores.
        boolean identidadVerificada
) {
}



