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
import uteq.edu.ec.artisync.dto.peticion.catalogo.UpdateAttributeRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.UpdateOfferingRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateAttributeRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateOfferingRequest;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.AttributeResponse;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.OfferingResponse;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.OfferingSummaryResponse;
import uteq.edu.ec.artisync.entity.catalogo.*;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.*;
import uteq.edu.ec.artisync.repository.perfil.CreatorProfileRepository;
import uteq.edu.ec.artisync.service.perfil.IVerificationService;

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
 * Pruebas unitarias de {@link OfferingCatalogServiceImpl}: alta, edición,
 * borrado y atributos dinámicos del catálogo, con la autorización de
 * propiedad-o-admin verificada explícitamente porque decide si se lanza
 * {@link BusinessRuleException}.
 */
@ExtendWith(MockitoExtension.class)
class OfferingCatalogServiceImplTest {

    @Mock private OfferingRepository servicioRepository;
    @Mock private CreatorProfileRepository perfilRepository;
    @Mock private SubcategoryRepository subcategoriaRepository;
    @Mock private DynamicAttributeRepository atributoRepository;
    @Mock private OfferingAttributeRepository servicioAtributoRepository;
    @Mock private TagRepository etiquetaRepository;
    @Mock private OfferingTagRepository servicioEtiquetaRepository;
    @Mock private OfferingSubcategoryRepository servicioSubcategoriaRepository;
    @Mock private IVerificationService verificacionServicio;

    @InjectMocks
    private OfferingCatalogServiceImpl servicioCatalogoServicio;

    private User usuario;
    private CreatorProfile perfil;
    private Category categoria;
    private Subcategory subcategoria;
    private Offering servicio;

    @BeforeEach
    void setUp() {
        usuario = User.builder().idUsuario(1L).nombres("Ana").apellidos("Diaz").correo("ana@test.com").build();
        perfil = CreatorProfile.builder().idPerfil(1L).usuario(usuario).build();
        categoria = Category.builder().idCategoria(1L).nombreCategoria("Arte").build();
        subcategoria = Subcategory.builder().idSubcategoria(1L).categoria(categoria).nombreSubcategoria("Ilustracion").build();
        servicio = Offering.builder()
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
        lenient().when(verificacionServicio.isIdentityVerified(anyLong())).thenReturn(true);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // ---------- createOffering ----------

    @Test
    @DisplayName("createOffering guarda el servicio cuando el precio y las referencias son validas")
    void crearServicio_guardaCuandoEsValido() {
        CreateOfferingRequest peticion = CreateOfferingRequest.builder()
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("15.00"))
                .idsSubcategoria(List.of(1L))
                .tipoItem("SERVICIO")
                .build();

        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(subcategoriaRepository.findAllById(List.of(1L))).willReturn(List.of(subcategoria));
        given(servicioRepository.save(any(Offering.class))).willReturn(servicio);
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        OfferingResponse respuesta = servicioCatalogoServicio.createOffering(1L, peticion);

        assertThat(respuesta.getIdServicio()).isEqualTo(10L);
        assertThat(respuesta.getTituloServicio()).isEqualTo("Ilustracion digital");
        verify(servicioRepository).save(any(Offering.class));
    }

    @Test
    @DisplayName("createOffering rechaza precio nulo o menor a 0.01")
    void crearServicio_rechazaPrecioInvalido() {
        CreateOfferingRequest peticion = CreateOfferingRequest.builder().precioBase(new BigDecimal("0.00")).build();

        assertThatThrownBy(() -> servicioCatalogoServicio.createOffering(1L, peticion))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(perfilRepository);
    }

