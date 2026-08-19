package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPerfil;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPerfil;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPerfil;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.service.perfil.IPerfilCreadorServicio;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerfilCreadorServicioImpl implements IPerfilCreadorServicio {

    private final PerfilCreadorRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public RespuestaPerfil crearPerfil(PeticionCrearPerfil peticion, String correoSolicitante, boolean esAdmin) {
        // El idUsuario del cuerpo solo se honra para un ADMIN. Antes se confiaba
        // en él sin más, así que cualquier CREADOR podía crear un perfil a nombre
        // de un idUsuario arbitrario.
        Long idDestino = esAdmin
                ? peticion.idUsuario()
                : resolverPorCorreo(correoSolicitante).getIdUsuario();

        if (perfilRepository.findByUsuarioIdUsuario(idDestino).isPresent()) {
            throw new ExcepcionRecursoDuplicado("El usuario ya tiene un perfil de creador asignado.");
        }

        Usuario usuario = usuarioRepository.findById(idDestino)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado con ID: " + idDestino));

        PerfilCreador perfil = PerfilCreador.builder()
                .usuario(usuario)
                .biografia(peticion.biografia())
                .urlRedSocial(peticion.urlRedSocial())
                .build();

        PerfilCreador guardado = perfilRepository.save(perfil);
        return mapearARespuesta(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPerfil obtenerPerfilPorId(Long idPerfil) {
        PerfilCreador perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Perfil no encontrado con ID: " + idPerfil));
        return mapearARespuesta(perfil);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPerfil obtenerPerfilPorUsuario(Long idUsuario) {
        PerfilCreador perfil = perfilRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No se encontró perfil para el usuario con ID: " + idUsuario));
        return mapearARespuesta(perfil);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaPerfil> listarPerfiles() {
        return perfilRepository.findAll().stream()
                .map(this::mapearARespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RespuestaPerfil actualizarPerfil(Long idPerfil, PeticionActualizarPerfil peticion,
                                            String correoSolicitante, boolean esAdmin) {
        PerfilCreador perfil = perfilRepository.findById(idPerfil)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Perfil no encontrado con ID: " + idPerfil));

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

        PerfilCreador actualizado = perfilRepository.save(perfil);
        return mapearARespuesta(actualizado);
    }

    @Override
    @Transactional
    public void eliminarPerfil(Long idPerfil) {
        if (!perfilRepository.existsById(idPerfil)) {
            throw new ExcepcionRecursoNoEncontrado("Perfil no encontrado con ID: " + idPerfil);
        }
        perfilRepository.deleteById(idPerfil);
    }

    /** Usuario autenticado a partir del correo que viaja en el token. */
    private Usuario resolverPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario autenticado no encontrado"));
    }

    private RespuestaPerfil mapearARespuesta(PerfilCreador perfil) {
        return RespuestaPerfil.builder()
                .idPerfil(perfil.getIdPerfil())
                .idUsuario(perfil.getUsuario() != null ? perfil.getUsuario().getIdUsuario() : null)
                .nombresUsuario(perfil.getUsuario() != null ? perfil.getUsuario().getNombres() : null)
                .apellidosUsuario(perfil.getUsuario() != null ? perfil.getUsuario().getApellidos() : null)
                .biografia(perfil.getBiografia())
                .urlRedSocial(perfil.getUrlRedSocial())
                .build();
    }
}

