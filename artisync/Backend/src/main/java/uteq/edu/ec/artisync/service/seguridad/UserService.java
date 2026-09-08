package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import uteq.edu.ec.artisync.dto.seguridad.request.ChangePasswordRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateUserRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;

public interface UserService {

    /**
     * Obtiene los datos del usuario autenticado.
     *
     * @param correo correo del usuario autenticado
     * @return los datos del usuario
     * @throws org.springframework.web.server.ResponseStatusException {@code NOT_FOUND} si el usuario no existe
     */
    UserResponse getCurrentUser(String correo);

    /**
     * Actualiza los datos de perfil del usuario autenticado.
     *
     * @param correo  correo del usuario autenticado
     * @param request campos a actualizar, incluyendo opcionalmente el país
     * @return los datos del usuario ya actualizados
     * @throws org.springframework.web.server.ResponseStatusException {@code NOT_FOUND} si el usuario no existe, o si {@code idPais} no corresponde a un país existente
     */
    UserResponse updateCurrentUser(String correo, UpdateUserRequest request);

    /**
     * Cambia la contraseña del usuario autenticado, validando la contraseña actual.
     *
     * @param correo  correo del usuario autenticado
     * @param request contraseña actual y nueva contraseña
     * @return mensaje de confirmación
     * @throws org.springframework.web.server.ResponseStatusException {@code NOT_FOUND} si el usuario no existe;
     *         {@code BAD_REQUEST} si la contraseña actual es incorrecta
     */
    RespuestaMensaje changePassword(String correo, ChangePasswordRequest request);

    /**
     * Elimina la cuenta del usuario autenticado.
     *
     * @param correo correo del usuario autenticado
     * @return mensaje de confirmación
     * @throws org.springframework.web.server.ResponseStatusException {@code NOT_FOUND} si el usuario no existe
     */
    RespuestaMensaje deleteOwnAccount(String correo);

    /**
     * Revoca todas las sesiones activas del usuario autenticado, en todos los dispositivos.
     *
     * @param correo correo del usuario autenticado
     * @return mensaje de confirmación
     * @throws org.springframework.web.server.ResponseStatusException {@code NOT_FOUND} si el usuario no existe
     */
    RespuestaMensaje revokeAllMySessions(String correo);

    /**
     * Sube o reemplaza la foto de perfil del usuario autenticado.
     *
     * @param correo correo del usuario autenticado
     * @param file   archivo de imagen a almacenar
     * @return los datos del usuario con la URL de foto ya actualizada
     * @throws org.springframework.web.server.ResponseStatusException {@code NOT_FOUND} si el usuario no existe
     */
    UserResponse uploadProfilePicture(String correo, org.springframework.web.multipart.MultipartFile file);
}

