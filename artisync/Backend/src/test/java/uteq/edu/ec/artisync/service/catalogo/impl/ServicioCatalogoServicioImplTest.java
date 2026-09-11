package uteq.edu.ec.artisync.service.catalogo.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarAtributo;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarServicio;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearAtributo;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearServicio;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaAtributo;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaServicio;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaServicioResumido;
import uteq.edu.ec.artisync.entity.catalogo.*;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.*;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.service.perfil.IVerificacionServicio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link ServicioCatalogoServicioImpl}: alta, edición,
 * borrado y atributos dinámicos del catálogo, con la autorización de
 * propiedad-o-admin verificada explícitamente porque decide si se lanza
 * {@link BusinessRuleException}.
 */
@ExtendWith(MockitoExtension.class)
class ServicioCatalogoServicioImplTest {

    @Mock private ServicioRepository servicioRepository;
    @Mock private PerfilCreadorRepository perfilRepository;
    @Mock private SubcategoriaRepository subcategoriaRepository;
    @Mock private AtributoDinamicoRepository atributoRepository;
    @Mock private ServicioAtributoRepository servicioAtributoRepository;
    @Mock private EtiquetaRepository etiquetaRepository;
    @Mock private ServicioEtiquetaRepository servicioEtiquetaRepository;
    @Mock private ServicioSubcategoriaRepository servicioSubcategoriaRepository;
    @Mock private IVerificacionServicio verificacionServicio;

    @InjectMocks
    private ServicioCatalogoServicioImpl servicioCatalogoServicio;

    private User usuario;
    private PerfilCreador perfil;
    private Categoria categoria;
    private Subcategoria subcategoria;
    private Servicio servicio;

    @BeforeEach
    void setUp() {
        usuario = User.builder().idUsuario(1L).nombres("Ana").apellidos("Diaz").correo("ana@test.com").build();
        perfil = PerfilCreador.builder().idPerfil(1L).usuario(usuario).build();
        categoria = Categoria.builder().idCategoria(1L).nombreCategoria("Arte").build();
        subcategoria = Subcategoria.builder().idSubcategoria(1L).categoria(categoria).nombreSubcategoria("Ilustracion").build();
        servicio = Servicio.builder()
                .idServicio(10L)
                .perfil(perfil)
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("15.00"))
                .tipoItem("SERVICIO")
                .estadoPublicacion("ACTIVO")
                .cargoRevisionAdicional(BigDecimal.ZERO)
                .limiteRevisionesBase(0)
                .build();

        // Por defecto el creador ya tiene su identidad verificada: la mayoría
        // de estos tests no ejercitan el gating de REQ-F-006 ampliado, así que
        // no deberían fallar por él. `lenient` porque no todos los tests
        // llegan a invocarlo (p. ej. los que fallan antes, por precio inválido).
        lenient().when(verificacionServicio.estaIdentidadVerificada(anyLong())).thenReturn(true);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // ---------- crearServicio ----------

    @Test
    @DisplayName("crearServicio guarda el servicio cuando el precio y las referencias son validas")
    void crearServicio_guardaCuandoEsValido() {
        PeticionCrearServicio peticion = PeticionCrearServicio.builder()
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("15.00"))
                .idsSubcategoria(List.of(1L))
                .tipoItem("SERVICIO")
                .build();

        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(subcategoriaRepository.findAllById(List.of(1L))).willReturn(List.of(subcategoria));
        given(servicioRepository.save(any(Servicio.class))).willReturn(servicio);
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        RespuestaServicio respuesta = servicioCatalogoServicio.crearServicio(1L, peticion);

