package uteq.edu.ec.artisync.controller.perfil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uteq.edu.ec.artisync.dto.peticion.perfil.UpdateProfileRequest;
import uteq.edu.ec.artisync.dto.peticion.perfil.CreateProfileRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.perfil.ProfileResponse;
import uteq.edu.ec.artisync.service.perfil.ICreatorProfileService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatorProfileControllerTest {

    @Mock
    private ICreatorProfileService perfilServicio;

    @InjectMocks
    private CreatorProfileController controlador;

    private Authentication mockAuthentication(boolean admin) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        if (admin) {
            org.mockito.Mockito.lenient().when(auth.getAuthorities())
                    .thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        } else {
            org.mockito.Mockito.lenient().when(auth.getAuthorities())
                    .thenAnswer(inv -> Collections.emptyList());
        }
        return auth;
    }

    @Test
    void crearPerfil_devuelveCreated() {
        Authentication auth = mockAuthentication(false);
        CreateProfileRequest peticion = new CreateProfileRequest(1L, "a", "b", "c");
        ProfileResponse respuesta = new ProfileResponse(1L, 1L, "a", "b", "c", "d", "e", "f", false);
        when(perfilServicio.createProfile(peticion, "test@test.com", false)).thenReturn(respuesta);

        ResponseEntity<ProfileResponse> res = controlador.createProfile(peticion, auth);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerPerfilPorId_devuelveOk() {
        ProfileResponse respuesta = new ProfileResponse(1L, 1L, "a", "b", "c", "d", "e", "f", false);
        when(perfilServicio.getProfileById(10L)).thenReturn(respuesta);

        ResponseEntity<ProfileResponse> res = controlador.getProfileById(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerPerfilPorUsuario_devuelveOk() {
        ProfileResponse respuesta = new ProfileResponse(1L, 1L, "a", "b", "c", "d", "e", "f", false);
        when(perfilServicio.getProfileByUser(1L)).thenReturn(respuesta);

        ResponseEntity<ProfileResponse> res = controlador.getProfileByUser(1L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarPerfiles_devuelveOk() {
        List<ProfileResponse> lista = Collections.emptyList();
        when(perfilServicio.listProfiles()).thenReturn(lista);

        ResponseEntity<List<ProfileResponse>> res = controlador.listProfiles();
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarPerfilesActivos_devuelveOk() {
        List<ProfileResponse> lista = Collections.emptyList();
        when(perfilServicio.listActiveProfiles()).thenReturn(lista);

        ResponseEntity<List<ProfileResponse>> res = controlador.listActiveProfiles();
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void actualizarPerfil_admin_devuelveOk() {
        Authentication auth = mockAuthentication(true);
        UpdateProfileRequest peticion = new UpdateProfileRequest("a", "b", "c");
        ProfileResponse respuesta = new ProfileResponse(1L, 1L, "a", "b", "c", "d", "e", "f", false);
        when(perfilServicio.updateProfile(10L, peticion, "test@test.com", true)).thenReturn(respuesta);

        ResponseEntity<ProfileResponse> res = controlador.updateProfile(10L, peticion, auth);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void eliminarPerfil_devuelveOk() {
        ResponseEntity<RespuestaMensaje> res = controlador.deleteProfile(10L);
        verify(perfilServicio).deleteProfile(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getMensaje()).contains("eliminado exitosamente");
    }
}
