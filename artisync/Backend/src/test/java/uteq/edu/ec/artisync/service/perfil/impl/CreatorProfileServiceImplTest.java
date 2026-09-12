package uteq.edu.ec.artisync.service.perfil.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import uteq.edu.ec.artisync.dto.peticion.perfil.UpdateProfileRequest;
import uteq.edu.ec.artisync.dto.peticion.perfil.CreateProfileRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.ProfileResponse;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.perfil.CreatorProfileRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.perfil.IVerificationService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreatorProfileServiceImplTest {

    private static final String CORREO_ANA = "ana@artisync.dev";
    private static final String CORREO_LUIS = "luis@artisync.dev";
    private static final String ADMIN = "admin@artisync.dev";

    @Mock private CreatorProfileRepository perfilRepository;
    @Mock private UserRepository usuarioRepository;
    @Mock private IVerificationService verificacionServicio;

    @InjectMocks
    private CreatorProfileServiceImpl perfilCreadorServicio;

    private User usuario;
    private CreatorProfile perfil;

    @BeforeEach
    void setUp() {
        usuario = User.builder().idUsuario(1L).nombres("Ana").apellidos("Diaz").build();
        perfil = CreatorProfile.builder().idPerfil(10L).usuario(usuario).biografia("bio").urlRedSocial("http://x.com").build();
        // lenient: no todos los tests llegan a mapearARespuesta (algunos cortan
        // antes con una excepción), y Mockito strict-stubs marcaría el resto
        // como "unnecessary stubbing" si no fuera lenient.
        lenient().when(verificacionServicio.isIdentityVerified(anyLong())).thenReturn(false);
    }

    @Test
    @DisplayName("createProfile guarda cuando el usuario no tiene perfil todavia")
    void crearPerfil_guarda() {
        CreateProfileRequest peticion = new CreateProfileRequest(1L, "bio", "http://x.com", null);
        given(perfilRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.empty());
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(perfilRepository.save(any(CreatorProfile.class))).willAnswer(inv -> inv.getArgument(0));

        ProfileResponse respuesta = perfilCreadorServicio.createProfile(peticion, ADMIN, true);

        assertThat(respuesta.nombresUsuario()).isEqualTo("Ana");
        assertThat(respuesta.biografia()).isEqualTo("bio");
    }

    @Test
    @DisplayName("createProfile rechaza si el usuario ya tiene perfil")
    void crearPerfil_rechazaDuplicado() {
        CreateProfileRequest peticion = new CreateProfileRequest(1L, "bio", null, null);
        given(perfilRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfil));

        assertThatThrownBy(() -> perfilCreadorServicio.createProfile(peticion, ADMIN, true))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("createProfile lanza recurso no encontrado si el usuario no existe")
    void crearPerfil_usuarioInexistente() {
        CreateProfileRequest peticion = new CreateProfileRequest(1L, "bio", null, null);
        given(perfilRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.empty());
        given(usuarioRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> perfilCreadorServicio.createProfile(peticion, ADMIN, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createProfile ignora el idUsuario del cuerpo si el solicitante no es ADMIN")
    void crearPerfil_noAdminNoPuedeCrearAnombreDeOtro() {
        // El cuerpo apunta al usuario 99, pero quien pide es Ana (id 1): el perfil
        // debe crearse para Ana. Antes se creaba para el 99.
        CreateProfileRequest peticion = new CreateProfileRequest(99L, "bio", null, null);
        given(usuarioRepository.findByCorreo(CORREO_ANA)).willReturn(Optional.of(usuario));
        given(perfilRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.empty());
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(perfilRepository.save(any(CreatorProfile.class))).willAnswer(inv -> inv.getArgument(0));

        ProfileResponse respuesta = perfilCreadorServicio.createProfile(peticion, CORREO_ANA, false);

        assertThat(respuesta.idUsuario()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getProfileById lanza recurso no encontrado si no existe")
    void obtenerPerfilPorId_inexistente() {
        given(perfilRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> perfilCreadorServicio.getProfileById(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getProfileByUser devuelve el perfil existente")
    void obtenerPerfilPorUsuario_devuelve() {
        given(perfilRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfil));

        assertThat(perfilCreadorServicio.getProfileByUser(1L).idPerfil()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getProfileById refleja el estado real de verificación de identidad del usuario")
    void obtenerPerfilPorId_reflejaIdentidadVerificada() {
        given(perfilRepository.findById(10L)).willReturn(Optional.of(perfil));
        given(verificacionServicio.isIdentityVerified(1L)).willReturn(true);

        ProfileResponse respuesta = perfilCreadorServicio.getProfileById(10L);

        assertThat(respuesta.identidadVerificada()).isTrue();
    }

    @Test
    @DisplayName("getProfileById no marca identidad verificada si no la tiene aprobada")
    void obtenerPerfilPorId_sinIdentidadVerificada() {
        given(perfilRepository.findById(10L)).willReturn(Optional.of(perfil));
        given(verificacionServicio.isIdentityVerified(1L)).willReturn(false);

        ProfileResponse respuesta = perfilCreadorServicio.getProfileById(10L);

        assertThat(respuesta.identidadVerificada()).isFalse();
    }

    @Test
    @DisplayName("updateProfile cambia el titulo profesional cuando se indica")
    void actualizarPerfil_cambiaTituloProfesional() {
        UpdateProfileRequest peticion = new UpdateProfileRequest(null, null, "Ilustradora & Directora de Arte");
        given(perfilRepository.findById(10L)).willReturn(Optional.of(perfil));
        given(perfilRepository.save(any(CreatorProfile.class))).willAnswer(inv -> inv.getArgument(0));

        ProfileResponse respuesta = perfilCreadorServicio.updateProfile(10L, peticion, ADMIN, true);

        assertThat(respuesta.tituloProfesional()).isEqualTo("Ilustradora & Directora de Arte");
    }

    @Test
    @DisplayName("getProfileByUser lanza recurso no encontrado si no existe")
    void obtenerPerfilPorUsuario_inexistente() {
        given(perfilRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> perfilCreadorServicio.getProfileByUser(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("REQ-NF-018: getProfileById oculta el perfil de una cuenta desactivada/suprimida")
    void obtenerPerfilPorId_ocultaCuentaDesactivada() {
        usuario.setEstadoCuenta(false);
        given(perfilRepository.findById(10L)).willReturn(Optional.of(perfil));

        assertThatThrownBy(() -> perfilCreadorServicio.getProfileById(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("REQ-NF-018: getProfileByUser oculta el perfil de una cuenta desactivada/suprimida")
    void obtenerPerfilPorUsuario_ocultaCuentaDesactivada() {
        usuario.setEstadoCuenta(false);
        given(perfilRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfil));

        assertThatThrownBy(() -> perfilCreadorServicio.getProfileByUser(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listProfiles mapea todos los registros")
    void listarPerfiles_mapea() {
        given(perfilRepository.findAll()).willReturn(List.of(perfil));

        assertThat(perfilCreadorServicio.listProfiles()).hasSize(1);
    }

    @Test
    @DisplayName("updateProfile cambia biografia y red social cuando se indican")
    void actualizarPerfil_cambiaDatos() {
        UpdateProfileRequest peticion = new UpdateProfileRequest("nueva bio", "http://y.com", null);
        given(perfilRepository.findById(10L)).willReturn(Optional.of(perfil));
        given(perfilRepository.save(any(CreatorProfile.class))).willAnswer(inv -> inv.getArgument(0));

        ProfileResponse respuesta = perfilCreadorServicio.updateProfile(10L, peticion, ADMIN, true);

        assertThat(respuesta.biografia()).isEqualTo("nueva bio");
        assertThat(respuesta.urlRedSocial()).isEqualTo("http://y.com");
    }

    @Test
    @DisplayName("updateProfile lanza recurso no encontrado si no existe")
    void actualizarPerfil_inexistente() {
        given(perfilRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> perfilCreadorServicio.updateProfile(
                10L, new UpdateProfileRequest(null, null, null), ADMIN, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateProfile deniega a un CREADOR que no es el propietario")
    void actualizarPerfil_rechazaAjeno() {
        // El perfil 10 es de Ana (id 1); quien pide es Luis (id 2) con rol CREADOR.
        User otro = User.builder().idUsuario(2L).nombres("Luis").apellidos("Paz").build();
        given(perfilRepository.findById(10L)).willReturn(Optional.of(perfil));
        given(usuarioRepository.findByCorreo(CORREO_LUIS)).willReturn(Optional.of(otro));

        assertThatThrownBy(() -> perfilCreadorServicio.updateProfile(
                10L, new UpdateProfileRequest("secuestrada", "http://malo.com", null), CORREO_LUIS, false))
                .isInstanceOf(AccessDeniedException.class);

        verify(perfilRepository, never()).save(any(CreatorProfile.class));
    }

    @Test
    @DisplayName("updateProfile permite al propietario aunque no sea ADMIN")
    void actualizarPerfil_permiteAlPropietario() {
        given(perfilRepository.findById(10L)).willReturn(Optional.of(perfil));
        given(usuarioRepository.findByCorreo(CORREO_ANA)).willReturn(Optional.of(usuario));
        given(perfilRepository.save(any(CreatorProfile.class))).willAnswer(inv -> inv.getArgument(0));

        ProfileResponse respuesta = perfilCreadorServicio.updateProfile(
                10L, new UpdateProfileRequest("mi nueva bio", null, null), CORREO_ANA, false);

        assertThat(respuesta.biografia()).isEqualTo("mi nueva bio");
    }

    @Test
    @DisplayName("deleteProfile borra cuando existe")
    void eliminarPerfil_borraCuandoExiste() {
        given(perfilRepository.existsById(10L)).willReturn(true);

        perfilCreadorServicio.deleteProfile(10L);

        verify(perfilRepository).deleteById(10L);
    }

    @Test
    @DisplayName("deleteProfile lanza recurso no encontrado si no existe")
    void eliminarPerfil_inexistente() {
        given(perfilRepository.existsById(10L)).willReturn(false);

        assertThatThrownBy(() -> perfilCreadorServicio.deleteProfile(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