        assertThat(respuesta.getIdServicio()).isEqualTo(10L);
        assertThat(respuesta.getTituloServicio()).isEqualTo("Ilustracion digital");
        verify(servicioRepository).save(any(Servicio.class));
    }

    @Test
    @DisplayName("crearServicio rechaza precio nulo o menor a 0.01")
    void crearServicio_rechazaPrecioInvalido() {
        PeticionCrearServicio peticion = PeticionCrearServicio.builder().precioBase(new BigDecimal("0.00")).build();

        assertThatThrownBy(() -> servicioCatalogoServicio.crearServicio(1L, peticion))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(perfilRepository);
    }

    @Test
    @DisplayName("crearServicio lanza recurso no encontrado si el perfil creador no existe")
    void crearServicio_perfilInexistente() {
        PeticionCrearServicio peticion = PeticionCrearServicio.builder().precioBase(new BigDecimal("10.00")).build();
        given(perfilRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicioCatalogoServicio.crearServicio(1L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crearServicio rechaza publicar si el perfil no tiene usuario asociado")
    void crearServicio_perfilSinUsuario_lanzaExcepcionReglaNegocio() {
        PerfilCreador perfilSinUsuario = PerfilCreador.builder().idPerfil(2L).usuario(null).build();
        PeticionCrearServicio peticion = PeticionCrearServicio.builder()
                .tituloServicio("X").descripcionDetallada("Descripcion de mas de veinte caracteres")
                .precioBase(new BigDecimal("15.00")).idsSubcategoria(List.of(1L)).build();
        given(perfilRepository.findById(2L)).willReturn(Optional.of(perfilSinUsuario));

        assertThatThrownBy(() -> servicioCatalogoServicio.crearServicio(2L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("verificar tu identidad");
    }

    @Test
    @DisplayName("crearServicio permite gestionar cuando la autenticacion no esta autenticada (p.ej. anonima)")
    void crearServicio_autenticacionNoAutenticada_noRechaza() {
        var authNoAutenticado = new UsernamePasswordAuthenticationToken("nadie@test.com", "N/A");
        authNoAutenticado.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authNoAutenticado);

        PeticionCrearServicio peticion = PeticionCrearServicio.builder()
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("15.00")).idsSubcategoria(List.of(1L)).build();
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(subcategoriaRepository.findAllById(List.of(1L))).willReturn(List.of(subcategoria));
        given(servicioRepository.save(any(Servicio.class))).willReturn(servicio);
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        assertThat(servicioCatalogoServicio.crearServicio(1L, peticion)).isNotNull();
    }

    @Test
    @DisplayName("crearServicio rechaza publicar si el creador no tiene la identidad verificada")
    void crearServicio_identidadNoVerificada_lanzaExcepcionReglaNegocio() {
        PeticionCrearServicio peticion = PeticionCrearServicio.builder()
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("15.00"))
                .idsSubcategoria(List.of(1L))
                .build();
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(verificacionServicio.estaIdentidadVerificada(1L)).willReturn(false);

        assertThatThrownBy(() -> servicioCatalogoServicio.crearServicio(1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("verificar tu identidad");
        verifyNoInteractions(subcategoriaRepository);
        verify(servicioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearServicio lanza recurso no encontrado si la subcategoria no existe")
    void crearServicio_subcategoriaInexistente() {
        PeticionCrearServicio peticion = PeticionCrearServicio.builder()
                .precioBase(new BigDecimal("10.00")).idsSubcategoria(List.of(99L)).build();
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(subcategoriaRepository.findAllById(List.of(99L))).willReturn(List.of());

        assertThatThrownBy(() -> servicioCatalogoServicio.crearServicio(1L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crearServicio rechaza a un usuario autenticado que no es dueno del perfil ni admin")
    void crearServicio_rechazaUsuarioNoPropietario() {
        autenticarComo("otro@test.com");
        PeticionCrearServicio peticion = PeticionCrearServicio.builder()
                .precioBase(new BigDecimal("10.00")).idsSubcategoria(List.of(1L)).build();
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));

        assertThatThrownBy(() -> servicioCatalogoServicio.crearServicio(1L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("crearServicio permite a un ADMIN gestionar el servicio de otro creador")
    void crearServicio_permiteAdmin() {
        autenticarComo("admin@test.com", "ROLE_ADMIN");
        PeticionCrearServicio peticion = PeticionCrearServicio.builder()
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("10.00")).idsSubcategoria(List.of(1L)).build();
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(subcategoriaRepository.findAllById(List.of(1L))).willReturn(List.of(subcategoria));
        given(servicioRepository.save(any(Servicio.class))).willReturn(servicio);
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        assertThat(servicioCatalogoServicio.crearServicio(1L, peticion)).isNotNull();
    }

    @Test
    @DisplayName("crearServicio aplica los valores por defecto cuando tipoItem, cargoRevisionAdicional y limiteRevisionesBase no vienen informados")
    void crearServicio_camposOpcionalesNulos_aplicaValoresPorDefecto() {
        PeticionCrearServicio peticion = PeticionCrearServicio.builder()
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("15.00"))
                .idsSubcategoria(List.of(1L))
                .build();

        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(subcategoriaRepository.findAllById(List.of(1L))).willReturn(List.of(subcategoria));
        given(servicioRepository.save(any(Servicio.class))).willAnswer(inv -> inv.getArgument(0));
        given(servicioRepository.findById(any())).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findByServicioIdServicio(any())).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(any())).willReturn(List.of());

        ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);
        servicioCatalogoServicio.crearServicio(1L, peticion);

        verify(servicioRepository).save(captor.capture());
        assertThat(captor.getValue().getTipoItem()).isEqualTo("SERVICIO");
        assertThat(captor.getValue().getCargoRevisionAdicional()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(captor.getValue().getLimiteRevisionesBase()).isEqualTo(0);
    }

    @Test
    @DisplayName("crearServicio asocia las etiquetas solicitadas")
    void crearServicio_asociaEtiquetas() {
        PeticionCrearServicio peticion = PeticionCrearServicio.builder()
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("10.00")).idsSubcategoria(List.of(1L))
                .etiquetaIds(List.of(5L))
                .build();
        Etiqueta etiqueta = Etiqueta.builder().idEtiqueta(5L).nombreEtiqueta("Digital").build();

        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(subcategoriaRepository.findAllById(List.of(1L))).willReturn(List.of(subcategoria));
        given(servicioRepository.save(any(Servicio.class))).willReturn(servicio);
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(etiquetaRepository.findAllById(List.of(5L))).willReturn(List.of(etiqueta));
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.crearServicio(1L, peticion);

        verify(servicioEtiquetaRepository).save(any(ServicioEtiqueta.class));
    }

    // ---------- actualizarServicio ----------

    @Test
    @DisplayName("actualizarServicio aplica los cambios permitidos")
    void actualizarServicio_aplicaCambios() {
        PeticionActualizarServicio peticion = PeticionActualizarServicio.builder()
                .tituloServicio("Nuevo titulo")
                .descripcionDetallada("Nueva descripcion detallada con mas de veinte caracteres")
                .precioBase(new BigDecimal("20.00"))
                .estadoPublicacion("PAUSADO")
                .build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioRepository.save(any(Servicio.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        RespuestaServicio respuesta = servicioCatalogoServicio.actualizarServicio(10L, peticion);

        assertThat(respuesta).isNotNull();
        assertThat(servicio.getTituloServicio()).isEqualTo("Nuevo titulo");
        assertThat(servicio.getEstadoPublicacion()).isEqualTo("PAUSADO");
    }

    @Test
    @DisplayName("actualizarServicio rechaza reactivar (ACTIVO) si el creador no tiene la identidad verificada")
    void actualizarServicio_reactivarSinIdentidadVerificada_lanzaExcepcionReglaNegocio() {
        servicio.setEstadoPublicacion("PAUSADO");
        PeticionActualizarServicio peticion = PeticionActualizarServicio.builder()
                .precioBase(new BigDecimal("20.00"))
                .estadoPublicacion("ACTIVO")
                .build();
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(verificacionServicio.estaIdentidadVerificada(1L)).willReturn(false);

        assertThatThrownBy(() -> servicioCatalogoServicio.actualizarServicio(10L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("verificar tu identidad");
        assertThat(servicio.getEstadoPublicacion()).isEqualTo("PAUSADO");
        verify(servicioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarServicio mantiene titulo, descripcion, tipoItem y estadoPublicacion cuando no vienen informados")
    void actualizarServicio_camposOpcionalesNulos_mantieneOriginales() {
        PeticionActualizarServicio peticion = PeticionActualizarServicio.builder()
                .precioBase(new BigDecimal("20.00"))
                .build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioRepository.save(any(Servicio.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.actualizarServicio(10L, peticion);

        assertThat(servicio.getTituloServicio()).isEqualTo("Ilustracion digital");
        assertThat(servicio.getTipoItem()).isEqualTo("SERVICIO");
        assertThat(servicio.getEstadoPublicacion()).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("actualizarServicio ignora titulo, descripcion, tipoItem y estadoPublicacion en blanco")
    void actualizarServicio_camposEnBlanco_seIgnoran() {
        PeticionActualizarServicio peticion = PeticionActualizarServicio.builder()
                .precioBase(new BigDecimal("20.00"))
                .tituloServicio("   ")
                .descripcionDetallada("")
                .tipoItem("  ")
                .estadoPublicacion("")
                .build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioRepository.save(any(Servicio.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.actualizarServicio(10L, peticion);

        assertThat(servicio.getTituloServicio()).isEqualTo("Ilustracion digital");
        assertThat(servicio.getTipoItem()).isEqualTo("SERVICIO");
        assertThat(servicio.getEstadoPublicacion()).isEqualTo("ACTIVO");
        verify(verificacionServicio, never()).estaIdentidadVerificada(any());
    }

    @Test
    @DisplayName("actualizarServicio rechaza precio invalido")
    void actualizarServicio_rechazaPrecioInvalido() {
        PeticionActualizarServicio peticion = PeticionActualizarServicio.builder().precioBase(null).build();

        assertThatThrownBy(() -> servicioCatalogoServicio.actualizarServicio(10L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("actualizarServicio lanza recurso no encontrado si el servicio no existe")
    void actualizarServicio_servicioInexistente() {
        PeticionActualizarServicio peticion = PeticionActualizarServicio.builder().precioBase(new BigDecimal("10.00")).build();
        given(servicioRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicioCatalogoServicio.actualizarServicio(10L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("actualizarServicio reemplaza las subcategorias cuando la peticion las incluye")
    void actualizarServicio_reemplazaSubcategorias() {
        Categoria otraCategoria = Categoria.builder().idCategoria(2L).nombreCategoria("Musica").build();
        Subcategoria otraSubcategoria = Subcategoria.builder().idSubcategoria(2L).categoria(otraCategoria).nombreSubcategoria("Produccion").build();
        PeticionActualizarServicio peticion = PeticionActualizarServicio.builder()
                .precioBase(new BigDecimal("10.00")).idsSubcategoria(List.of(2L)).build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(subcategoriaRepository.findAllById(List.of(2L))).willReturn(List.of(otraSubcategoria));
        given(servicioRepository.save(any(Servicio.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.actualizarServicio(10L, peticion);

        verify(servicioSubcategoriaRepository).deleteByServicioIdServicio(10L);
        verify(servicioSubcategoriaRepository).save(argThat(ss -> ss.getSubcategoria().equals(otraSubcategoria)));
    }

    @Test
    @DisplayName("actualizarServicio rechaza una lista de subcategorias vacia")
    void actualizarServicio_rechazaSubcategoriasVacias() {
        PeticionActualizarServicio peticion = PeticionActualizarServicio.builder()
                .precioBase(new BigDecimal("10.00")).idsSubcategoria(List.of()).build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));

        assertThatThrownBy(() -> servicioCatalogoServicio.actualizarServicio(10L, peticion))
                .isInstanceOf(BusinessRuleException.class);
        verify(servicioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarServicio aplica tipoItem, cargoRevisionAdicional y limiteRevisionesBase cuando vienen informados")
    void actualizarServicio_aplicaCamposAdicionales() {
        PeticionActualizarServicio peticion = PeticionActualizarServicio.builder()
                .precioBase(new BigDecimal("10.00"))
                .tipoItem("PRODUCTO")
                .cargoRevisionAdicional(new BigDecimal("5.00"))
                .limiteRevisionesBase(3)
                .build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioRepository.save(any(Servicio.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.actualizarServicio(10L, peticion);

        assertThat(servicio.getTipoItem()).isEqualTo("PRODUCTO");
        assertThat(servicio.getCargoRevisionAdicional()).isEqualByComparingTo("5.00");
        assertThat(servicio.getLimiteRevisionesBase()).isEqualTo(3);
    }

    @Test
    @DisplayName("actualizarServicio reactiva (ACTIVO) exitosamente cuando el creador si tiene la identidad verificada")
    void actualizarServicio_reactivaConIdentidadVerificada() {
        servicio.setEstadoPublicacion("PAUSADO");
        PeticionActualizarServicio peticion = PeticionActualizarServicio.builder()
                .precioBase(new BigDecimal("10.00"))
                .estadoPublicacion("ACTIVO")
                .build();
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioRepository.save(any(Servicio.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.actualizarServicio(10L, peticion);

        assertThat(servicio.getEstadoPublicacion()).isEqualTo("ACTIVO");
        verify(verificacionServicio).estaIdentidadVerificada(1L);
    }

    @Test
    @DisplayName("actualizarServicio reemplaza las etiquetas cuando la peticion las incluye")
    void actualizarServicio_reemplazaEtiquetas() {
        PeticionActualizarServicio peticion = PeticionActualizarServicio.builder()
                .precioBase(new BigDecimal("10.00")).etiquetaIds(List.of()).build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioRepository.save(any(Servicio.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.actualizarServicio(10L, peticion);

        verify(servicioEtiquetaRepository).deleteByServicioIdServicio(10L);
    }

    // ---------- obtenerServicioPorId / eliminarServicio ----------

    @Test
    @DisplayName("obtenerServicioPorId lanza recurso no encontrado si no existe")
    void obtenerServicioPorId_inexistente() {
        given(servicioRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicioCatalogoServicio.obtenerServicioPorId(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("obtenerServicioPorId usa 'Creador' como nombre por defecto si el perfil no tiene usuario asociado")
    void obtenerServicioPorId_perfilSinUsuario_usaNombrePorDefecto() {
        PerfilCreador perfilSinUsuario = PerfilCreador.builder().idPerfil(2L).usuario(null).build();
        Servicio servicioSinUsuario = Servicio.builder()
                .idServicio(20L).perfil(perfilSinUsuario)
                .tituloServicio("X").precioBase(BigDecimal.TEN).build();
        given(servicioRepository.findById(20L)).willReturn(Optional.of(servicioSinUsuario));
        given(servicioAtributoRepository.findByServicioIdServicio(20L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(20L)).willReturn(List.of());

        RespuestaServicio respuesta = servicioCatalogoServicio.obtenerServicioPorId(20L);

        assertThat(respuesta.getNombreCreador()).isEqualTo("Creador");
    }

    @Test
    @DisplayName("obtenerServicioPorId incluye las etiquetas asociadas")
    void obtenerServicioPorId_incluyeEtiquetas() {
        Etiqueta etiqueta = Etiqueta.builder().idEtiqueta(5L).nombreEtiqueta("Digital").build();
        ServicioEtiqueta se = ServicioEtiqueta.builder().servicio(servicio).etiqueta(etiqueta).build();
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of(se));

        RespuestaServicio respuesta = servicioCatalogoServicio.obtenerServicioPorId(10L);

        assertThat(respuesta.getEtiquetas()).hasSize(1);
        assertThat(respuesta.getEtiquetas().get(0).getNombreEtiqueta()).isEqualTo("Digital");
    }

    @Test
    @DisplayName("eliminarServicio borra el servicio y sus etiquetas asociadas")
    void eliminarServicio_borraServicio() {
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));

        servicioCatalogoServicio.eliminarServicio(10L);

        verify(servicioEtiquetaRepository).deleteByServicioIdServicio(10L);
        verify(servicioRepository).delete(servicio);
    }

    @Test
    @DisplayName("eliminarServicio rechaza a un usuario no propietario")
    void eliminarServicio_rechazaNoPropietario() {
        autenticarComo("otro@test.com");
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));

        assertThatThrownBy(() -> servicioCatalogoServicio.eliminarServicio(10L))
                .isInstanceOf(BusinessRuleException.class);
        verify(servicioRepository, never()).delete(any(Servicio.class));
    }

    // ---------- listarServiciosPorCreador ----------

    @Test
    @DisplayName("listarServiciosPorCreador filtra por estado cuando se indica")
    void listarServiciosPorCreador_filtraPorEstado() {
        given(perfilRepository.existsById(1L)).willReturn(true);
        given(servicioRepository.findByPerfilIdPerfilAndEstadoPublicacion(1L, "ACTIVO")).willReturn(List.of(servicio));
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        List<RespuestaServicioResumido> resultado = servicioCatalogoServicio.listarServiciosPorCreador(1L, "ACTIVO");

        assertThat(resultado).hasSize(1);
        verify(servicioRepository).findByPerfilIdPerfilAndEstadoPublicacion(1L, "ACTIVO");
    }

    @Test
    @DisplayName("listarServiciosPorCreador lista todos cuando el estado viene en blanco")
    void listarServiciosPorCreador_estadoEnBlanco() {
        given(perfilRepository.existsById(1L)).willReturn(true);
        given(servicioRepository.findByPerfilIdPerfil(1L)).willReturn(List.of(servicio));
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        assertThat(servicioCatalogoServicio.listarServiciosPorCreador(1L, "   ")).hasSize(1);
        verify(servicioRepository, never()).findByPerfilIdPerfilAndEstadoPublicacion(any(), any());
    }

    @Test
    @DisplayName("listarServiciosPorCreador lista todos cuando no se indica estado")
    void listarServiciosPorCreador_sinFiltro() {
        given(perfilRepository.existsById(1L)).willReturn(true);
        given(servicioRepository.findByPerfilIdPerfil(1L)).willReturn(List.of(servicio));
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        assertThat(servicioCatalogoServicio.listarServiciosPorCreador(1L, null)).hasSize(1);
    }

    @Test
    @DisplayName("listarServiciosPorCreador incluye las etiquetas de cada servicio")
    void listarServiciosPorCreador_incluyeEtiquetas() {
        Etiqueta etiqueta = Etiqueta.builder().idEtiqueta(5L).nombreEtiqueta("Digital").build();
        ServicioEtiqueta se = ServicioEtiqueta.builder().servicio(servicio).etiqueta(etiqueta).build();
        given(perfilRepository.existsById(1L)).willReturn(true);
        given(servicioRepository.findByPerfilIdPerfil(1L)).willReturn(List.of(servicio));
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of(se));

        List<RespuestaServicioResumido> resultado = servicioCatalogoServicio.listarServiciosPorCreador(1L, null);

        assertThat(resultado.get(0).getEtiquetas()).hasSize(1);
        assertThat(resultado.get(0).getEtiquetas().get(0).getNombreEtiqueta()).isEqualTo("Digital");
    }

    @Test
    @DisplayName("listarServiciosPorCreador lanza recurso no encontrado si el perfil no existe")
    void listarServiciosPorCreador_perfilInexistente() {
        given(perfilRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> servicioCatalogoServicio.listarServiciosPorCreador(1L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- buscarCatalogoServicios ----------

    @Test
    @DisplayName("buscarCatalogoServicios aplica orden por precio ascendente")
    void buscarCatalogoServicios_ordenaPorPrecioAsc() {
        Page<Servicio> pagina = new PageImpl<>(List.of(servicio));
        given(servicioRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(pagina);
        given(servicioEtiquetaRepository.findByServicioIdServicioIn(List.of(10L))).willReturn(List.of());

        Page<RespuestaServicioResumido> resultado = servicioCatalogoServicio.buscarCatalogoServicios(
                null, null, null, null, null, null, "precioAsc", 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("buscarCatalogoServicios usa el orden por defecto cuando no se indica sort")
    void buscarCatalogoServicios_ordenPorDefecto() {
        Page<Servicio> pagina = new PageImpl<>(List.of(servicio));
        given(servicioRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(pagina);
        given(servicioEtiquetaRepository.findByServicioIdServicioIn(List.of(10L))).willReturn(List.of());

        Page<RespuestaServicioResumido> resultado = servicioCatalogoServicio.buscarCatalogoServicios(
                1L, 1L, BigDecimal.ONE, BigDecimal.TEN, List.of(5L), "ilustracion", null, 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("buscarCatalogoServicios aplica orden por precio descendente")
    void buscarCatalogoServicios_ordenaPorPrecioDesc() {
        Page<Servicio> pagina = new PageImpl<>(List.of(servicio));
        given(servicioRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(pagina);
        given(servicioEtiquetaRepository.findByServicioIdServicioIn(List.of(10L))).willReturn(List.of());

        Page<RespuestaServicioResumido> resultado = servicioCatalogoServicio.buscarCatalogoServicios(
                null, null, null, null, null, null, "precioDesc", 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("buscarCatalogoServicios aplica orden por titulo ascendente")
    void buscarCatalogoServicios_ordenaPorTitulo() {
        Page<Servicio> pagina = new PageImpl<>(List.of(servicio));
        given(servicioRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(pagina);
        given(servicioEtiquetaRepository.findByServicioIdServicioIn(List.of(10L))).willReturn(List.of());

        Page<RespuestaServicioResumido> resultado = servicioCatalogoServicio.buscarCatalogoServicios(
                null, null, null, null, null, null, "tituloServicio,asc", 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("buscarCatalogoServicios incluye las etiquetas de cada servicio de la pagina")
    void buscarCatalogoServicios_incluyeEtiquetas() {
        Etiqueta etiqueta = Etiqueta.builder().idEtiqueta(5L).nombreEtiqueta("Digital").build();
        ServicioEtiqueta se = ServicioEtiqueta.builder().servicio(servicio).etiqueta(etiqueta).build();
        Page<Servicio> pagina = new PageImpl<>(List.of(servicio));
        given(servicioRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(pagina);
        given(servicioEtiquetaRepository.findByServicioIdServicioIn(List.of(10L))).willReturn(List.of(se));

        Page<RespuestaServicioResumido> resultado = servicioCatalogoServicio.buscarCatalogoServicios(
                null, null, null, null, null, null, null, 0, 10);

        assertThat(resultado.getContent().get(0).getEtiquetas()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEtiquetas().get(0).getNombreEtiqueta()).isEqualTo("Digital");
    }

    // ---------- Atributos dinamicos ----------

    @Test
    @DisplayName("listarAtributosPorServicio devuelve los atributos existentes")
    void listarAtributosPorServicio_devuelveAtributos() {
        AtributoDinamico atributo = AtributoDinamico.builder().idAtributo(1L).nombreAtributo("Color").tipoDato("TEXTO").build();
        ServicioAtributo sa = ServicioAtributo.builder().idServicioAtributo(1L).servicio(servicio).atributo(atributo).valorAsignado("Rojo").build();
        given(servicioRepository.existsById(10L)).willReturn(true);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of(sa));

        assertThat(servicioCatalogoServicio.listarAtributosPorServicio(10L)).hasSize(1);
    }

    @Test
    @DisplayName("listarAtributosPorServicio lanza recurso no encontrado si el servicio no existe")
    void listarAtributosPorServicio_servicioInexistente() {
        given(servicioRepository.existsById(10L)).willReturn(false);

        assertThatThrownBy(() -> servicioCatalogoServicio.listarAtributosPorServicio(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("agregarAtributo reutiliza un atributo dinamico existente")
    void agregarAtributo_reutilizaExistente() {
        AtributoDinamico atributo = AtributoDinamico.builder().idAtributo(1L).nombreAtributo("Color").tipoDato("TEXTO").build();
        PeticionCrearAtributo peticion = PeticionCrearAtributo.builder()
                .nombreAtributo("Color").valorAsignado("Rojo").tipoDato("TEXTO").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.countByServicioIdServicio(10L)).willReturn(2L);
        given(atributoRepository.findByNombreAtributoIgnoreCase("Color")).willReturn(Optional.of(atributo));
        given(servicioAtributoRepository.findByServicioIdServicioAndAtributoIdAtributo(10L, 1L)).willReturn(Optional.empty());
        given(servicioAtributoRepository.save(any(ServicioAtributo.class)))
                .willAnswer(inv -> inv.getArgument(0));

        RespuestaAtributo respuesta = servicioCatalogoServicio.agregarAtributo(10L, peticion);

        assertThat(respuesta.getNombreAtributo()).isEqualTo("Color");
        verify(atributoRepository, never()).save(any());
    }

    @Test
    @DisplayName("agregarAtributo crea un atributo dinamico nuevo si no existe")
    void agregarAtributo_creaAtributoNuevo() {
        PeticionCrearAtributo peticion = PeticionCrearAtributo.builder()
                .nombreAtributo("Formato").valorAsignado("PDF").tipoDato("TEXTO").build();
        AtributoDinamico nuevo = AtributoDinamico.builder().idAtributo(2L).nombreAtributo("Formato").tipoDato("TEXTO").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.countByServicioIdServicio(10L)).willReturn(0L);
        given(atributoRepository.findByNombreAtributoIgnoreCase("Formato")).willReturn(Optional.empty());
        given(atributoRepository.save(any(AtributoDinamico.class))).willReturn(nuevo);
        given(servicioAtributoRepository.findByServicioIdServicioAndAtributoIdAtributo(10L, 2L)).willReturn(Optional.empty());
        given(servicioAtributoRepository.save(any(ServicioAtributo.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaAtributo respuesta = servicioCatalogoServicio.agregarAtributo(10L, peticion);

        assertThat(respuesta.getNombreAtributo()).isEqualTo("Formato");
    }

    @Test
    @DisplayName("agregarAtributo rechaza el limite de 10 atributos por servicio")
    void agregarAtributo_rechazaLimite() {
        PeticionCrearAtributo peticion = PeticionCrearAtributo.builder()
                .nombreAtributo("Color").valorAsignado("Rojo").tipoDato("TEXTO").build();
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.countByServicioIdServicio(10L)).willReturn(10L);

        assertThatThrownBy(() -> servicioCatalogoServicio.agregarAtributo(10L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("agregarAtributo rechaza un atributo ya asociado al servicio")
    void agregarAtributo_rechazaDuplicado() {
        AtributoDinamico atributo = AtributoDinamico.builder().idAtributo(1L).nombreAtributo("Color").tipoDato("TEXTO").build();
        ServicioAtributo existente = ServicioAtributo.builder().idServicioAtributo(1L).servicio(servicio).atributo(atributo).valorAsignado("Rojo").build();
        PeticionCrearAtributo peticion = PeticionCrearAtributo.builder()
                .nombreAtributo("Color").valorAsignado("Azul").tipoDato("TEXTO").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.countByServicioIdServicio(10L)).willReturn(1L);
        given(atributoRepository.findByNombreAtributoIgnoreCase("Color")).willReturn(Optional.of(atributo));
        given(servicioAtributoRepository.findByServicioIdServicioAndAtributoIdAtributo(10L, 1L)).willReturn(Optional.of(existente));

        assertThatThrownBy(() -> servicioCatalogoServicio.agregarAtributo(10L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("actualizarAtributo cambia el valor asignado")
    void actualizarAtributo_cambiaValor() {
        AtributoDinamico atributo = AtributoDinamico.builder().idAtributo(1L).nombreAtributo("Color").tipoDato("TEXTO").build();
        ServicioAtributo sa = ServicioAtributo.builder().idServicioAtributo(1L).servicio(servicio).atributo(atributo).valorAsignado("Rojo").build();
        PeticionActualizarAtributo peticion = PeticionActualizarAtributo.builder().valorAsignado("Azul").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findById(1L)).willReturn(Optional.of(sa));
        given(servicioAtributoRepository.save(any(ServicioAtributo.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaAtributo respuesta = servicioCatalogoServicio.actualizarAtributo(10L, 1L, peticion);

        assertThat(respuesta.getValorAsignado()).isEqualTo("Azul");
    }

    @Test
    @DisplayName("actualizarAtributo rechaza un atributo que no pertenece al servicio")
    void actualizarAtributo_rechazaAtributoDeOtroServicio() {
        Servicio otroServicio = Servicio.builder().idServicio(20L).build();
        AtributoDinamico atributo = AtributoDinamico.builder().idAtributo(1L).build();
        ServicioAtributo sa = ServicioAtributo.builder().idServicioAtributo(1L).servicio(otroServicio).atributo(atributo).valorAsignado("Rojo").build();
        PeticionActualizarAtributo peticion = PeticionActualizarAtributo.builder().valorAsignado("Azul").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findById(1L)).willReturn(Optional.of(sa));

        assertThatThrownBy(() -> servicioCatalogoServicio.actualizarAtributo(10L, 1L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("actualizarAtributo lanza recurso no encontrado si el servicio-atributo no existe")
    void actualizarAtributo_servicioAtributoInexistente() {
        PeticionActualizarAtributo peticion = PeticionActualizarAtributo.builder().valorAsignado("Azul").build();
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicioCatalogoServicio.actualizarAtributo(10L, 1L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("eliminarAtributo borra la asociacion cuando pertenece al servicio")
    void eliminarAtributo_borraAsociacion() {
        AtributoDinamico atributo = AtributoDinamico.builder().idAtributo(1L).build();
        ServicioAtributo sa = ServicioAtributo.builder().idServicioAtributo(1L).servicio(servicio).atributo(atributo).valorAsignado("Rojo").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findById(1L)).willReturn(Optional.of(sa));

        servicioCatalogoServicio.eliminarAtributo(10L, 1L);

        verify(servicioAtributoRepository).delete(sa);
    }

    @Test
    @DisplayName("eliminarAtributo rechaza un atributo que no pertenece al servicio")
    void eliminarAtributo_rechazaAtributoDeOtroServicio() {
        Servicio otroServicio = Servicio.builder().idServicio(20L).build();
        AtributoDinamico atributo = AtributoDinamico.builder().idAtributo(1L).build();
        ServicioAtributo sa = ServicioAtributo.builder().idServicioAtributo(1L).servicio(otroServicio).atributo(atributo).valorAsignado("Rojo").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findById(1L)).willReturn(Optional.of(sa));

        assertThatThrownBy(() -> servicioCatalogoServicio.eliminarAtributo(10L, 1L))
                .isInstanceOf(BusinessRuleException.class);
        verify(servicioAtributoRepository, never()).delete(any());
    }

    // ---------- quitarSubcategoria ----------

    @Test
    @DisplayName("quitarSubcategoria borra la asociacion cuando el servicio tiene mas de una")
    void quitarSubcategoria_borraCuandoQuedaAlMenosUna() {
        given(servicioRepository.existsById(10L)).willReturn(true);
        given(servicioSubcategoriaRepository.countByServicioIdServicio(10L)).willReturn(2L);
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.quitarSubcategoria(10L, 1L);

        verify(servicioSubcategoriaRepository).deleteByServicioIdServicioAndSubcategoriaIdSubcategoria(10L, 1L);
    }

    @Test
    @DisplayName("quitarSubcategoria rechaza si el servicio se quedaria sin ninguna")
    void quitarSubcategoria_rechazaSiQuedaSinNinguna() {
        given(servicioRepository.existsById(10L)).willReturn(true);
        given(servicioSubcategoriaRepository.countByServicioIdServicio(10L)).willReturn(1L);

        assertThatThrownBy(() -> servicioCatalogoServicio.quitarSubcategoria(10L, 1L))
                .isInstanceOf(BusinessRuleException.class);
        verify(servicioSubcategoriaRepository, never()).deleteByServicioIdServicioAndSubcategoriaIdSubcategoria(any(), any());
    }

    private void autenticarComo(String correo, String... authorities) {
        List<SimpleGrantedAuthority> roles = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        var auth = new UsernamePasswordAuthenticationToken(correo, "N/A", roles);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
