package uteq.edu.ec.artisync.service.seguridad.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.seguridad.response.TwoFactorSetupResponse;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.AutenticacionDosFactores;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.AutenticacionDosFactoresRepository;
import uteq.edu.ec.artisync.repository.seguridad.CodigoRespaldo2FaRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRolRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AutenticacionDosFactoresRepository autenticacionDosFactoresRepository;
    @Mock
    private CodigoRespaldo2FaRepository codigoRespaldo2FaRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private PerfilCreadorRepository perfilCreadorRepository;
    @Mock
    private CertificadoIaRepository certificadoIaRepository;

    @InjectMocks
    private TwoFactorServiceImpl twoFactorService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .idUsuario(1L)
                .correo("creador@example.com")
                .build();
    }

    @Test
    void setup2Fa_ShouldThrowForbidden_WhenUserIsCreadorAndNotApproved() {
        when(usuarioRepository.findByCorreo("creador@example.com")).thenReturn(Optional.of(usuario));
        Rol rolCreador = Rol.builder().nombreRol("CREADOR").build();
        UsuarioRol ur = UsuarioRol.builder().rol(rolCreador).build();
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(ur));
        
        PerfilCreador perfil = PerfilCreador.builder().idPerfil(10L).usuario(usuario).build();
        when(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(perfil));
        when(certificadoIaRepository.existsByPerfilIdPerfilAndEstadoVerificacionNombreEstado(10L, "APROBADO")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> twoFactorService.setup2Fa("creador@example.com"));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("verificar tu identidad"));
    }

    @Test
    void setup2Fa_ShouldSucceed_WhenUserIsCliente() {
        when(usuarioRepository.findByCorreo("cliente@example.com")).thenReturn(Optional.of(usuario));
        Rol rolCliente = Rol.builder().nombreRol("CLIENTE").build();
        UsuarioRol ur = UsuarioRol.builder().rol(rolCliente).build();
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(ur));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.empty());

        TwoFactorSetupResponse response = twoFactorService.setup2Fa("cliente@example.com");

        assertNotNull(response);
        assertNotNull(response.getSecreto());
        assertEquals(8, response.getCodigosRespaldo().size());
    }
}
