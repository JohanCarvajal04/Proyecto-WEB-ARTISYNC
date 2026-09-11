package uteq.edu.ec.artisync.service.seguridad.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ContextoAuditoria;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.entity.legal.Contrato;
import uteq.edu.ec.artisync.entity.legal.PagoGarantia;
import uteq.edu.ec.artisync.entity.pedido.HistorialEstadoPedido;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;
import uteq.edu.ec.artisync.entity.perfil.DatosPagoCreador;
import uteq.edu.ec.artisync.entity.seguridad.AutenticacionDosFactores;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.legal.PagoGarantiaRepository;
import uteq.edu.ec.artisync.repository.pedido.FlujoEtapaConfigRepository;
import uteq.edu.ec.artisync.repository.pedido.HistorialEstadoPedidoRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.repository.perfil.DatosPagoCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.AutenticacionDosFactoresRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.seguridad.PrivacidadService;
import uteq.edu.ec.artisync.service.seguridad.TwoFactorService;
import uteq.edu.ec.artisync.service.shared.IntentosAutenticacionService;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.StoredProcedureExceptionTranslator;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * REQ-NF-018: supresión real de datos personales, complementaria a la baja
 * lógica de cuenta (estadoCuenta=false) que ya ofrecen UserService y
 * AdminUserService. Ver docs/basedatos/POLITICA-RETENCION.md.
 *
 * Estrategia de anonimización (no borrado físico): se sobrescriben los
 * campos identificativos de {@code usuarios}, se limpian los datos extraídos
 * por IA y el hash de {@code certificados_ia}, y se anonimiza el correo de
 * PayPal en {@code datos_pago_creador} — salvo que el usuario tenga un
 * contrato con fondos aún retenidos en garantía (pagos_garantia.estado_fondos
 * = "Retenido"), en cuyo caso ese dato de pago queda excluido y la excepción
 * legal se declara en la respuesta y en el detalle del evento de auditoría.
 * {@code contratos} no tiene campos personales propios (solo hashes/URLs) —
 * su plazo de retención se declara en la política, no requiere anonimización
 * aquí (ver REQ-NF-020 para la re-verificación del hash de firma).
 *
 * Antes de tocar cualquier dato, se rechaza la operación completa si el
 * usuario tiene un pedido cuya etapa actual no es la etapa final de su flujo
 * ("en curso"): anonimizar a alguien en medio de una transacción activa
 * rompería la atribución de mensajería, entregables y reseñas para la
 * contraparte. El titular debe esperar a que sus pedidos en curso terminen o
 * se cancelen antes de solicitar la supresión.
 *
 * La idempotencia se resuelve releyendo el propio correo del usuario bajo
 * bloqueo pesimista de fila ({@link UsuarioRepository#findByIdParaAnonimizar}):
 * si ya termina en el dominio anónimo, la operación ya se ejecutó. El bloqueo
 * evita la condición de carrera de dos solicitudes casi simultáneas (doble
 * clic, autoservicio + admin a la vez) que, con una simple lectura sin
 * bloquear, podían pasar ambas el chequeo antes de que la primera confirmara
 * su cambio.
 */
@Service
@RequiredArgsConstructor
public class PrivacidadServiceImpl implements PrivacidadService {

    private static final String ACCION_ANONIMIZAR = "USUARIO_ANONIMIZAR";
    private static final String ENTIDAD_USUARIOS = "usuarios";
    private static final String CORREO_ANONIMO_DOMINIO = "@eliminado.artisync.invalid";
    private static final String NOMBRE_ANONIMO = "Usuario eliminado";
    private static final String APELLIDOS_ANONIMOS = "(dato suprimido)";
    private static final String CORREO_PAYPAL_ANONIMO = "eliminado@eliminado.artisync.invalid";
    private static final String ESTADO_FONDOS_RETENIDO = "Retenido";
    private static final String MENSAJE_YA_ANONIMIZADO = "Tus datos personales ya fueron suprimidos anteriormente.";
    private static final String MENSAJE_PEDIDO_EN_CURSO =
            "No fue posible suprimir los datos: el usuario tiene al menos un pedido en curso "
                    + "(aún no llega a la etapa final de su flujo). Debe completarse o cancelarse antes de solicitar la supresión.";
    private static final String AMBITO_2FA_SUPRESION = "2fa-supresion-cuenta";
    private static final int LIMITE_INTENTOS_2FA = 5;
    private static final Duration VENTANA_INTENTOS_2FA = Duration.ofMinutes(15);

    private final UsuarioRepository usuarioRepository;
    private final CertificadoIaRepository certificadoIaRepository;
    private final DatosPagoCreadorRepository datosPagoCreadorRepository;
    private final ContratoRepository contratoRepository;
    private final PagoGarantiaRepository pagoGarantiaRepository;
    private final PedidoRepository pedidoRepository;
    private final HistorialEstadoPedidoRepository historialEstadoPedidoRepository;
    private final FlujoEtapaConfigRepository flujoEtapaConfigRepository;
    private final AutenticacionDosFactoresRepository autenticacionDosFactoresRepository;
    private final TwoFactorService twoFactorService;
    private final IntentosAutenticacionService intentosAutenticacionService;
    private final SessionRevocationService sessionRevocationService;
    private final AlmacenamientoDocumentos almacenamientoDocumentos;

    @Override
    @Transactional
    @Auditable(accion = ACCION_ANONIMIZAR, modulo = ModuloAuditoria.SEGURIDAD,
            entidad = ENTIDAD_USUARIOS, idEntidad = "#idUsuario",
            detalle = "{origen: 'AUTOSERVICIO'}")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @param codigo parametro requerido para la correcta ejecucion del procedimiento
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaMensaje solicitarSupresionPropia(Long idUsuario, String codigo) {
        Usuario usuario = usuarioRepository.findByIdParaAnonimizar(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (estaAnonimizado(usuario)) {
            return new RespuestaMensaje(MENSAJE_YA_ANONIMIZADO);
        }

        exigirSegundoFactorSiHabilitado(usuario, codigo);

        if (tienePedidoEnCurso(idUsuario)) {
            return new RespuestaMensaje(MENSAJE_PEDIDO_EN_CURSO);
        }

        List<String> excepciones = anonimizar(usuario);
        return construirMensaje(excepciones);
    }

    /**
     * REQ-NF-018 (ajuste de seguimiento): step-up de verificación antes de una
     * acción irreversible — mismo criterio que ya exige
     * {@code TwoFactorServiceImpl#disable2Fa} para desactivar el propio 2FA.
     * Solo aplica al autoservicio: el administrador no tiene el código 2FA
     * del usuario, por eso existe la vía {@code anonimizarUsuarioAdmin}.
     */
    private void exigirSegundoFactorSiHabilitado(Usuario usuario, String codigo) {
        boolean tiene2Fa = autenticacionDosFactoresRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .map(AutenticacionDosFactores::getEstaHabilitado)
                .map(Boolean.TRUE::equals)
                .orElse(false);
        if (!tiene2Fa) {
            return;
        }

        if (codigo == null || codigo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requiere tu código de autenticación de dos factores para suprimir tus datos");
        }

        if (!twoFactorService.validarCodigoOBackup(usuario.getCorreo(), codigo)) {
            intentosAutenticacionService.verificarCuota(
                    AMBITO_2FA_SUPRESION, usuario.getCorreo(), LIMITE_INTENTOS_2FA, VENTANA_INTENTOS_2FA);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido o expirado");
        }
        intentosAutenticacionService.limpiar(AMBITO_2FA_SUPRESION, usuario.getCorreo());
    }

    @Override
    @Transactional
    @Auditable(accion = ACCION_ANONIMIZAR, modulo = ModuloAuditoria.SEGURIDAD,
            entidad = ENTIDAD_USUARIOS, idEntidad = "#idUsuario",
            detalle = "{origen: 'ADMIN', idAdminActual: #idAdminActual}")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @param idAdminActual identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaMensaje anonimizarUsuarioAdmin(Long idUsuario, Long idAdminActual) {
        if (idUsuario.equals(idAdminActual)) {
            throw new ExcepcionReglaNegocio("No puedes suprimir tus propios datos desde el panel administrativo; usa la opción de autoservicio.");
        }

        Usuario usuario = usuarioRepository.findByIdParaAnonimizar(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // A diferencia del autoservicio, aquí SÍ se rechaza en vez de responder
        // de forma idempotente: el administrador solo debe ejecutar esta acción
        // cuando el usuario no la tiene ya solicitada/realizada.
        if (estaAnonimizado(usuario)) {
            throw new ExcepcionReglaNegocio(
                    "El usuario ya tiene sus datos personales suprimidos; no es necesario repetir la acción.");
        }

        if (tienePedidoEnCurso(idUsuario)) {
            throw new ExcepcionReglaNegocio(MENSAJE_PEDIDO_EN_CURSO);
        }

        List<String> excepciones = anonimizar(usuario);
        return construirMensaje(excepciones);
    }

    private boolean estaAnonimizado(Usuario usuario) {
        return usuario.getCorreo() != null && usuario.getCorreo().endsWith(CORREO_ANONIMO_DOMINIO);
    }

    /**
     * ¿El usuario (como cliente o como creador) tiene algún pedido cuya
     * transición más reciente NO apunta a la etapa final de su flujo? Un
     * pedido sin ninguna transición registrada se trata, conservadoramente,
     * como "en curso": no hay evidencia de que haya terminado.
     */
    private boolean tienePedidoEnCurso(Long idUsuario) {
        List<Pedido> pedidos = Stream.concat(
                        pedidoRepository.findByUsuarioClienteIdUsuario(idUsuario).stream(),
                        pedidoRepository.findByServicioPerfilUsuarioIdUsuario(idUsuario).stream())
                .toList();

        for (Pedido pedido : pedidos) {
            Optional<HistorialEstadoPedido> ultimaTransicion =
                    historialEstadoPedidoRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(pedido.getIdPedido());

            if (ultimaTransicion.isEmpty()) {
                return true;
            }

            boolean etapaActualEsFinal = flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapaAndEsEtapaFinalTrue(
                    pedido.getFlujo().getIdFlujo(), ultimaTransicion.get().getEtapa().getIdEtapa());
            if (!etapaActualEsFinal) {
                return true;
            }
        }
        return false;
    }

    private List<String> anonimizar(Usuario usuario) {
        List<String> excepciones = new ArrayList<>();

        anonimizarDatosUsuario(usuario);
        anonimizarCertificados(usuario.getIdUsuario());
        anonimizarDatosPago(usuario.getIdUsuario(), excepciones);

        ContextoAuditoria.aportar("excepcionesLegales", excepciones);
        return excepciones;
    }

    private void anonimizarDatosUsuario(Usuario usuario) {
        if (usuario.getUrlFotoPerfil() != null) {
            try {
                almacenamientoDocumentos.eliminar(usuario.getUrlFotoPerfil());
            } catch (Exception e) {
                // La foto ya podría no existir en el proveedor; no bloquear la supresión por esto.
            }
        }

        usuario.setNombres(NOMBRE_ANONIMO);
        usuario.setApellidos(APELLIDOS_ANONIMOS);
        // REQ-NF-018 (ajuste de seguimiento): el UUID hace el correo
        // impredecible, no solo derivado del id -- sin esto, alguien podía
        // auto-registrarse hoy con "usuario-<id>@..." para un id que aún no
        // se ha suprimido y provocar una colisión contra el UNIQUE de
        // usuarios.correo (V1__schema_inicial.sql) el día que sí se suprima.
        // Mismo idiom que JwtService (jti), AuthServiceImpl (token de
        // recuperación) y TwoFactorServiceImpl (código de respaldo).
        usuario.setCorreo("usuario-" + usuario.getIdUsuario() + "-" + UUID.randomUUID() + CORREO_ANONIMO_DOMINIO);
        usuario.setFechaNacimiento(null);
        usuario.setUrlFotoPerfil(null);
        try {
            usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            // Defensa en profundidad: con el UUID de arriba esta rama es
            // prácticamente inalcanzable, pero degrada a un 409 controlado
            // en vez de un 500 sin manejar si alguna vez ocurre.
            throw StoredProcedureExceptionTranslator.traducir(e, HttpStatus.CONFLICT);
        }

        // fn_cambiar_estado_cuenta desactiva la cuenta y revoca sus sesiones
        // atómicamente (mismo mecanismo que el soft-delete existente).
        sessionRevocationService.cambiarEstadoCuenta(usuario.getIdUsuario(), false);
    }

    private void anonimizarCertificados(Long idUsuario) {
        List<CertificadoIa> certificados = certificadoIaRepository.findByUsuarioIdUsuario(idUsuario);
        for (CertificadoIa certificado : certificados) {
            if (!certificado.isDocumentoEliminado() && certificado.getUrlDocumentoS3() != null) {
                try {
                    almacenamientoDocumentos.eliminar(certificado.getUrlDocumentoS3());
                } catch (Exception e) {
                    // El documento ya podría no existir; no bloquear la supresión por esto.
                }
                certificado.setDocumentoEliminado(true);
            }
            certificado.setDatosExtraidosIa(null);
            certificado.setHashDocumento(null);
            certificado.setRazonIa(null);
        }
        certificadoIaRepository.saveAll(certificados);
    }

    /**
     * Anonimiza el correo de PayPal salvo que exista un contrato (como
     * cliente o como creador) con fondos aún retenidos en garantía — en ese
     * caso el dato de pago se conserva y la excepción legal queda declarada.
     */
    private void anonimizarDatosPago(Long idUsuario, List<String> excepciones) {
        Optional<DatosPagoCreador> datosPago = datosPagoCreadorRepository.findByUsuarioIdUsuario(idUsuario);
        if (datosPago.isEmpty()) {
            return;
        }

        if (tieneFondosRetenidos(idUsuario)) {
            excepciones.add(
                    "datos_pago_creador: correo de PayPal conservado por tener fondos retenidos en garantía "
                            + "en un contrato activo (registro contable pendiente).");
            return;
        }

        DatosPagoCreador entidad = datosPago.get();
        entidad.setCorreoPaypal(CORREO_PAYPAL_ANONIMO);
        datosPagoCreadorRepository.save(entidad);
    }

    private boolean tieneFondosRetenidos(Long idUsuario) {
        List<Contrato> contratos = Stream.concat(
                        contratoRepository.findByPedidoUsuarioClienteIdUsuario(idUsuario).stream(),
                        contratoRepository.findByPedidoServicioPerfilUsuarioIdUsuario(idUsuario).stream())
                .toList();

        for (Contrato contrato : contratos) {
            Optional<PagoGarantia> pago = pagoGarantiaRepository.findByContratoIdContrato(contrato.getIdContrato());
            if (pago.isPresent() && ESTADO_FONDOS_RETENIDO.equalsIgnoreCase(pago.get().getEstadoFondos())) {
                return true;
            }
        }
        return false;
    }

    private RespuestaMensaje construirMensaje(List<String> excepciones) {
        if (excepciones.isEmpty()) {
            return new RespuestaMensaje("Datos personales suprimidos exitosamente.");
        }
        return new RespuestaMensaje(
                "Datos personales suprimidos, con las siguientes excepciones legales: " + String.join(" | ", excepciones));
    }
}
