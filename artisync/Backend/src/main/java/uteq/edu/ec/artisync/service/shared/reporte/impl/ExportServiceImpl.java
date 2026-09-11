package uteq.edu.ec.artisync.service.shared.reporte.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ValueFormatter;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.service.shared.reporte.ReportGenerator;
import uteq.edu.ec.artisync.service.shared.reporte.IExportService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportModel;

import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ExportServiceImpl implements IExportService {

    private static final DateTimeFormatter FORMATO_MARCA_TIEMPO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final Pattern CARACTERES_NO_SLUG = Pattern.compile("[^a-z0-9]+");

    private final Map<ReportFormat, ReportGenerator> generadoresPorFormato;

    public ExportServiceImpl(List<ReportGenerator> generadores) {
        this.generadoresPorFormato = new EnumMap<>(ReportFormat.class);
        for (ReportGenerator generador : generadores) {
            ReportGenerator previo = this.generadoresPorFormato.put(generador.formato(), generador);
            if (previo != null) {
                throw new IllegalStateException(
                        "Hay más de un ReportGenerator registrado para el formato " + generador.formato());
            }
        }
        for (ReportFormat formato : ReportFormat.values()) {
            if (!this.generadoresPorFormato.containsKey(formato)) {
                throw new IllegalStateException("Falta un ReportGenerator para el formato " + formato);
            }
        }
    }

    @Override
    public <T> GeneratedDocument exportar(ReportModel<T> modelo, ReportFormat formato) {
        int totalFilas = modelo.getFilas().size();
        if (totalFilas > formato.topeFilas()) {
            throw new BusinessRuleException(
                    "El reporte '" + modelo.getTitulo() + "' tiene " + totalFilas + " filas, más de las "
                            + formato.topeFilas() + " que admite una exportación en " + formato
                            + ". Acote los filtros aplicados.");
        }

        ReportGenerator generador = generadoresPorFormato.get(formato);
        GeneratedDocument documento = generador.generar(modelo);

        log.info("Reporte '{}' exportado en {}: {} filas, {} bytes",
                modelo.getTitulo(), formato, totalFilas, documento.contenido().length);

        String nombreArchivo = nombreArchivo(modelo.getTitulo(), formato);
        return new GeneratedDocument(documento.contenido(), documento.contentType(), nombreArchivo);
    }

    private String nombreArchivo(String titulo, ReportFormat formato) {
        String slug = CARACTERES_NO_SLUG.matcher(titulo.toLowerCase(Locale.ROOT)).replaceAll("_");
        slug = slug.replaceAll("^_+|_+$", "");
        if (slug.isEmpty()) {
            slug = "reporte";
        }
        String marcaTiempo = FORMATO_MARCA_TIEMPO.format(
                java.time.LocalDateTime.now(ValueFormatter.zona()));
        return slug + "_" + marcaTiempo + "." + formato.extension();
    }
}
