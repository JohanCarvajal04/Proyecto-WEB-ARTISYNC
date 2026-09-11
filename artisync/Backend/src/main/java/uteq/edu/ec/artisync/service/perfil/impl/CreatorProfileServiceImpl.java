package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.perfil.CreateProfileRequest;
import uteq.edu.ec.artisync.dto.peticion.perfil.UpdateProfileRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.ProfileResponse;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.repository.perfil.CreatorProfileRepository;
import uteq.edu.ec.artisync.service.perfil.ICreatorProfileService;
import uteq.edu.ec.artisync.service.perfil.IVerificationService;
import uteq.edu.ec.artisync.service.shared.almacenamiento.ProfilePhotoUrl;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreatorProfileServiceImpl implements ICreatorProfileService {

    private final CreatorProfileRepository perfilRepository;
    private final UserRepository usuarioRepository;
    private final IVerificationService verificacionServicio;

    @Override
    @Transactional
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @param correoSolicitante direccion de correo electronico del actor o usuario principal
     * @param esAdmin parametro requerido para la correcta ejecucion del procedimiento
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ProfileResponse crearPerfil(CreateProfileRequest peticion, String correoSolicitante, boolean esAdmin) {
        // El idUsuario del cuerpo solo se honra para un ADMIN. Antes se confiaba
        // en él sin más, así que cualquier CREADOR podía crear un perfil a nombre
        // de un idUsuario arbitrario.
        Long idDestino = esAdmin
                ? peticion.idUsuario()
                : resolverPorCorreo(correoSolicitante).getIdUsuario();

        if (perfilRepository.findByUsuarioIdUsuario(idDestino).isPresent()) {
            throw new DuplicateResourceException("El usuario ya tiene un perfil de creador asignado.");
        }

        User usuario = usuarioRepository.findById(idDestino)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado con ID: " + idDestino));

        CreatorProfile perfil = CreatorProfile.builder()
                .usuario(usuario)
                .biografia(peticion.biografia())
                .urlRedSocial(peticion.urlRedSocial())
                .tituloProfesional(peticion.tituloProfesional())
                .build();

        CreatorProfile guardado = perfilRepository.save(perfil);
        return mapearARespuesta(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idPerfil identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ProfileResponse obtenerPerfilPorId(Long idPerfil) {
        CreatorProfile perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado con ID: " + idPerfil));
        exigirCuentaActiva(perfil);
        return mapearARespuesta(perfil);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ProfileResponse obtenerPerfilPorUsuario(Long idUsuario) {
        CreatorProfile perfil = perfilRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró perfil para el usuario con ID: " + idUsuario));
        exigirCuentaActiva(perfil);
        return mapearARespuesta(perfil);
    }

    /**
     * REQ-NF-018 (ajuste de seguimiento): oculta el perfil público de un
     * creador con la cuenta desactivada (soft-delete o supresión real) —
     * mismo criterio que ya aplica {@code listarPerfilesActivos()}, sin
     * excepción para ningún llamante: esta ruta está marcada permitAll() en
     * SecurityConfig y no distingue admin de público. Se responde igual que
     * "no existe" (404) para no revelar si el perfil está desactivado o
     * nunca existió.
     */
    private void exigirCuentaActiva(CreatorProfile perfil) {
        User usuario = perfil.getUsuario();
        if (usuario != null && !Boolean.TRUE.equals(usuario.getEstadoCuenta())) {
            throw new ResourceNotFoundException("Perfil no disponible");
        }
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<ProfileResponse> listarPerfiles() {
        return perfilRepository.findAll().stream()
                .map(this::mapearARespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<ProfileResponse> listarPerfilesActivos() {
        return perfilRepository.findByUsuarioEstadoCuentaTrue().stream()
                .map(this::mapearARespuesta)
                .collect(Collectors.toList());
    }

    /**
     * Aplica modificaciones y validaciones de negocio sobre los datos de un registro existente.
     * @param idPerfil id del perfil
     * @param peticion peticion
     * @param fotoPortada foto de portada
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     */
    @Override
    @Transactional
    public ProfileResponse actualizarPerfil(Long idPerfil, UpdateProfileRequest peticion,
                                            String correoSolicitante, boolean esAdmin) {
        CreatorProfile perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado con ID: " + idPerfil));

        // El @PreAuthorize del controlador comprueba el ROL, no la propiedad. Sin
        // esta verificación cualquier CREADOR podía sobrescribir la biografía y la
        // urlRedSocial de otro creador enumerando ids con GET /api/v1/perfiles.
        if (!esAdmin) {
            Long propietario = perfil.getUsuario() != null ? perfil.getUsuario().getIdUsuario() : null;
            if (!resolverPorCorreo(correoSolicitante).getIdUsuario().equals(propietario)) {
                throw new AccessDeniedException("No puedes modificar el perfil de otro usuario");
            }
        }

        if (peticion.biografia() != null) {
            perfil.setBiografia(peticion.biografia());
        }
        if (peticion.urlRedSocial() != null) {
            perfil.setUrlRedSocial(peticion.urlRedSocial());
        }
        if (peticion.tituloProfesional() != null) {
            perfil.setTituloProfesional(peticion.tituloProfesional());
        }

        CreatorProfile actualizado = perfilRepository.save(perfil);
        return mapearARespuesta(actualizado);
    }

    @Override
    @Transactional
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idPerfil identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void eliminarPerfil(Long idPerfil) {
        if (!perfilRepository.existsById(idPerfil)) {
            throw new ResourceNotFoundException("Perfil no encontrado con ID: " + idPerfil);
        }
        perfilRepository.deleteById(idPerfil);
    }

    /** User autenticado a partir del correo que viaja en el token. */
    private User resolverPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("User autenticado no encontrado"));
    }

    private ProfileResponse mapearARespuesta(CreatorProfile perfil) {
        Long idUsuario = perfil.getUsuario() != null ? perfil.getUsuario().getIdUsuario() : null;
        return ProfileResponse.builder()
                .idPerfil(perfil.getIdPerfil())
                .idUsuario(idUsuario)
                .nombresUsuario(perfil.getUsuario() != null ? perfil.getUsuario().getNombres() : null)
                .apellidosUsuario(perfil.getUsuario() != null ? perfil.getUsuario().getApellidos() : null)
                .biografia(perfil.getBiografia())
                .urlRedSocial(perfil.getUrlRedSocial())
                .urlFotoPerfil(perfil.getUsuario() != null ? ProfilePhotoUrl.construir(perfil.getUsuario().getUrlFotoPerfil()) : null)
                .tituloProfesional(perfil.getTituloProfesional())
                // Antes el frontend pintaba "Identidad verificada" fijo para
                // cualquier creador; ahora refleja el estado real (mismo criterio
                // que gatea publicar servicios y crear pedidos).
                .identidadVerificada(idUsuario != null && verificacionServicio.estaIdentidadVerificada(idUsuario))
                .build();
    }
}