    @Test
    @DisplayName("createOffering lanza recurso no encontrado si el perfil creador no existe")
    void crearServicio_perfilInexistente() {
        CreateOfferingRequest peticion = CreateOfferingRequest.builder().precioBase(new BigDecimal("10.00")).build();
        given(perfilRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicioCatalogoServicio.createOffering(1L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createOffering rechaza publicar si el perfil no tiene usuario asociado")
    void crearServicio_perfilSinUsuario_lanzaExcepcionReglaNegocio() {
        CreatorProfile perfilSinUsuario = CreatorProfile.builder().idPerfil(2L).usuario(null).build();
        CreateOfferingRequest peticion = CreateOfferingRequest.builder()
                .tituloServicio("X").descripcionDetallada("Descripcion de mas de veinte caracteres")
                .precioBase(new BigDecimal("15.00")).idsSubcategoria(List.of(1L)).build();
        given(perfilRepository.findById(2L)).willReturn(Optional.of(perfilSinUsuario));

        assertThatThrownBy(() -> servicioCatalogoServicio.createOffering(2L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("verificar tu identidad");
    }

    @Test
    @DisplayName("createOffering permite gestionar cuando la autenticacion no esta autenticada (p.ej. anonima)")
    void crearServicio_autenticacionNoAutenticada_noRechaza() {
        var authNoAutenticado = new UsernamePasswordAuthenticationToken("nadie@test.com", "N/A");
        authNoAutenticado.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authNoAutenticado);

        CreateOfferingRequest peticion = CreateOfferingRequest.builder()
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("15.00")).idsSubcategoria(List.of(1L)).build();
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(subcategoriaRepository.findAllById(List.of(1L))).willReturn(List.of(subcategoria));
        given(servicioRepository.save(any(Offering.class))).willReturn(servicio);
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        assertThat(servicioCatalogoServicio.createOffering(1L, peticion)).isNotNull();
    }

    @Test
    @DisplayName("createOffering rechaza publicar si el creador no tiene la identidad verificada")
    void crearServicio_identidadNoVerificada_lanzaExcepcionReglaNegocio() {
        CreateOfferingRequest peticion = CreateOfferingRequest.builder()
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("15.00"))
                .idsSubcategoria(List.of(1L))
                .build();
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(verificacionServicio.isIdentityVerified(1L)).willReturn(false);

        assertThatThrownBy(() -> servicioCatalogoServicio.createOffering(1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("verificar tu identidad");
        verifyNoInteractions(subcategoriaRepository);
        verify(servicioRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOffering lanza recurso no encontrado si la subcategoria no existe")
    void crearServicio_subcategoriaInexistente() {
        CreateOfferingRequest peticion = CreateOfferingRequest.builder()
                .precioBase(new BigDecimal("10.00")).idsSubcategoria(List.of(99L)).build();
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(subcategoriaRepository.findAllById(List.of(99L))).willReturn(List.of());

        assertThatThrownBy(() -> servicioCatalogoServicio.createOffering(1L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createOffering rechaza a un usuario autenticado que no es dueno del perfil ni admin")
    void crearServicio_rechazaUsuarioNoPropietario() {
        autenticarComo("otro@test.com");
        CreateOfferingRequest peticion = CreateOfferingRequest.builder()
                .precioBase(new BigDecimal("10.00")).idsSubcategoria(List.of(1L)).build();
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));

        assertThatThrownBy(() -> servicioCatalogoServicio.createOffering(1L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("createOffering permite a un ADMIN gestionar el servicio de otro creador")
    void crearServicio_permiteAdmin() {
        autenticarComo("admin@test.com", "ROLE_ADMIN");
        CreateOfferingRequest peticion = CreateOfferingRequest.builder()
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("10.00")).idsSubcategoria(List.of(1L)).build();
        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(subcategoriaRepository.findAllById(List.of(1L))).willReturn(List.of(subcategoria));
        given(servicioRepository.save(any(Offering.class))).willReturn(servicio);
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        assertThat(servicioCatalogoServicio.createOffering(1L, peticion)).isNotNull();
    }

    @Test
    @DisplayName("createOffering aplica los valores por defecto cuando tipoItem, cargoRevisionAdicional y limiteRevisionesBase no vienen informados")
    void crearServicio_camposOpcionalesNulos_aplicaValoresPorDefecto() {
        CreateOfferingRequest peticion = CreateOfferingRequest.builder()
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("15.00"))
                .idsSubcategoria(List.of(1L))
                .build();

        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(subcategoriaRepository.findAllById(List.of(1L))).willReturn(List.of(subcategoria));
        given(servicioRepository.save(any(Offering.class))).willAnswer(inv -> inv.getArgument(0));
        given(servicioRepository.findById(any())).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findByServicioIdServicio(any())).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(any())).willReturn(List.of());

        ArgumentCaptor<Offering> captor = ArgumentCaptor.forClass(Offering.class);
        servicioCatalogoServicio.createOffering(1L, peticion);

        verify(servicioRepository).save(captor.capture());
        assertThat(captor.getValue().getTipoItem()).isEqualTo("SERVICIO");
        assertThat(captor.getValue().getCargoRevisionAdicional()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(captor.getValue().getLimiteRevisionesBase()).isEqualTo(0);
    }

    @Test
    @DisplayName("createOffering asocia las etiquetas solicitadas")
    void crearServicio_asociaEtiquetas() {
        CreateOfferingRequest peticion = CreateOfferingRequest.builder()
                .tituloServicio("Ilustracion digital")
                .descripcionDetallada("Descripcion detallada de ejemplo con mas de veinte caracteres")
                .precioBase(new BigDecimal("10.00")).idsSubcategoria(List.of(1L))
                .etiquetaIds(List.of(5L))
                .build();
        Tag etiqueta = Tag.builder().idEtiqueta(5L).nombreEtiqueta("Digital").build();

        given(perfilRepository.findById(1L)).willReturn(Optional.of(perfil));
        given(subcategoriaRepository.findAllById(List.of(1L))).willReturn(List.of(subcategoria));
        given(servicioRepository.save(any(Offering.class))).willReturn(servicio);
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(etiquetaRepository.findAllById(List.of(5L))).willReturn(List.of(etiqueta));
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.createOffering(1L, peticion);

        verify(servicioEtiquetaRepository).save(any(OfferingTag.class));
    }

    // ---------- updateOffering ----------

    @Test
    @DisplayName("updateOffering aplica los cambios permitidos")
    void actualizarServicio_aplicaCambios() {
        UpdateOfferingRequest peticion = UpdateOfferingRequest.builder()
                .tituloServicio("Nuevo titulo")
                .descripcionDetallada("Nueva descripcion detallada con mas de veinte caracteres")
                .precioBase(new BigDecimal("20.00"))
                .estadoPublicacion("PAUSADO")
                .build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioRepository.save(any(Offering.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        OfferingResponse respuesta = servicioCatalogoServicio.updateOffering(10L, peticion);

        assertThat(respuesta).isNotNull();
        assertThat(servicio.getTituloServicio()).isEqualTo("Nuevo titulo");
        assertThat(servicio.getEstadoPublicacion()).isEqualTo("PAUSADO");
    }

    @Test
    @DisplayName("updateOffering rechaza reactivar (ACTIVO) si el creador no tiene la identidad verificada")
    void actualizarServicio_reactivarSinIdentidadVerificada_lanzaExcepcionReglaNegocio() {
        servicio.setEstadoPublicacion("PAUSADO");
        UpdateOfferingRequest peticion = UpdateOfferingRequest.builder()
                .precioBase(new BigDecimal("20.00"))
                .estadoPublicacion("ACTIVO")
                .build();
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(verificacionServicio.isIdentityVerified(1L)).willReturn(false);

        assertThatThrownBy(() -> servicioCatalogoServicio.updateOffering(10L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("verificar tu identidad");
        assertThat(servicio.getEstadoPublicacion()).isEqualTo("PAUSADO");
        verify(servicioRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateOffering mantiene titulo, descripcion, tipoItem y estadoPublicacion cuando no vienen informados")
    void actualizarServicio_camposOpcionalesNulos_mantieneOriginales() {
        UpdateOfferingRequest peticion = UpdateOfferingRequest.builder()
                .precioBase(new BigDecimal("20.00"))
                .build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioRepository.save(any(Offering.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.updateOffering(10L, peticion);

        assertThat(servicio.getTituloServicio()).isEqualTo("Ilustracion digital");
        assertThat(servicio.getTipoItem()).isEqualTo("SERVICIO");
        assertThat(servicio.getEstadoPublicacion()).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("updateOffering ignora titulo, descripcion, tipoItem y estadoPublicacion en blanco")
    void actualizarServicio_camposEnBlanco_seIgnoran() {
        UpdateOfferingRequest peticion = UpdateOfferingRequest.builder()
                .precioBase(new BigDecimal("20.00"))
                .tituloServicio("   ")
                .descripcionDetallada("")
                .tipoItem("  ")
                .estadoPublicacion("")
                .build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioRepository.save(any(Offering.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.updateOffering(10L, peticion);

        assertThat(servicio.getTituloServicio()).isEqualTo("Ilustracion digital");
        assertThat(servicio.getTipoItem()).isEqualTo("SERVICIO");
        assertThat(servicio.getEstadoPublicacion()).isEqualTo("ACTIVO");
        verify(verificacionServicio, never()).isIdentityVerified(any());
    }

    @Test
    @DisplayName("updateOffering rechaza precio invalido")
    void actualizarServicio_rechazaPrecioInvalido() {
        UpdateOfferingRequest peticion = UpdateOfferingRequest.builder().precioBase(null).build();

        assertThatThrownBy(() -> servicioCatalogoServicio.updateOffering(10L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("updateOffering lanza recurso no encontrado si el servicio no existe")
    void actualizarServicio_servicioInexistente() {
        UpdateOfferingRequest peticion = UpdateOfferingRequest.builder().precioBase(new BigDecimal("10.00")).build();
        given(servicioRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicioCatalogoServicio.updateOffering(10L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateOffering reemplaza las subcategorias cuando la peticion las incluye")
    void actualizarServicio_reemplazaSubcategorias() {
        Category otraCategoria = Category.builder().idCategoria(2L).nombreCategoria("Musica").build();
        Subcategory otraSubcategoria = Subcategory.builder().idSubcategoria(2L).categoria(otraCategoria).nombreSubcategoria("Produccion").build();
        UpdateOfferingRequest peticion = UpdateOfferingRequest.builder()
                .precioBase(new BigDecimal("10.00")).idsSubcategoria(List.of(2L)).build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(subcategoriaRepository.findAllById(List.of(2L))).willReturn(List.of(otraSubcategoria));
        given(servicioRepository.save(any(Offering.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.updateOffering(10L, peticion);

        verify(servicioSubcategoriaRepository).deleteByServicioIdServicio(10L);
        verify(servicioSubcategoriaRepository).save(argThat(ss -> ss.getSubcategoria().equals(otraSubcategoria)));
    }

    @Test
    @DisplayName("updateOffering rechaza una lista de subcategorias vacia")
    void actualizarServicio_rechazaSubcategoriasVacias() {
        UpdateOfferingRequest peticion = UpdateOfferingRequest.builder()
                .precioBase(new BigDecimal("10.00")).idsSubcategoria(List.of()).build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));

        assertThatThrownBy(() -> servicioCatalogoServicio.updateOffering(10L, peticion))
                .isInstanceOf(BusinessRuleException.class);
        verify(servicioRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateOffering aplica tipoItem, cargoRevisionAdicional y limiteRevisionesBase cuando vienen informados")
    void actualizarServicio_aplicaCamposAdicionales() {
        UpdateOfferingRequest peticion = UpdateOfferingRequest.builder()
                .precioBase(new BigDecimal("10.00"))
                .tipoItem("PRODUCTO")
                .cargoRevisionAdicional(new BigDecimal("5.00"))
                .limiteRevisionesBase(3)
                .build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioRepository.save(any(Offering.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.updateOffering(10L, peticion);

        assertThat(servicio.getTipoItem()).isEqualTo("PRODUCTO");
        assertThat(servicio.getCargoRevisionAdicional()).isEqualByComparingTo("5.00");
        assertThat(servicio.getLimiteRevisionesBase()).isEqualTo(3);
    }

    @Test
    @DisplayName("updateOffering reactiva (ACTIVO) exitosamente cuando el creador si tiene la identidad verificada")
    void actualizarServicio_reactivaConIdentidadVerificada() {
        servicio.setEstadoPublicacion("PAUSADO");
        UpdateOfferingRequest peticion = UpdateOfferingRequest.builder()
                .precioBase(new BigDecimal("10.00"))
                .estadoPublicacion("ACTIVO")
                .build();
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioRepository.save(any(Offering.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.updateOffering(10L, peticion);

        assertThat(servicio.getEstadoPublicacion()).isEqualTo("ACTIVO");
        verify(verificacionServicio).isIdentityVerified(1L);
    }

    @Test
    @DisplayName("updateOffering reemplaza las etiquetas cuando la peticion las incluye")
    void actualizarServicio_reemplazaEtiquetas() {
        UpdateOfferingRequest peticion = UpdateOfferingRequest.builder()
                .precioBase(new BigDecimal("10.00")).etiquetaIds(List.of()).build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioRepository.save(any(Offering.class))).willReturn(servicio);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.updateOffering(10L, peticion);

        verify(servicioEtiquetaRepository).deleteByServicioIdServicio(10L);
    }

    // ---------- getOfferingById / deleteOffering ----------

    @Test
    @DisplayName("getOfferingById lanza recurso no encontrado si no existe")
    void obtenerServicioPorId_inexistente() {
        given(servicioRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicioCatalogoServicio.getOfferingById(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOfferingById usa 'Creador' como nombre por defecto si el perfil no tiene usuario asociado")
    void obtenerServicioPorId_perfilSinUsuario_usaNombrePorDefecto() {
        CreatorProfile perfilSinUsuario = CreatorProfile.builder().idPerfil(2L).usuario(null).build();
        Offering servicioSinUsuario = Offering.builder()
                .idServicio(20L).perfil(perfilSinUsuario)
                .tituloServicio("X").precioBase(BigDecimal.TEN).build();
        given(servicioRepository.findById(20L)).willReturn(Optional.of(servicioSinUsuario));
        given(servicioAtributoRepository.findByServicioIdServicio(20L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(20L)).willReturn(List.of());

        OfferingResponse respuesta = servicioCatalogoServicio.getOfferingById(20L);

        assertThat(respuesta.getNombreCreador()).isEqualTo("Creador");
    }

    @Test
    @DisplayName("getOfferingById incluye las etiquetas asociadas")
    void obtenerServicioPorId_incluyeEtiquetas() {
        Tag etiqueta = Tag.builder().idEtiqueta(5L).nombreEtiqueta("Digital").build();
        OfferingTag se = OfferingTag.builder().servicio(servicio).etiqueta(etiqueta).build();
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of(se));

        OfferingResponse respuesta = servicioCatalogoServicio.getOfferingById(10L);

        assertThat(respuesta.getEtiquetas()).hasSize(1);
        assertThat(respuesta.getEtiquetas().get(0).getNombreEtiqueta()).isEqualTo("Digital");
    }

    @Test
    @DisplayName("deleteOffering borra el servicio y sus etiquetas asociadas")
    void eliminarServicio_borraServicio() {
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));

        servicioCatalogoServicio.deleteOffering(10L);

        verify(servicioEtiquetaRepository).deleteByServicioIdServicio(10L);
        verify(servicioRepository).delete(servicio);
    }

    @Test
    @DisplayName("deleteOffering rechaza a un usuario no propietario")
    void eliminarServicio_rechazaNoPropietario() {
        autenticarComo("otro@test.com");
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));

        assertThatThrownBy(() -> servicioCatalogoServicio.deleteOffering(10L))
                .isInstanceOf(BusinessRuleException.class);
        verify(servicioRepository, never()).delete(any(Offering.class));
    }

    // ---------- listOfferingsByCreator ----------

    @Test
    @DisplayName("listOfferingsByCreator filtra por estado cuando se indica")
    void listarServiciosPorCreador_filtraPorEstado() {
        given(perfilRepository.existsById(1L)).willReturn(true);
        given(servicioRepository.findByPerfilIdPerfilAndEstadoPublicacion(1L, "ACTIVO")).willReturn(List.of(servicio));
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        List<OfferingSummaryResponse> resultado = servicioCatalogoServicio.listOfferingsByCreator(1L, "ACTIVO");

        assertThat(resultado).hasSize(1);
        verify(servicioRepository).findByPerfilIdPerfilAndEstadoPublicacion(1L, "ACTIVO");
    }

    @Test
    @DisplayName("listOfferingsByCreator lista todos cuando el estado viene en blanco")
    void listarServiciosPorCreador_estadoEnBlanco() {
        given(perfilRepository.existsById(1L)).willReturn(true);
        given(servicioRepository.findByPerfilIdPerfil(1L)).willReturn(List.of(servicio));
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        assertThat(servicioCatalogoServicio.listOfferingsByCreator(1L, "   ")).hasSize(1);
        verify(servicioRepository, never()).findByPerfilIdPerfilAndEstadoPublicacion(any(), any());
    }

    @Test
    @DisplayName("listOfferingsByCreator lista todos cuando no se indica estado")
    void listarServiciosPorCreador_sinFiltro() {
        given(perfilRepository.existsById(1L)).willReturn(true);
        given(servicioRepository.findByPerfilIdPerfil(1L)).willReturn(List.of(servicio));
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        assertThat(servicioCatalogoServicio.listOfferingsByCreator(1L, null)).hasSize(1);
    }

    @Test
    @DisplayName("listOfferingsByCreator incluye las etiquetas de cada servicio")
    void listarServiciosPorCreador_incluyeEtiquetas() {
        Tag etiqueta = Tag.builder().idEtiqueta(5L).nombreEtiqueta("Digital").build();
        OfferingTag se = OfferingTag.builder().servicio(servicio).etiqueta(etiqueta).build();
        given(perfilRepository.existsById(1L)).willReturn(true);
        given(servicioRepository.findByPerfilIdPerfil(1L)).willReturn(List.of(servicio));
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of(se));

        List<OfferingSummaryResponse> resultado = servicioCatalogoServicio.listOfferingsByCreator(1L, null);

        assertThat(resultado.get(0).getEtiquetas()).hasSize(1);
        assertThat(resultado.get(0).getEtiquetas().get(0).getNombreEtiqueta()).isEqualTo("Digital");
    }

    @Test
    @DisplayName("listOfferingsByCreator lanza recurso no encontrado si el perfil no existe")
    void listarServiciosPorCreador_perfilInexistente() {
        given(perfilRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> servicioCatalogoServicio.listOfferingsByCreator(1L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- searchCatalogOfferings ----------

    @Test
    @DisplayName("searchCatalogOfferings aplica orden por precio ascendente")
    void buscarCatalogoServicios_ordenaPorPrecioAsc() {
        Page<Offering> pagina = new PageImpl<>(List.of(servicio));
        given(servicioRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(pagina);
        given(servicioEtiquetaRepository.findByServicioIdServicioIn(List.of(10L))).willReturn(List.of());

        Page<OfferingSummaryResponse> resultado = servicioCatalogoServicio.searchCatalogOfferings(
                null, null, null, null, null, null, "precioAsc", 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("searchCatalogOfferings usa el orden por defecto cuando no se indica sort")
    void buscarCatalogoServicios_ordenPorDefecto() {
        Page<Offering> pagina = new PageImpl<>(List.of(servicio));
        given(servicioRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(pagina);
        given(servicioEtiquetaRepository.findByServicioIdServicioIn(List.of(10L))).willReturn(List.of());

        Page<OfferingSummaryResponse> resultado = servicioCatalogoServicio.searchCatalogOfferings(
                1L, 1L, BigDecimal.ONE, BigDecimal.TEN, List.of(5L), "ilustracion", null, 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("searchCatalogOfferings aplica orden por precio descendente")
    void buscarCatalogoServicios_ordenaPorPrecioDesc() {
        Page<Offering> pagina = new PageImpl<>(List.of(servicio));
        given(servicioRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(pagina);
        given(servicioEtiquetaRepository.findByServicioIdServicioIn(List.of(10L))).willReturn(List.of());

        Page<OfferingSummaryResponse> resultado = servicioCatalogoServicio.searchCatalogOfferings(
                null, null, null, null, null, null, "precioDesc", 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("searchCatalogOfferings aplica orden por titulo ascendente")
    void buscarCatalogoServicios_ordenaPorTitulo() {
        Page<Offering> pagina = new PageImpl<>(List.of(servicio));
        given(servicioRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(pagina);
        given(servicioEtiquetaRepository.findByServicioIdServicioIn(List.of(10L))).willReturn(List.of());

        Page<OfferingSummaryResponse> resultado = servicioCatalogoServicio.searchCatalogOfferings(
                null, null, null, null, null, null, "tituloServicio,asc", 0, 10);

        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("searchCatalogOfferings incluye las etiquetas de cada servicio de la pagina")
    void buscarCatalogoServicios_incluyeEtiquetas() {
        Tag etiqueta = Tag.builder().idEtiqueta(5L).nombreEtiqueta("Digital").build();
        OfferingTag se = OfferingTag.builder().servicio(servicio).etiqueta(etiqueta).build();
        Page<Offering> pagina = new PageImpl<>(List.of(servicio));
        given(servicioRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(pagina);
        given(servicioEtiquetaRepository.findByServicioIdServicioIn(List.of(10L))).willReturn(List.of(se));

        Page<OfferingSummaryResponse> resultado = servicioCatalogoServicio.searchCatalogOfferings(
                null, null, null, null, null, null, null, 0, 10);

        assertThat(resultado.getContent().get(0).getEtiquetas()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEtiquetas().get(0).getNombreEtiqueta()).isEqualTo("Digital");
    }

    // ---------- Atributos dinamicos ----------

    @Test
    @DisplayName("listAttributesByOffering devuelve los atributos existentes")
    void listarAtributosPorServicio_devuelveAtributos() {
        DynamicAttribute atributo = DynamicAttribute.builder().idAtributo(1L).nombreAtributo("Color").tipoDato("TEXTO").build();
        OfferingAttribute sa = OfferingAttribute.builder().idServicioAtributo(1L).servicio(servicio).atributo(atributo).valorAsignado("Rojo").build();
        given(servicioRepository.existsById(10L)).willReturn(true);
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of(sa));

        assertThat(servicioCatalogoServicio.listAttributesByOffering(10L)).hasSize(1);
    }

    @Test
    @DisplayName("listAttributesByOffering lanza recurso no encontrado si el servicio no existe")
    void listarAtributosPorServicio_servicioInexistente() {
        given(servicioRepository.existsById(10L)).willReturn(false);

        assertThatThrownBy(() -> servicioCatalogoServicio.listAttributesByOffering(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("addAttribute reutiliza un atributo dinamico existente")
    void agregarAtributo_reutilizaExistente() {
        DynamicAttribute atributo = DynamicAttribute.builder().idAtributo(1L).nombreAtributo("Color").tipoDato("TEXTO").build();
        CreateAttributeRequest peticion = CreateAttributeRequest.builder()
                .nombreAtributo("Color").valorAsignado("Rojo").tipoDato("TEXTO").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.countByServicioIdServicio(10L)).willReturn(2L);
        given(atributoRepository.findByNombreAtributoIgnoreCase("Color")).willReturn(Optional.of(atributo));
        given(servicioAtributoRepository.findByServicioIdServicioAndAtributoIdAtributo(10L, 1L)).willReturn(Optional.empty());
        given(servicioAtributoRepository.save(any(OfferingAttribute.class)))
                .willAnswer(inv -> inv.getArgument(0));

        AttributeResponse respuesta = servicioCatalogoServicio.addAttribute(10L, peticion);

        assertThat(respuesta.getNombreAtributo()).isEqualTo("Color");
        verify(atributoRepository, never()).save(any());
    }

    @Test
    @DisplayName("addAttribute crea un atributo dinamico nuevo si no existe")
    void agregarAtributo_creaAtributoNuevo() {
        CreateAttributeRequest peticion = CreateAttributeRequest.builder()
                .nombreAtributo("Formato").valorAsignado("PDF").tipoDato("TEXTO").build();
        DynamicAttribute nuevo = DynamicAttribute.builder().idAtributo(2L).nombreAtributo("Formato").tipoDato("TEXTO").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.countByServicioIdServicio(10L)).willReturn(0L);
        given(atributoRepository.findByNombreAtributoIgnoreCase("Formato")).willReturn(Optional.empty());
        given(atributoRepository.save(any(DynamicAttribute.class))).willReturn(nuevo);
        given(servicioAtributoRepository.findByServicioIdServicioAndAtributoIdAtributo(10L, 2L)).willReturn(Optional.empty());
        given(servicioAtributoRepository.save(any(OfferingAttribute.class))).willAnswer(inv -> inv.getArgument(0));

        AttributeResponse respuesta = servicioCatalogoServicio.addAttribute(10L, peticion);

        assertThat(respuesta.getNombreAtributo()).isEqualTo("Formato");
    }

    @Test
    @DisplayName("addAttribute rechaza el limite de 10 atributos por servicio")
    void agregarAtributo_rechazaLimite() {
        CreateAttributeRequest peticion = CreateAttributeRequest.builder()
                .nombreAtributo("Color").valorAsignado("Rojo").tipoDato("TEXTO").build();
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.countByServicioIdServicio(10L)).willReturn(10L);

        assertThatThrownBy(() -> servicioCatalogoServicio.addAttribute(10L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("addAttribute rechaza un atributo ya asociado al servicio")
    void agregarAtributo_rechazaDuplicado() {
        DynamicAttribute atributo = DynamicAttribute.builder().idAtributo(1L).nombreAtributo("Color").tipoDato("TEXTO").build();
        OfferingAttribute existente = OfferingAttribute.builder().idServicioAtributo(1L).servicio(servicio).atributo(atributo).valorAsignado("Rojo").build();
        CreateAttributeRequest peticion = CreateAttributeRequest.builder()
                .nombreAtributo("Color").valorAsignado("Azul").tipoDato("TEXTO").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.countByServicioIdServicio(10L)).willReturn(1L);
        given(atributoRepository.findByNombreAtributoIgnoreCase("Color")).willReturn(Optional.of(atributo));
        given(servicioAtributoRepository.findByServicioIdServicioAndAtributoIdAtributo(10L, 1L)).willReturn(Optional.of(existente));

        assertThatThrownBy(() -> servicioCatalogoServicio.addAttribute(10L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("updateAttribute cambia el valor asignado")
    void actualizarAtributo_cambiaValor() {
        DynamicAttribute atributo = DynamicAttribute.builder().idAtributo(1L).nombreAtributo("Color").tipoDato("TEXTO").build();
        OfferingAttribute sa = OfferingAttribute.builder().idServicioAtributo(1L).servicio(servicio).atributo(atributo).valorAsignado("Rojo").build();
        UpdateAttributeRequest peticion = UpdateAttributeRequest.builder().valorAsignado("Azul").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findById(1L)).willReturn(Optional.of(sa));
        given(servicioAtributoRepository.save(any(OfferingAttribute.class))).willAnswer(inv -> inv.getArgument(0));

        AttributeResponse respuesta = servicioCatalogoServicio.updateAttribute(10L, 1L, peticion);

        assertThat(respuesta.getValorAsignado()).isEqualTo("Azul");
    }

    @Test
    @DisplayName("updateAttribute rechaza un atributo que no pertenece al servicio")
    void actualizarAtributo_rechazaAtributoDeOtroServicio() {
        Offering otroServicio = Offering.builder().idServicio(20L).build();
        DynamicAttribute atributo = DynamicAttribute.builder().idAtributo(1L).build();
        OfferingAttribute sa = OfferingAttribute.builder().idServicioAtributo(1L).servicio(otroServicio).atributo(atributo).valorAsignado("Rojo").build();
        UpdateAttributeRequest peticion = UpdateAttributeRequest.builder().valorAsignado("Azul").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findById(1L)).willReturn(Optional.of(sa));

        assertThatThrownBy(() -> servicioCatalogoServicio.updateAttribute(10L, 1L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("updateAttribute lanza recurso no encontrado si el servicio-atributo no existe")
    void actualizarAtributo_servicioAtributoInexistente() {
        UpdateAttributeRequest peticion = UpdateAttributeRequest.builder().valorAsignado("Azul").build();
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicioCatalogoServicio.updateAttribute(10L, 1L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteAttribute borra la asociacion cuando pertenece al servicio")
    void eliminarAtributo_borraAsociacion() {
        DynamicAttribute atributo = DynamicAttribute.builder().idAtributo(1L).build();
        OfferingAttribute sa = OfferingAttribute.builder().idServicioAtributo(1L).servicio(servicio).atributo(atributo).valorAsignado("Rojo").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findById(1L)).willReturn(Optional.of(sa));

        servicioCatalogoServicio.deleteAttribute(10L, 1L);

        verify(servicioAtributoRepository).delete(sa);
    }

    @Test
    @DisplayName("deleteAttribute rechaza un atributo que no pertenece al servicio")
    void eliminarAtributo_rechazaAtributoDeOtroServicio() {
        Offering otroServicio = Offering.builder().idServicio(20L).build();
        DynamicAttribute atributo = DynamicAttribute.builder().idAtributo(1L).build();
        OfferingAttribute sa = OfferingAttribute.builder().idServicioAtributo(1L).servicio(otroServicio).atributo(atributo).valorAsignado("Rojo").build();

        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findById(1L)).willReturn(Optional.of(sa));

        assertThatThrownBy(() -> servicioCatalogoServicio.deleteAttribute(10L, 1L))
                .isInstanceOf(BusinessRuleException.class);
        verify(servicioAtributoRepository, never()).delete(any());
    }

    // ---------- removeSubcategory ----------

    @Test
    @DisplayName("removeSubcategory borra la asociacion cuando el servicio tiene mas de una")
    void quitarSubcategoria_borraCuandoQuedaAlMenosUna() {
        given(servicioRepository.existsById(10L)).willReturn(true);
        given(servicioSubcategoriaRepository.countByServicioIdServicio(10L)).willReturn(2L);
        given(servicioRepository.findById(10L)).willReturn(Optional.of(servicio));
        given(servicioAtributoRepository.findByServicioIdServicio(10L)).willReturn(List.of());
        given(servicioEtiquetaRepository.findByServicioIdServicio(10L)).willReturn(List.of());

        servicioCatalogoServicio.removeSubcategory(10L, 1L);

        verify(servicioSubcategoriaRepository).deleteByServicioIdServicioAndSubcategoriaIdSubcategoria(10L, 1L);
    }

    @Test
    @DisplayName("removeSubcategory rechaza si el servicio se quedaria sin ninguna")
    void quitarSubcategoria_rechazaSiQuedaSinNinguna() {
        given(servicioRepository.existsById(10L)).willReturn(true);
        given(servicioSubcategoriaRepository.countByServicioIdServicio(10L)).willReturn(1L);

        assertThatThrownBy(() -> servicioCatalogoServicio.removeSubcategory(10L, 1L))
                .isInstanceOf(BusinessRuleException.class);
        verify(servicioSubcategoriaRepository, never()).deleteByServicioIdServicioAndSubcategoriaIdSubcategoria(any(), any());
    }

    private void autenticarComo(String correo, String... authorities) {
        List<SimpleGrantedAuthority> roles = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        var auth = new UsernamePasswordAuthenticationToken(correo, "N/A", roles);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
