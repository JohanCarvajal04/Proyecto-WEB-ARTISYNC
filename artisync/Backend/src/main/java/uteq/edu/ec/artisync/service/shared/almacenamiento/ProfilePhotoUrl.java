package uteq.edu.ec.artisync.service.shared.almacenamiento;

/**
 * Traduce la referencia de almacenamiento cruda (p. ej. "perfiles/uuid.jpg",
 * tal como la guarda {@link uteq.edu.ec.artisync.entity.seguridad.User#getUrlFotoPerfil()})
 * en la URL pública que sirve {@code UserController.servirFotoPerfil}.
 *
 * <p>Único punto de esta regla: antes solo vivía dentro de UserMapper, y
 * cualquier otra respuesta que quisiera mostrar la foto (p. ej. el perfil
 * público de un creador) tenía que reinventarla o quedarse sin el campo.
 */
public final class ProfilePhotoUrl {

    private ProfilePhotoUrl() {
    }

    public static String construir(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }
        return "/api/v1/usuarios/foto/" + referencia;
    }
}
