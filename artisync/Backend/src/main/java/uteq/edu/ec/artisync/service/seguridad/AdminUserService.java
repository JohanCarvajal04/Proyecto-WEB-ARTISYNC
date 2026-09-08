package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.peticion.seguridad.FiltroUsuario;
import uteq.edu.ec.artisync.dto.seguridad.request.*;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.service.shared.reporte.TipoGraficaReporte;

public interface AdminUserService {
    /**
     * Lista usuarios paginados, aplicando filtros de búsqueda, rol y estado de cuenta.
     *
     * @param filtro   criterios de búsqueda, rol y estado de cuenta
     * @param pageable paginación y orden solicitados
     * @return la página de usuarios que cumplen el filtro
     */
    PagedResponse<UserResponse> getAllUsers(FiltroUsuario filtro, Pageable pageable);

    /**
     * Obtiene el detalle de un usuario por su id.
     *
     * @param id id del usuario
     * @return el detalle del usuario
     */
    UserResponse getUserById(Long id);

    /**
     * Crea un usuario desde el panel de administración, con los roles indicados (CLIENTE por defecto).
     *
     * @param request datos del usuario a crear (nombres, correo, contraseña, roles, etc.)
     * @return el usuario recién creado
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * Actualiza los datos de un usuario existente, incluyendo país, 2FA y roles si se envían.
     *
     * @param id      id del usuario a actualizar
     * @param request campos a modificar; los nulos se dejan sin cambios
     * @return el usuario ya actualizado
     */
    UserResponse updateUser(Long id, AdminUpdateUserRequest request);

    /**
     * Activa o desactiva la cuenta de un usuario, revocando sus sesiones si pasa a inactiva.
     *
     * @param id             id del usuario
     * @param request        nuevo estado de cuenta
     * @param idAdminActual  id del administrador que ejecuta la acción
     * @return el usuario con su estado ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el administrador intenta desactivar su propia cuenta
     */
    UserResponse changeEstado(Long id, ChangeEstadoRequest request, Long idAdminActual);

    /**
     * Reasigna los roles de un usuario y revoca sus sesiones para forzar el refresco de claims del JWT.
     *
     * @param id             id del usuario
     * @param request        nuevo conjunto de roles
     * @param idAdminActual  id del administrador que ejecuta la acción
     * @return el usuario con sus roles ya actualizados
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el administrador intenta cambiar sus propios roles
     */
    UserResponse assignRoles(Long id, AssignRolesRequest request, Long idAdminActual);

    /**
     * Revoca todas las sesiones activas de un usuario.
     *
     * @param id id del usuario cuyas sesiones se revocan
     * @return mensaje de confirmación
     */
    RespuestaMensaje revokeUserSessions(Long id);

    /**
     * Desactiva la cuenta de un usuario (soft-delete: no hay borrado físico) y revoca sus sesiones.
     *
     * @param id            id del usuario a desactivar
     * @param idAdminActual id del administrador que ejecuta la acción
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el administrador intenta eliminar su propia cuenta
     */
    void deleteUser(Long id, Long idAdminActual);

    /**
     * Exporta el listado de usuarios que cumplen el filtro indicado, en el formato solicitado.
     *
     * @param filtro            criterios de búsqueda, rol y estado de cuenta
     * @param formato           formato del documento a generar
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con el listado de usuarios
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el listado filtrado excede el tope de filas admitido por el formato
     */
    DocumentoGenerado exportar(FiltroUsuario filtro, FormatoReporte formato, String correoSolicitante);
    DocumentoGenerado exportar(FiltroUsuario filtro, FormatoReporte formato, TipoGraficaReporte tipoGrafica, String correoSolicitante);
}
