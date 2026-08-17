package uteq.edu.ec.artisync.service.perfil.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPerfil;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPerfil;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPerfil;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PerfilCreadorServicioImplTest {

    @Mock private PerfilCreadorRepository perfilRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PerfilCreadorServicioImpl perfilCreadorServicio;

    private Usuario usuario;
    private PerfilCreador perfil;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().idUsuario(1L).nombres("Ana").apellidos("Diaz").build();
        perfil = PerfilCreador.builder().idPerfil(10L).usuario(usuario).biografia("bio").urlRedSocial("http://x.com").build();
    }

    @Test
    @DisplayName("crearPerfil guarda cuando el usuario no tiene perfil todavia")
    void crearPerfil_guarda() {
        PeticionCrearPerfil peticion = new PeticionCrearPerfil(1L, "bio", "http://x.com");
        given(perfilRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.empty());
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(perfilRepository.save(any(PerfilCreador.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaPerfil respuesta = perfilCreadorServicio.crearPerfil(peticion);

        assertThat(respuesta.nombresUsuario()).isEqualTo("Ana");
        assertThat(respuesta.biografia()).isEqualTo("bio");
    }

    @Test
    @DisplayName("crearPerfil rechaza si el usuario ya tiene perfil")
    void crearPerfil_rechazaDuplicado() {
        PeticionCrearPerfil peticion = new PeticionCrearPerfil(1L, "bio", null);
        given(perfilRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfil));

        assertThatThrownBy(() -> perfilCreadorServicio.crearPerfil(peticion))
                .isInstanceOf(ExcepcionRecursoDuplicado.class);
    }

    @Test
    @DisplayName("crearPerfil lanza recurso no encontrado si el usuario no existe")
    void crearPerfil_usuarioInexistente() {
        PeticionCrearPerfil peticion = new PeticionCrearPerfil(1L, "bio", null);
        given(perfilRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.empty());
        given(usuarioRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> perfilCreadorServicio.crearPerfil(peticion))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerPerfilPorId lanza recurso no encontrado si no existe")
    void obtenerPerfilPorId_inexistente() {
        given(perfilRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> perfilCreadorServicio.obtenerPerfilPorId(10L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerPerfilPorUsuario devuelve el perfil existente")
    void obtenerPerfilPorUsuario_devuelve() {
        given(perfilRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.of(perfil));

        assertThat(perfilCreadorServicio.obtenerPerfilPorUsuario(1L).idPerfil()).isEqualTo(10L);
    }

    @Test
    @DisplayName("obtenerPerfilPorUsuario lanza recurso no encontrado si no existe")
    void obtenerPerfilPorUsuario_inexistente() {
        given(perfilRepository.findByUsuarioIdUsuario(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> perfilCreadorServicio.obtenerPerfilPorUsuario(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("listarPerfiles mapea todos los registros")
    void listarPerfiles_mapea() {
        given(perfilRepository.findAll()).willReturn(List.of(perfil));

        assertThat(perfilCreadorServicio.listarPerfiles()).hasSize(1);
    }

    @Test
    @DisplayName("actualizarPerfil cambia biografia y red social cuando se indican")
    void actualizarPerfil_cambiaDatos() {
        PeticionActualizarPerfil peticion = new PeticionActualizarPerfil("nueva bio", "http://y.com");
        given(perfilRepository.findById(10L)).willReturn(Optional.of(perfil));
        given(perfilRepository.save(any(PerfilCreador.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaPerfil respuesta = perfilCreadorServicio.actualizarPerfil(10L, peticion);

        assertThat(respuesta.biografia()).isEqualTo("nueva bio");
        assertThat(respuesta.urlRedSocial()).isEqualTo("http://y.com");
    }

    @Test
    @DisplayName("actualizarPerfil lanza recurso no encontrado si no existe")
    void actualizarPerfil_inexistente() {
        given(perfilRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> perfilCreadorServicio.actualizarPerfil(10L, new PeticionActualizarPerfil(null, null)))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("eliminarPerfil borra cuando existe")
    void eliminarPerfil_borraCuandoExiste() {
        given(perfilRepository.existsById(10L)).willReturn(true);

        perfilCreadorServicio.eliminarPerfil(10L);

        verify(perfilRepository).deleteById(10L);
    }

    @Test
    @DisplayName("eliminarPerfil lanza recurso no encontrado si no existe")
    void eliminarPerfil_inexistente() {
        given(perfilRepository.existsById(10L)).willReturn(false);

        assertThatThrownBy(() -> perfilCreadorServicio.eliminarPerfil(10L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }
}
