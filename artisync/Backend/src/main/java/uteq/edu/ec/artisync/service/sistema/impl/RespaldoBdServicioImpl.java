package uteq.edu.ec.artisync.service.sistema.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.peticion.sistema.PeticionActualizarPoliticaRespaldo;
import uteq.edu.ec.artisync.dto.respuesta.sistema.RespuestaRespaldo;
import uteq.edu.ec.artisync.dto.respuesta.sistema.ResumenRespaldos;
import uteq.edu.ec.artisync.entity.sistema.RespaldoBd;
import uteq.edu.ec.artisync.entity.sistema.RespaldoPolitica;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.sistema.RespaldoBdRepository;
import uteq.edu.ec.artisync.repository.sistema.RespaldoPoliticaRepository;
import uteq.edu.ec.artisync.service.sistema.IRespaldoBdServicio;
import uteq.edu.ec.artisync.util.PagedResponse;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RespaldoBdServicioImpl implements IRespaldoBdServicio {

    /** Tiempo máximo que se espera a pg_dump/pg_restore antes de cancelarlos. */
    private static final long TIMEOUT_MINUTOS_PROCESO = 5;

    private static final Pattern JDBC_POSTGRES_PATTERN =
            Pattern.compile("jdbc:postgresql://([^:/]+):(\\d+)/([^?]+)");

    /** Firma de un dump de PostgreSQL en formato custom ("PGDMP" en ASCII). */
    private static final byte[] FIRMA_PGDMP = "PGDMP".getBytes(StandardCharsets.US_ASCII);

    /**
     * pg_restore --clean va soltando (DROP) e insertando datos tabla por tabla;
     * si falla o se cancela a mitad de camino, la base puede quedar con solo
     * una parte de las tablas restauradas (estado mixto entre el dato viejo y
     * el nuevo). Se lo advierte explícitamente en cada error de restauración
     * para que el admin no asuma que la base sigue intacta.
     */
    private static final String ADVERTENCIA_RESTAURACION_INCOMPLETA =
            "ADVERTENCIA: la restauración pudo haber quedado incompleta a mitad de camino. "
            + "La base de datos puede estar en un estado inconsistente (parte de las tablas restauradas y parte no); "
            + "verifique manualmente antes de seguir usando el sistema, o intente restaurar nuevamente.";

    private final RespaldoBdRepository respaldoRepository;
    private final RespaldoPoliticaRepository politicaRepository;

    @Value("${app.respaldo.ruta-base:/var/artisync/backups}")
    private String rutaBase;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    // pg_dump necesita leer todo el esquema y pg_restore --clean necesita DDL
    // (DROP/CREATE) para limpiar objetos antes de restaurar. artisync_app es
    // de privilegios mínimos (solo DML, sin DDL), así que se usan las
    // credenciales de administración de esquema (las mismas que usa Flyway)
    // en vez del datasource de la aplicación.
    @Value("${spring.flyway.user}")
    private String dbUser;

    @Value("${spring.flyway.password}")
    private String dbPassword;

    @PostConstruct
    public void init() {
        File dir = new File(rutaBase);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RespuestaRespaldo> listar(Pageable pageable, String tipo, String categoria) {
        RespaldoBd.TipoRespaldo tipoEnum = (tipo != null && !tipo.isEmpty())
                ? RespaldoBd.TipoRespaldo.valueOf(tipo.toUpperCase()) : null;
        RespaldoBd.CategoriaRespaldo categoriaEnum = (categoria != null && !categoria.isEmpty())
                ? RespaldoBd.CategoriaRespaldo.valueOf(categoria.toUpperCase()) : null;

        Page<RespaldoBd> pagina;
        if (tipoEnum != null && categoriaEnum != null) {
            pagina = respaldoRepository.findByTipoAndCategoria(tipoEnum, categoriaEnum, pageable);
        } else if (categoriaEnum != null) {
            pagina = respaldoRepository.findByCategoria(categoriaEnum, pageable);
        } else if (tipoEnum != null) {
            pagina = respaldoRepository.findByTipo(tipoEnum, pageable);
        } else {
            pagina = respaldoRepository.findAll(pageable);
        }
        Page<RespuestaRespaldo> p = pagina.map(this::mapToResponse);
        return new PagedResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isLast());
    }

    @Override
    @Transactional
    public RespuestaRespaldo crearRespaldoManual(String correoUsuario, RespaldoBd.CategoriaRespaldo categoria) {
        RespaldoBd.CategoriaRespaldo cat = categoria != null ? categoria : RespaldoBd.CategoriaRespaldo.FULL;
        return ejecutarRespaldo(RespaldoBd.TipoRespaldo.MANUAL, cat, correoUsuario);
    }

    @Override
    @Transactional
    public void ejecutarRespaldoAutomatico(RespaldoBd.CategoriaRespaldo categoria) {
        ejecutarRespaldo(RespaldoBd.TipoRespaldo.AUTOMATICO, categoria, null);
    }

    private RespuestaRespaldo ejecutarRespaldo(RespaldoBd.TipoRespaldo tipo, RespaldoBd.CategoriaRespaldo categoria, String usuario) {
        RespaldoBd respaldo = new RespaldoBd();
        respaldo.setTipo(tipo);
        respaldo.setCategoria(categoria);
        respaldo.setEstado(RespaldoBd.EstadoRespaldo.EN_PROGRESO);
        respaldo.setCreadoPor(usuario);

        String fechaStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String nombreArchivo = "backup_" + categoria.name().toLowerCase() + "_" + fechaStr + ".dump";
        respaldo.setNombreArchivo(nombreArchivo);
        respaldo.setTamanoBytes(0L);

        RespaldoPolitica politica = obtenerPolitica();
        int retencionDias = categoria == RespaldoBd.CategoriaRespaldo.FULL
                ? politica.getRetencionDiasFull() : politica.getRetencionDiasDiario();
        if (retencionDias > 0) {
            respaldo.setFechaExpiracion(LocalDateTime.now().plusDays(retencionDias));
        }

        respaldo = respaldoRepository.save(respaldo);

        String rutaCompleta = Paths.get(rutaBase, nombreArchivo).toString();
        File logProceso = null;

        try {
            ConexionBd conexion = parsearUrlJdbc(dbUrl);

            logProceso = File.createTempFile("pg_dump_", ".log");
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", conexion.host(), "-p", conexion.puerto(),
                    "-U", dbUser, "-d", conexion.nombreBd(),
                    "--format=custom",
                    "--file=" + rutaCompleta
            );
            pb.environment().put("PGPASSWORD", dbPassword);
            pb.redirectErrorStream(true);
            pb.redirectOutput(logProceso);

            Process proceso = pb.start();
            boolean terminoATiempo = proceso.waitFor(TIMEOUT_MINUTOS_PROCESO, TimeUnit.MINUTES);

            if (!terminoATiempo) {
                proceso.destroyForcibly();
                respaldo.setEstado(RespaldoBd.EstadoRespaldo.FALLIDO);
                respaldo.setMensajeError("pg_dump excedió el tiempo máximo de " + TIMEOUT_MINUTOS_PROCESO + " minutos y fue cancelado");
                log.error("[RESPALDO] pg_dump cancelado por timeout para {}", nombreArchivo);
            } else if (proceso.exitValue() == 0) {
                File archivo = new File(rutaCompleta);
                respaldo.setTamanoBytes(archivo.length());
                respaldo.setHashSha256(calcularSha256(archivo));
                respaldo.setEstado(RespaldoBd.EstadoRespaldo.COMPLETADO);
            } else {
                String error = leerLogProceso(logProceso);
                respaldo.setEstado(RespaldoBd.EstadoRespaldo.FALLIDO);
                respaldo.setMensajeError("Exit code " + proceso.exitValue() + ": " + error);
            }

        } catch (Exception e) {
            log.error("Error al ejecutar respaldo", e);
            respaldo.setEstado(RespaldoBd.EstadoRespaldo.FALLIDO);
            respaldo.setMensajeError(e.getMessage());
        } finally {
            if (logProceso != null) {
                logProceso.delete();
            }
        }

        return mapToResponse(respaldoRepository.save(respaldo));
    }

    @Override
    @Transactional
    public RespuestaRespaldo importarRespaldoExterno(String correoUsuario, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ExcepcionReglaNegocio("El archivo está vacío.");
        }
        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || !nombreOriginal.toLowerCase().endsWith(".dump")) {
            throw new ExcepcionReglaNegocio("Solo se aceptan archivos con extensión .dump");
        }

        String fechaStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String nombreArchivo = "backup_externo_" + fechaStr + ".dump";
        String rutaCompleta = Paths.get(rutaBase, nombreArchivo).toString();
        File destino = new File(rutaCompleta);

        try {
            archivo.transferTo(destino);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el archivo: " + e.getMessage());
        }

        try {
            validarFirmaPgDump(destino);
        } catch (ExcepcionReglaNegocio e) {
            destino.delete();
            throw e;
        }

        RespaldoBd respaldo = new RespaldoBd();
        respaldo.setNombreArchivo(nombreArchivo);
        respaldo.setTipo(RespaldoBd.TipoRespaldo.MANUAL);
        respaldo.setCategoria(RespaldoBd.CategoriaRespaldo.FULL);
        respaldo.setTamanoBytes(destino.length());
        respaldo.setEstado(RespaldoBd.EstadoRespaldo.COMPLETADO);
        respaldo.setCreadoPor(correoUsuario + " (importado)");

        try {
            respaldo.setHashSha256(calcularSha256(destino));
        } catch (Exception e) {
            log.warn("No se pudo calcular el hash del archivo importado {}", nombreArchivo, e);
        }

        RespaldoPolitica politica = obtenerPolitica();
        if (politica.getRetencionDiasFull() > 0) {
            respaldo.setFechaExpiracion(LocalDateTime.now().plusDays(politica.getRetencionDiasFull()));
        }

        return mapToResponse(respaldoRepository.save(respaldo));
    }

    private void validarFirmaPgDump(File archivo) {
        byte[] cabecera = new byte[5];
        try (FileInputStream in = new FileInputStream(archivo)) {
            int leidos = in.read(cabecera);
            if (leidos < 5 || !Arrays.equals(cabecera, FIRMA_PGDMP)) {
                throw new ExcepcionReglaNegocio("El archivo no parece un dump de PostgreSQL en formato custom (falta la firma PGDMP)");
            }
        } catch (IOException e) {
            throw new ExcepcionReglaNegocio("No se pudo leer el archivo subido: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaRespaldo obtenerPorId(Long id) {
        return respaldoRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Respaldo no encontrado"));
    }

    @Override
    public Resource descargar(Long id) {
        RespaldoBd respaldo = respaldoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Respaldo no encontrado"));

        if (respaldo.getEstado() != RespaldoBd.EstadoRespaldo.COMPLETADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se pueden descargar respaldos completados");
        }

        File archivo = new File(rutaBase, respaldo.getNombreArchivo());
        if (!archivo.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo físico del respaldo no existe");
        }

        return new FileSystemResource(archivo);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        RespaldoBd respaldo = respaldoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Respaldo no encontrado"));

        File archivo = new File(rutaBase, respaldo.getNombreArchivo());
        if (archivo.exists() && !archivo.delete()) {
            log.warn("[RESPALDO] No se pudo eliminar el archivo físico '{}' del respaldo {}", archivo.getAbsolutePath(), id);
        }

        respaldoRepository.delete(respaldo);
    }

    @Override
    public void restaurar(Long id) {
        // Sin @Transactional a propósito: pg_restore corre con --clean, que hace
        // DROP/ALTER (ACCESS EXCLUSIVE) sobre las tablas de esta misma base,
        // incluida respaldos_bd. Si este método mantuviera una transacción JPA
        // abierta (con el ACCESS SHARE del findById) mientras se espera a que el
        // proceso externo termine, pg_restore quedaría bloqueado esperando ese
        // lock para siempre —y esa transacción solo se cierra cuando este método
        // retorna—, un auto-bloqueo que antes se resolvía recién al timeout de
        // TIMEOUT_MINUTOS_PROCESO. findById() ya es transaccional por sí mismo
        // (vía Spring Data) y libera la conexión de inmediato al retornar.
        RespaldoBd respaldo = respaldoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Respaldo no encontrado"));

        if (respaldo.getEstado() != RespaldoBd.EstadoRespaldo.COMPLETADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El respaldo no está en estado COMPLETADO");
        }

        File archivo = new File(rutaBase, respaldo.getNombreArchivo());
        if (!archivo.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo físico del respaldo no existe");
        }

        File logProceso = null;
        try {
            ConexionBd conexion = parsearUrlJdbc(dbUrl);

            logProceso = File.createTempFile("pg_restore_", ".log");
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_restore",
                    "-h", conexion.host(), "-p", conexion.puerto(),
                    "-U", dbUser, "-d", conexion.nombreBd(),
                    "--clean", "--if-exists",
                    archivo.getAbsolutePath()
            );
            pb.environment().put("PGPASSWORD", dbPassword);
            pb.redirectErrorStream(true);
            pb.redirectOutput(logProceso);

            Process proceso = pb.start();
            boolean terminoATiempo = proceso.waitFor(TIMEOUT_MINUTOS_PROCESO, TimeUnit.MINUTES);

            if (!terminoATiempo) {
                proceso.destroyForcibly();
                throw new RuntimeException("pg_restore excedió el tiempo máximo de " + TIMEOUT_MINUTOS_PROCESO + " minutos y fue cancelado");
            }
            if (proceso.exitValue() != 0) {
                String error = leerLogProceso(logProceso);
                throw new RuntimeException("Error en pg_restore: Exit code " + proceso.exitValue() + ": " + error);
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al restaurar respaldo", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al restaurar: " + e.getMessage() + " " + ADVERTENCIA_RESTAURACION_INCOMPLETA);
        } finally {
            if (logProceso != null) {
                logProceso.delete();
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenRespaldos obtenerResumen() {
        long total = respaldoRepository.count();
        long auto = respaldoRepository.countByEstadoAndTipo(RespaldoBd.EstadoRespaldo.COMPLETADO, RespaldoBd.TipoRespaldo.AUTOMATICO);
        long manual = respaldoRepository.countByEstadoAndTipo(RespaldoBd.EstadoRespaldo.COMPLETADO, RespaldoBd.TipoRespaldo.MANUAL);
        long full = respaldoRepository.countByEstadoAndCategoria(RespaldoBd.EstadoRespaldo.COMPLETADO, RespaldoBd.CategoriaRespaldo.FULL);
        long diario = respaldoRepository.countByEstadoAndCategoria(RespaldoBd.EstadoRespaldo.COMPLETADO, RespaldoBd.CategoriaRespaldo.DIARIO);

        String ultimaFecha = respaldoRepository.findTopByEstadoOrderByFechaCreacionDesc(RespaldoBd.EstadoRespaldo.COMPLETADO)
                .map(r -> r.getFechaCreacion().toString())
                .orElse(null);

        RespaldoPolitica pol = obtenerPolitica();
        String proximoFull = calcularProximaEjecucion(pol.getCronFull());
        String proximoDiario = calcularProximaEjecucion(pol.getCronDiario());

        LocalDateTime ultimoFullAuto = respaldoRepository.findTopByEstadoAndTipoAndCategoriaOrderByFechaCreacionDesc(
                RespaldoBd.EstadoRespaldo.COMPLETADO, RespaldoBd.TipoRespaldo.AUTOMATICO, RespaldoBd.CategoriaRespaldo.FULL)
                .map(RespaldoBd::getFechaCreacion).orElse(null);
        LocalDateTime ultimoDiarioAuto = respaldoRepository.findTopByEstadoAndTipoAndCategoriaOrderByFechaCreacionDesc(
                RespaldoBd.EstadoRespaldo.COMPLETADO, RespaldoBd.TipoRespaldo.AUTOMATICO, RespaldoBd.CategoriaRespaldo.DIARIO)
                .map(RespaldoBd::getFechaCreacion).orElse(null);

        boolean fullAtrasado = estaAtrasado(pol.getCronFull(), ultimoFullAuto);
        boolean diarioAtrasado = estaAtrasado(pol.getCronDiario(), ultimoDiarioAuto);

        long sizeBytes = 0;
        File dir = new File(rutaBase);
        if (dir.exists() && dir.listFiles() != null) {
            for (File f : dir.listFiles()) {
                sizeBytes += f.length();
            }
        }

        return new ResumenRespaldos(
                total, auto, manual, full, diario, ultimaFecha, proximoFull, proximoDiario,
                pol.getCronFull(), pol.getRetencionDiasFull(),
                pol.getCronDiario(), pol.getRetencionDiasDiario(),
                formatearTamano(sizeBytes),
                ultimoFullAuto != null ? ultimoFullAuto.toString() : null,
                ultimoDiarioAuto != null ? ultimoDiarioAuto.toString() : null,
                fullAtrasado, diarioAtrasado
        );
    }

    /**
     * Compara el último respaldo AUTOMATICO exitoso de una categoría contra lo
     * que su propio cron esperaría, para detectar que el scheduler dejó de
     * correr (típicamente porque el proceso no estaba vivo a esa hora: un
     * servicio dormido/apagado, un free-tier suspendido, o el contenedor
     * detenido). Un cron por sí solo no dispara nada si la JVM no está viva
     * en ese instante exacto, y eso no queda registrado en ningún lado salvo
     * por la ausencia del respaldo esperado.
     */
    private boolean estaAtrasado(String cron, LocalDateTime ultimoExitoso) {
        try {
            CronExpression ce = CronExpression.parse(cron);
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime next1 = ce.next(ahora);
            LocalDateTime next2 = next1 != null ? ce.next(next1) : null;
            if (next1 == null || next2 == null) return false;

            Duration intervalo = Duration.between(next1, next2);
            // Margen de gracia sobre el intervalo esperado, para absorber que el
            // proceso tarde en despertar/reiniciar: la mitad del intervalo, con
            // un piso de 2 horas para crons muy frecuentes.
            Duration margen = intervalo.compareTo(Duration.ofHours(4)) > 0
                    ? intervalo.dividedBy(2) : Duration.ofHours(2);
            Duration limite = intervalo.plus(margen);

            if (ultimoExitoso == null) {
                // Nunca hubo un respaldo automático exitoso de esta categoría:
                // atrasado solo si el cron ya debería haber disparado al menos
                // una vez dentro de la ventana reciente (evita marcar como
                // atrasado algo que sencillamente todavía no le tocaba correr).
                LocalDateTime ocurrenciaEnVentana = ce.next(ahora.minus(limite));
                return ocurrenciaEnVentana != null && !ocurrenciaEnVentana.isAfter(ahora);
            }

            return Duration.between(ultimoExitoso, ahora).compareTo(limite) > 0;
        } catch (Exception e) {
            log.warn("No se pudo evaluar si el respaldo automático está atrasado para el cron '{}': {}", cron, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void purgarExpirados() {
        List<RespaldoBd> expirados = respaldoRepository.findByEstadoAndFechaExpiracionBefore(
                RespaldoBd.EstadoRespaldo.COMPLETADO, LocalDateTime.now());

        for (RespaldoBd r : expirados) {
            eliminar(r.getIdRespaldo());
            log.info("Respaldo expirado purgado: {}", r.getNombreArchivo());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RespaldoPolitica obtenerPolitica() {
        return politicaRepository.findById(1)
                .orElseThrow(() -> new IllegalStateException("Política de respaldo no encontrada"));
    }

    @Override
    @Transactional
    public RespaldoPolitica actualizarPolitica(PeticionActualizarPoliticaRespaldo peticion) {
        RespaldoPolitica pol = obtenerPolitica();
        pol.setCronFull(peticion.cronFull());
        pol.setRetencionDiasFull(peticion.retencionDiasFull());
        pol.setCronDiario(peticion.cronDiario());
        pol.setRetencionDiasDiario(peticion.retencionDiasDiario());
        return politicaRepository.save(pol);
    }

    private RespuestaRespaldo mapToResponse(RespaldoBd r) {
        return new RespuestaRespaldo(
                r.getIdRespaldo(),
                r.getNombreArchivo(),
                r.getTipo().name(),
                r.getCategoria().name(),
                r.getTamanoBytes(),
                formatearTamano(r.getTamanoBytes()),
                r.getEstado().name(),
                r.getMensajeError(),
                r.getCreadoPor(),
                r.getFechaCreacion() != null ? r.getFechaCreacion().toString() : null,
                r.getFechaExpiracion() != null ? r.getFechaExpiracion().toString() : null,
                r.getHashSha256()
        );
    }

    private String formatearTamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String calcularSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(Files.readAllBytes(file.toPath()));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String leerLogProceso(File logProceso) {
        try {
            return Files.readString(logProceso.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "(no se pudo leer la salida del proceso: " + e.getMessage() + ")";
        }
    }

    private String calcularProximaEjecucion(String cron) {
        try {
            return CronExpression.parse(cron).next(LocalDateTime.now()).toString();
        } catch (Exception e) {
            log.warn("No se pudo calcular la próxima ejecución para el cron '{}': {}", cron, e.getMessage());
            return null;
        }
    }

    private record ConexionBd(String host, String puerto, String nombreBd) {}

    private ConexionBd parsearUrlJdbc(String url) {
        Matcher matcher = JDBC_POSTGRES_PATTERN.matcher(url);
        if (!matcher.find()) {
            log.error("[RESPALDO] URL JDBC con formato inesperado: {}", url);
            throw new IllegalStateException("La URL de la base de datos no tiene el formato esperado (jdbc:postgresql://host:puerto/bd): " + url);
        }
        return new ConexionBd(matcher.group(1), matcher.group(2), matcher.group(3));
    }
}
