package uteq.edu.ec.artisync.service.shared.storage;

/**
 * Traduce la referencia de almacenamiento cruda (p. ej. "perfiles/uuid.jpg",
 * tal como la guarda {@code User.getUrlFotoPerfil()})
 * en la URL pública que sirve {@code UserController.serveProfilePhoto}.
 *
 * <p>Único punto de esta regla: antes solo vivía dentro de UserMapper, y
 * cualquier otra respuesta que quisiera mostrar la foto (p. ej. el perfil
 * público de un creador) tenía que reinventarla o quedarse sin el campo.
 */
public final class ProfilePhotoUrl {

    private ProfilePhotoUrl() {
    }

    /**
     * Construye la URL pública de la foto de perfil a partir de su referencia de almacenamiento.
     * @param referencia referencia cruda guardada en {@code User.getProfilePhotoUrl()}
     * @return la URL pública servida por el backend, o {@code null} si no hay foto
     */
    public static String construir(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }
        return "/api/v1/usuarios/foto/" + referencia;
    }
}
