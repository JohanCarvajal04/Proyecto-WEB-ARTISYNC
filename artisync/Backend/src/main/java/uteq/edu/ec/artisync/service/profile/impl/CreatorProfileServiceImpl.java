package uteq.edu.ec.artisync.service.profile.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.request.profile.CreateProfileRequest;
import uteq.edu.ec.artisync.dto.request.profile.UpdateProfileRequest;
import uteq.edu.ec.artisync.dto.response.profile.ProfileResponse;
import uteq.edu.ec.artisync.entity.profile.CreatorProfile;
import uteq.edu.ec.artisync.entity.security.User;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.security.UserRepository;
import uteq.edu.ec.artisync.repository.profile.CreatorProfileRepository;
import uteq.edu.ec.artisync.service.profile.ICreatorProfileService;
import uteq.edu.ec.artisync.service.profile.IVerificationService;
import uteq.edu.ec.artisync.service.shared.storage.ProfilePhotoUrl;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreatorProfileServiceImpl implements ICreatorProfileService {

    private final CreatorProfileRepository perfilRepository;
    private final UserRepository usuarioRepository;
    private final IVerificationService verificacionServicio;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProfileResponse createProfile(CreateProfileRequest peticion, String correoSolicitante, boolean esAdmin) {
        // El idUsuario del cuerpo solo se honra para un ADMIN. Antes se confiaba
        // en él sin más, así que cualquier CREADOR podía crear un perfil a nombre
        // de un idUsuario arbitrario.
        Long idDestino = esAdmin
                ? peticion.idUsuario()
                : resolveByEmail(correoSolicitante).getIdUsuario();

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
        return mapToResponse(guardado);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(Long idPerfil) {
        CreatorProfile perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado con ID: " + idPerfil));
        requireActiveAccount(perfil);
        return mapToResponse(perfil);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileByUser(Long idUsuario) {
        CreatorProfile perfil = perfilRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró perfil para el usuario con ID: " + idUsuario));
        requireActiveAccount(perfil);
        return mapToResponse(perfil);
    }

    /**
     * REQ-NF-018 (ajuste de seguimiento): oculta el perfil público de un
     * creador con la cuenta desactivada (soft-eliminar o supresión real) —
     * mismo criterio que ya aplica {@code listActiveProfiles()}, sin
     * excepción para ningún llamante: esta ruta está marcada permitAll() en
     * SecurityConfig y no distingue admin de público. Se responde igual que
     * "no existe" (404) para no revelar si el perfil está desactivado o
     * nunca existió.
     */
    private void requireActiveAccount(CreatorProfile perfil) {
        User usuario = perfil.getUsuario();
        if (usuario != null && !Boolean.TRUE.equals(usuario.getEstadoCuenta())) {
            throw new ResourceNotFoundException("Perfil no disponible");
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<ProfileResponse> listProfiles() {
        return perfilRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<ProfileResponse> listActiveProfiles() {
        return perfilRepository.findByUsuarioEstadoCuentaTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ProfileResponse updateProfile(Long idPerfil, UpdateProfileRequest peticion,
                                            String correoSolicitante, boolean esAdmin) {
        CreatorProfile perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado con ID: " + idPerfil));

        // El @PreAuthorize del controlador comprueba el ROL, no la propiedad. Sin
        // esta verificación cualquier CREADOR podía sobrescribir la biografía y la
        // urlRedSocial de otro creador enumerando ids con GET /api/v1/perfiles.
        if (!esAdmin) {
            Long propietario = perfil.getUsuario() != null ? perfil.getUsuario().getIdUsuario() : null;
            if (!resolveByEmail(correoSolicitante).getIdUsuario().equals(propietario)) {
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
        return mapToResponse(actualizado);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteProfile(Long idPerfil) {
        if (!perfilRepository.existsById(idPerfil)) {
            throw new ResourceNotFoundException("Perfil no encontrado con ID: " + idPerfil);
        }
        perfilRepository.deleteById(idPerfil);
    }

    /** User autenticado a partir del correo que viaja en el token. */
    private User resolveByEmail(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException("User autenticado no encontrado"));
    }

    private ProfileResponse mapToResponse(CreatorProfile perfil) {
        Long idUsuario = perfil.getUsuario() != null ? perfil.getUsuario().getIdUsuario() : null;
        return ProfileResponse.builder()
                .idPerfil(perfil.getIdPerfil())
                .idUsuario(idUsuario)
                .nombresUsuario(perfil.getUsuario() != null ? perfil.getUsuario().getNombres() : null)
                .apellidosUsuario(perfil.getUsuario() != null ? perfil.getUsuario().getApellidos() : null)
                .biografia(perfil.getBiografia())
                .urlRedSocial(perfil.getUrlRedSocial())
                .urlFotoPerfil(perfil.getUsuario() != null ? ProfilePhotoUrl.build(perfil.getUsuario().getUrlFotoPerfil()) : null)
                .tituloProfesional(perfil.getTituloProfesional())
                // Antes el frontend pintaba "Identidad verificada" fijo para
                // cualquier creador; ahora refleja el estado real (mismo criterio
                // que gatea publicar servicios y crear pedidos).
                .identidadVerificada(idUsuario != null && verificacionServicio.isIdentityVerified(idUsuario))
                .build();
    }
}

