package uteq.edu.ec.artisync.service.perfil;

import uteq.edu.ec.artisync.dto.peticion.perfil.CreateProfileRequest;
import uteq.edu.ec.artisync.dto.peticion.perfil.UpdateProfileRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.ProfileResponse;

import java.util.List;

public interface ICreatorProfileService {

    /**
     * Crea un perfil de creador. El {@code idUsuario} del cuerpo solo se respeta
     * si el solicitante es ADMIN; para el resto el perfil se crea siempre a
     * nombre del usuario autenticado, porque el @PreAuthorize del controlador
     * comprueba el rol pero no de quién es el recurso.
     *
     * @param peticion          datos del perfil a crear, incluyendo el usuario destino si lo crea un ADMIN
     * @param correoSolicitante correo del usuario autenticado que solicita la creación
     * @param esAdmin           si el solicitante tiene rol de administrador
     * @return el perfil recién creado
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el usuario destino ya tiene un perfil de creador
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario destino no existe
     */
    ProfileResponse createProfile(CreateProfileRequest peticion, String correoSolicitante, boolean esAdmin);

    /**
     * Obtiene un perfil de creador por su id.
     *
     * @param idPerfil id del perfil
     * @return el perfil encontrado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil no existe
     */
    ProfileResponse getProfileById(Long idPerfil);

    /**
     * Obtiene el perfil de creador asociado a un usuario.
     *
     * @param idUsuario id del usuario
     * @return el perfil del usuario
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario no tiene perfil de creador
     */
    ProfileResponse getProfileByUser(Long idUsuario);

    /**
     * Lista todos los perfiles de creador registrados.
     *
     * @return todos los perfiles
     */
    List<ProfileResponse> listProfiles();

    /**
     * Directorio público de creadores con cuenta activa (no suspendida).
     *
     * @return los perfiles con cuenta activa
     */
    List<ProfileResponse> listActiveProfiles();

    /**
     * Actualiza un perfil. Salvo que el solicitante sea ADMIN, debe ser el
     * propietario del perfil: el rol CREADOR por sí solo no autoriza a editar el
     * perfil de otro creador.
     *
     * @param idPerfil          id del perfil a actualizar
     * @param peticion          campos a modificar
     * @param correoSolicitante correo del usuario autenticado que solicita la actualización
     * @param esAdmin           si el solicitante tiene rol de administrador
     * @return el perfil ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil no existe
     * @throws org.springframework.security.access.AccessDeniedException si el solicitante no es el dueño del perfil ni administrador
     */
    ProfileResponse updateProfile(Long idPerfil, UpdateProfileRequest peticion, String correoSolicitante, boolean esAdmin);

    /**
     * Elimina un perfil de creador.
     *
     * @param idPerfil id del perfil a eliminar
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil no existe
     */
    void deleteProfile(Long idPerfil);
}
