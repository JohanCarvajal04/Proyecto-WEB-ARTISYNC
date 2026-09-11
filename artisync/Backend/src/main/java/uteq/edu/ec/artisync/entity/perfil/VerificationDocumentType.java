package uteq.edu.ec.artisync.entity.perfil;

/**
 * Entidad del modelo de dominio que representa Enumeracion de documentos KYC admitidos (CEDULA, PASAPORTE, RUC).
 * 
 * Ciclo de vida: Constante enumerada para estandarizar la captura de informacion de identidad.
 * 
 * Relaciones principales: Se persiste como string en los metadatos de validacion.
 */
public enum VerificationDocumentType {
    IDENTIDAD,
    CERTIFICADO
}


