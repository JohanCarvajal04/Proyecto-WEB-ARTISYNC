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

public interface AdminUserService {
    PagedResponse<UserResponse> getAllUsers(FiltroUsuario filtro, Pageable pageable);
    UserResponse getUserById(Long id);
    UserResponse createUser(CreateUserRequest request);
    UserResponse updateUser(Long id, AdminUpdateUserRequest request);
    UserResponse changeEstado(Long id, ChangeEstadoRequest request, Long idAdminActual);
    UserResponse assignRoles(Long id, AssignRolesRequest request, Long idAdminActual);
    RespuestaMensaje revokeUserSessions(Long id);
    void deleteUser(Long id, Long idAdminActual);
    DocumentoGenerado exportar(FiltroUsuario filtro, FormatoReporte formato, String correoSolicitante);
}

