package uteq.edu.ec.artisync.service.seguridad.impl;
import uteq.edu.ec.artisync.service.seguridad.*;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditContext;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.seguridad.UserFilter;
import uteq.edu.ec.artisync.dto.seguridad.request.*;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.entity.seguridad.*;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;
import uteq.edu.ec.artisync.repository.catalogo.*;
import uteq.edu.ec.artisync.repository.pedido.*;
import uteq.edu.ec.artisync.repository.legal.*;
import uteq.edu.ec.artisync.repository.comunicacion.*;
import uteq.edu.ec.artisync.repository.social.*;
import uteq.edu.ec.artisync.security.JwtService;
import uteq.edu.ec.artisync.service.seguridad.AdminUserService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportColumn;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.service.shared.reporte.IExportService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportModel;
import uteq.edu.ec.artisync.specification.seguridad.UserSpecification;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.util.PagedResponseBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.StoredProcedureExceptionTranslator;
import uteq.edu.ec.artisync.service.shared.UserMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository usuarioRepository;
    private final UserRoleRepository usuarioRolRepository;
    private final CountryRepository paisRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper usuarioMapper;
    private final SessionRevocationService sessionRevocationService;
    private final TwoFactorAuthenticationRepository autenticacionDosFactoresRepository;
    private final EntityManager entityManager;
    private final IExportService servicioExportacion;
    private final uteq.edu.ec.artisync.service.shared.reporte.impl.ReportChartGenerator generadorGraficaReporte;

    @Override
    @Transactional(readOnly = true)
    // Fase 2 rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §8):
    // toUserResponseList batchea los roles/permisos/2FA de toda la pagina en
    // dos consultas IN (...), en vez del N+1 de invocar toUserResponse() (dos
    // consultas por fila) elemento a elemento. El Pageable/Sort de la peticion
    // se conserva intacto -- se sigue resolviendo con findAll(pageable), no se
    // reemplaza por una rutina con orden fijo.
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @param pageable configuracion de paginacion y ordenamiento para la capa de datos
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public PagedResponse<UserResponse> getAllUsers(UserFilter filtro, Pageable pageable) {
        Specification<User> spec = UserSpecification.conFiltros(
                filtro.getBusqueda(), filtro.getRol(), filtro.getEstadoCuenta());
        Page<User> usuariosPage = usuarioRepository.findAll(spec, pageable);
        return PagedResponseBuilder.buildAndMapList(usuariosPage, usuarioMapper::toUserResponseList);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param id identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public UserResponse getUserById(Long id) {
        User usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User no encontrado con ID: " + id));
        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    @Auditable(accion = "USUARIO_CREAR", modulo = AuditModule.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#resultado.idUsuario",
            detalle = "{correo: #request.correo, roles: #request.roles}")
    // Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §4): delega
    // en fn_crear_usuario_admin, que captura unique_violation sobre el correo
    // en vez de la comprobacion existsByCorreo previa a esta version, que no
    // era atomica respecto al save() (lectura fantasma, A3), y compone con
    // fn_sincronizar_roles_usuario (Fase 1) para los roles y el perfil de
    // creador en la misma transaccion.
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param request estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public UserResponse createUser(CreateUserRequest request) {
        List<String> rolesAsignar = (request.getRoles() != null && !request.getRoles().isEmpty())
                ? request.getRoles() : List.of("CLIENTE");
        String[] roles = rolesAsignar.stream().map(String::toUpperCase).toArray(String[]::new);

        Long idUsuario;
        try {
            idUsuario = usuarioRepository.crearUsuarioAdmin(
                    request.getNombres(),
                    request.getApellidos(),
                    request.getCorreo(),
                    passwordEncoder.encode(request.getContrasena()),
                    request.getFechaNacimiento(),
                    request.getIdPais(),
                    request.getEstadoCuenta(),
                    roles);
        } catch (RuntimeException e) {
            throw StoredProcedureExceptionTranslator.traducir(e, HttpStatus.BAD_REQUEST);
        }

        User usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear el usuario"));

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    @Auditable(accion = "USUARIO_EDITAR", modulo = AuditModule.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#id",
            detalle = "{estadoCuenta: #request.estadoCuenta, roles: #request.roles}")
    /**
     * Aplica modificaciones y validaciones de negocio sobre los datos de un registro existente.
     *
     * @param id identificador unico que referencia de manera univoca al registro
     * @param request estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public UserResponse updateUser(Long id, AdminUpdateUserRequest request) {
        User usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User no encontrado"));

        AuditContext.aportar("antes", Map.of(
                "nombres", usuario.getNombres(),
                "apellidos", usuario.getApellidos(),
                "estadoCuenta", usuario.getEstadoCuenta()));

        if (request.getNombres() != null && !request.getNombres().isBlank()) {
            usuario.setNombres(request.getNombres());
        }
        if (request.getApellidos() != null && !request.getApellidos().isBlank()) {
            usuario.setApellidos(request.getApellidos());
        }
        if (request.getFechaNacimiento() != null) {
            usuario.setFechaNacimiento(request.getFechaNacimiento());
        }
        if (request.getIdPais() != null) {
            if (request.getIdPais() <= 0) {
                usuario.setPais(null);
            } else {
                Country pais = paisRepository.findById(request.getIdPais())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "País no encontrado"));
                usuario.setPais(pais);
            }
        }
        if (Boolean.FALSE.equals(request.getDosFactoresHabilitado())) {
            // Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §7):
            // fn_desactivar_2fa desactiva el flag y purga los codigos de
            // respaldo en una unica transaccion; es idempotente (no lanza si
            // el usuario no tenia 2FA configurado, igual que el ifPresent
            // anterior). Unifica el codigo antes duplicado con
            // TwoFactorServiceImpl.disable2Fa.
            boolean desactivado = autenticacionDosFactoresRepository.desactivar2Fa(usuario.getIdUsuario());
            if (desactivado) {
                log.info("2FA desactivado por administrador para usuario: {}", usuario.getCorreo());
            }
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            actualizarRoles(usuario, request.getRoles());
        }

        // Persiste primero nombres/apellidos/fechaNacimiento/pais (los unicos
        // campos mutados directamente en la entidad hasta aqui) para que este
        // UPDATE no incluya estado_cuenta -- ese campo se cambia mas abajo por
        // la rutina atomica, nunca por esta escritura JPA.
        usuario = usuarioRepository.save(usuario);

        if (request.getEstadoCuenta() != null) {
            // Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5):
            // fn_cambiar_estado_cuenta decide "hubo transicion activa->inactiva?"
            // y revoca sesiones bajo SELECT FOR UPDATE en el motor, en vez de
            // comparar aqui un estadoAnterior que otro administrador concurrente
            // pudo dejar obsoleto (actualizacion perdida).
            //
            // entityManager.refresh() en vez de usuario.setEstadoCuenta(...):
            // el save() de arriba ya vacio cualquier cambio pendiente, asi que
            // no hay nada que perder; refresh() releae la fila (reflejando el
            // estado_cuenta que la funcion nativa acaba de escribir) y
            // resincroniza el snapshot de Hibernate, evitando que el commit de
            // esta transaccion dispare un SEGUNDO UPDATE redundante reescribiendo
            // el mismo valor que la funcion atomica ya persistio.
            sessionRevocationService.cambiarEstadoCuenta(usuario.getIdUsuario(), request.getEstadoCuenta());
            entityManager.refresh(usuario);
        }

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    @Auditable(accion = "USUARIO_CAMBIAR_ESTADO", modulo = AuditModule.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#id",
            detalle = "{estadoCuenta: #request.estadoCuenta}")
    /**
     * Aplica modificaciones y validaciones de negocio sobre los datos de un registro existente.
     *
     * @param id identificador unico que referencia de manera univoca al registro
     * @param request estructura de transferencia de datos con la informacion estructurada de entrada
     * @param idAdminActual identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public UserResponse changeEstado(Long id, ChangeEstadoRequest request, Long idAdminActual) {
        if (id.equals(idAdminActual) && !request.getEstadoCuenta()) {
            throw new BusinessRuleException("No puedes desactivar tu propia cuenta.");
        }

        User usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User no encontrado"));

        // Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5):
        // fn_cambiar_estado_cuenta aplica el cambio y revoca sesiones (si hubo
        // transicion activa->inactiva) en una unica transaccion serializada con
        // SELECT FOR UPDATE, sustituyendo el find+mutate+save+revocar en cuatro
        // pasos no atomicos que tenia esta operacion.
        //
        // entityManager.refresh() en vez de usuario.setEstadoCuenta(...): este
        // metodo no tiene ningun otro cambio pendiente sobre `usuario`, asi que
        // mutar el campo en el objeto managed lo marcaria "dirty" y Hibernate
        // emitiria, al confirmar la transaccion, un UPDATE adicional reescribiendo
        // el mismo valor que la funcion atomica ya persistio. refresh() releae la
        // fila real (sin escribir nada) y resincroniza el snapshot de Hibernate.
        sessionRevocationService.cambiarEstadoCuenta(usuario.getIdUsuario(), request.getEstadoCuenta());
        entityManager.refresh(usuario);

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    @Auditable(accion = "USUARIO_ASIGNAR_ROLES", modulo = AuditModule.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#id",
            detalle = "{roles: #request.roles}")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param id identificador unico que referencia de manera univoca al registro
     * @param request estructura de transferencia de datos con la informacion estructurada de entrada
     * @param idAdminActual identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public UserResponse assignRoles(Long id, AssignRolesRequest request, Long idAdminActual) {
        if (id.equals(idAdminActual)) {
            throw new BusinessRuleException("No puedes cambiar tus propios roles. Pide a otro administrador que lo haga.");
        }

        User usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User no encontrado"));

        AuditContext.aportar("antes", Map.of("roles",
                usuarioRolRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                        .map(ur -> ur.getRol().getNombreRol())
                        .toList()));

        actualizarRoles(usuario, request.getRoles());
        sessionRevocationService.revocarSesionesUsuario(usuario.getIdUsuario()); // Revocar sesiones para obligar a refrescar claims JWT con nuevos roles

        return usuarioMapper.toUserResponse(usuario);
    }

    @Override
    @Transactional
    // Es un soft-delete (estadoCuenta=false), no un borrado físico: la acción
    // se llama USUARIO_DESACTIVAR y no USUARIO_ELIMINAR para que la bitácora
    // describa lo que realmente ocurre en la base de datos.
    @Auditable(accion = "USUARIO_DESACTIVAR", modulo = AuditModule.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#id")
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param id identificador unico que referencia de manera univoca al registro
     * @param idAdminActual identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void deleteUser(Long id, Long idAdminActual) {
        if (id.equals(idAdminActual)) {
            throw new BusinessRuleException("No puedes eliminar tu propia cuenta.");
        }

        // Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5):
        // fn_cambiar_estado_cuenta desactiva la cuenta y revoca sus sesiones
        // atomicamente; ya no hace falta cargar la entidad completa. El
        // existsById previo era redundante: fn_cambiar_estado_cuenta ya
        // lanza P0002 si el usuario no existe, y SessionRevocationService
        // ya lo traduce a 404 (revision de codigo, hallazgo de eficiencia).
        sessionRevocationService.cambiarEstadoCuenta(id, false);
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(accion = "USUARIO_EXPORTAR", modulo = AuditModule.SEGURIDAD, entidad = "usuarios",
            detalle = "{formato: #formato}")
    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @param formato parametro requerido para la correcta ejecucion del procedimiento
     * @param correoSolicitante direccion de correo electronico del actor o usuario principal
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public GeneratedDocument exportar(UserFilter filtro, ReportFormat formato, String correoSolicitante) {
        return exportar(filtro, formato, uteq.edu.ec.artisync.service.shared.reporte.ReportChartType.AMBAS, null, null, correoSolicitante);
    }

    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     * @param filtro filtro de busqueda
     * @param formato formato de reporte
     * @param correoAdmin correo del admin
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     */
    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     * @param filtro filtro de busqueda
     * @param formato formato de reporte
     * @param orderSorts ordenamiento
     * @param correoAdmin correo del admin
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     */
    @Override
    @Transactional(readOnly = true)
    public GeneratedDocument exportar(UserFilter filtro, ReportFormat formato,
                                      uteq.edu.ec.artisync.service.shared.reporte.ReportChartType tipoGrafica,
                                      String correoSolicitante) {
        return exportar(filtro, formato, tipoGrafica, null, null, correoSolicitante);
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(accion = "USUARIO_EXPORTAR", modulo = AuditModule.SEGURIDAD, entidad = "usuarios",
            detalle = "{formato: #formato, grafica: #tipoGrafica, page: #page, size: #size}")
    public GeneratedDocument exportar(UserFilter filtro, ReportFormat formato,
                                      uteq.edu.ec.artisync.service.shared.reporte.ReportChartType tipoGrafica,
                                      Integer page, Integer size,
                                      String correoSolicitante) {
        Specification<User> spec = UserSpecification.conFiltros(
                filtro.getBusqueda(), filtro.getRol(), filtro.getEstadoCuenta());

        Page<User> pagina;
        String titulo = "Usuarios";
        String subtitulo = "Listado administrativo de usuarios";

        if (page != null) {
            int pageSize = (size != null && size > 0 && size <= formato.topeFilas()) ? size : formato.topeFilas();
            pagina = usuarioRepository.findAll(spec, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "idUsuario")));
            int parte = page + 1;
            int totalPartes = Math.max(1, pagina.getTotalPages());
            titulo = "Usuarios - Parte " + parte;
            subtitulo = "Listado administrativo de usuarios — Parte " + parte + " de " + totalPartes
                    + " (" + pagina.getTotalElements() + " usuarios en total)";
        } else {
            long total = usuarioRepository.count(spec);
            if (total > formato.topeFilas()) {
                throw new BusinessRuleException(
                        "El listado filtrado tiene " + total + " usuarios, más de los " + formato.topeFilas()
                                + " que admite una exportación en " + formato + ". Acote los filtros o utilice la opción de exportar por partes.");
            }

            pagina = usuarioRepository.findAll(
                    spec, PageRequest.of(0, formato.topeFilas(), Sort.by(Sort.Direction.ASC, "idUsuario")));
        }

        List<UserResponse> filas = usuarioMapper.toUserResponseList(pagina.getContent());

        // Calcular métricas (KPIs) y datos agregados para las gráficas
        uteq.edu.ec.artisync.service.shared.reporte.ReportChartType graficaElegida =
                tipoGrafica != null ? tipoGrafica : uteq.edu.ec.artisync.service.shared.reporte.ReportChartType.AMBAS;
        List<uteq.edu.ec.artisync.service.shared.reporte.ReportChart> graficas = new ArrayList<>();
        List<uteq.edu.ec.artisync.service.shared.reporte.ReportKpi> kpis = new ArrayList<>();

        if (!filas.isEmpty() && (formato == ReportFormat.PDF || formato == ReportFormat.XLSX)) {
            Map<String, Long> conteoRoles = new LinkedHashMap<>();
            Map<String, Long> conteoPaises = new LinkedHashMap<>();
            long activos = 0;

            for (UserResponse u : filas) {
                if (Boolean.TRUE.equals(u.getEstadoCuenta())) {
                    activos++;
                }

                if (u.getRoles() != null && !u.getRoles().isEmpty()) {
                    for (String rol : u.getRoles()) {
                        conteoRoles.put(rol, conteoRoles.getOrDefault(rol, 0L) + 1);
                    }
                } else {
                    conteoRoles.put("SIN_ROL", conteoRoles.getOrDefault("SIN_ROL", 0L) + 1);
                }

                String pais = (u.getNombrePais() != null && !u.getNombrePais().isBlank())
                        ? u.getNombrePais() : "Sin especificar";
                conteoPaises.put(pais, conteoPaises.getOrDefault(pais, 0L) + 1);
            }

            // KPIs
            kpis.add(new uteq.edu.ec.artisync.service.shared.reporte.ReportKpi("Total Usuarios", String.valueOf(filas.size()), "Registrados en listado"));
            double pctActivos = ((double) activos / filas.size()) * 100.0;
            kpis.add(new uteq.edu.ec.artisync.service.shared.reporte.ReportKpi("Usuarios Activos", String.format("%d (%.0f%%)", activos, pctActivos), "Cuentas habilitadas"));
            kpis.add(new uteq.edu.ec.artisync.service.shared.reporte.ReportKpi("Roles Representados", String.valueOf(conteoRoles.size()), "Roles distintos"));
            kpis.add(new uteq.edu.ec.artisync.service.shared.reporte.ReportKpi("Países", String.valueOf(conteoPaises.size()), "Distribución geográfica"));

            // Gráficas estadísticas
            if (graficaElegida == uteq.edu.ec.artisync.service.shared.reporte.ReportChartType.ROL
                    || graficaElegida == uteq.edu.ec.artisync.service.shared.reporte.ReportChartType.AMBAS) {
                byte[] imgRol = generadorGraficaReporte.generarGraficaRol(conteoRoles);
                graficas.add(new uteq.edu.ec.artisync.service.shared.reporte.ReportChart("Distribución de Usuarios por Role",
                        "Proporción de usuarios según su rol asignado", imgRol, conteoRoles));
            }
            if (graficaElegida == uteq.edu.ec.artisync.service.shared.reporte.ReportChartType.PAIS
                    || graficaElegida == uteq.edu.ec.artisync.service.shared.reporte.ReportChartType.AMBAS) {
                byte[] imgPais = generadorGraficaReporte.generarGraficaPais(conteoPaises);
                graficas.add(new uteq.edu.ec.artisync.service.shared.reporte.ReportChart("Distribución de Usuarios por País",
                        "Concentración geográfica de los usuarios registrados", imgPais, conteoPaises));
            }
        }

        ReportModel<UserResponse> modelo = ReportModel.<UserResponse>builder()
                .titulo(titulo)
                .subtitulo(subtitulo)
                .filtrosAplicados(filtrosLegibles(filtro))
                .kpis(kpis)
                .graficas(graficas)
                .columnas(List.of(
                        ReportColumn.entero("Id", UserResponse::getIdUsuario),
                        ReportColumn.texto("Nombres", UserResponse::getNombres),
                        ReportColumn.texto("Apellidos", UserResponse::getApellidos),
                        ReportColumn.texto("Correo", UserResponse::getCorreo),
                        ReportColumn.texto("País", UserResponse::getNombrePais),
                        ReportColumn.fechaHora("Registrado", UserResponse::getFechaRegistro),
                        ReportColumn.booleano("Activo", UserResponse::getEstadoCuenta),
                        ReportColumn.texto("Roles", u -> u.getRoles() == null ? "" : String.join(", ", u.getRoles()))))
                .filas(filas)
                .generadoPor(correoSolicitante)
                .build();

        return servicioExportacion.exportar(modelo, formato);
    }

    private Map<String, String> filtrosLegibles(UserFilter filtro) {
        Map<String, String> filtros = new LinkedHashMap<>();
        if (filtro.getBusqueda() != null && !filtro.getBusqueda().isBlank()) {
            filtros.put("Búsqueda", filtro.getBusqueda());
        }
        if (filtro.getRol() != null && !filtro.getRol().isBlank()) {
            filtros.put("Role", filtro.getRol());
        }
        if (filtro.getEstadoCuenta() != null) {
            filtros.put("Estado", filtro.getEstadoCuenta() ? "Activo" : "Suspendido");
        }
        return filtros;
    }

    @Override
    @Auditable(accion = "SESION_REVOCAR", modulo = AuditModule.SEGURIDAD,
            entidad = "usuarios", idEntidad = "#id")
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param id identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaMensaje revokeUserSessions(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User no encontrado con ID: " + id);
        }
        sessionRevocationService.revocarSesionesUsuario(id);
        return new RespuestaMensaje("Se han revocado exitosamente todas las sesiones del usuario ID: " + id);
    }

    /**
     * Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §3) -
     * fn_sincronizar_roles_usuario reemplaza atomicamente, en una unica llamada
     * al motor, lo que antes eran ~10 viajes no atomicos (findByUsuarioIdUsuario
     * + deleteAll + flush + por cada rol: findByNombreRol + save + consulta de
     * perfil + save de perfil). Corrige dos anomalias: la lectura fantasma que
     * permitia roles duplicados cuando dos administradores editaban al mismo
     * usuario a la vez, y el estado a medias (usuario sin ningun rol) si el
     * bucle en Java fallaba despues del delete.
     */
    private void actualizarRoles(User usuario, List<String> nuevosRoles) {
        try {
            usuarioRolRepository.sincronizarRoles(
                    usuario.getIdUsuario(),
                    nuevosRoles.stream().map(String::toUpperCase).toArray(String[]::new));
        } catch (RuntimeException e) {
            throw StoredProcedureExceptionTranslator.traducir(e, HttpStatus.BAD_REQUEST);
        }
    }
}

