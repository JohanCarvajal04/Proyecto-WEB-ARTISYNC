package uteq.edu.ec.artisync.service.shared.reporte.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormateadorValores;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.GeneradorReporte;
import uteq.edu.ec.artisync.service.shared.reporte.IServicioExportacion;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;

import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ServicioExportacionImpl implements IServicioExportacion {

    private static final DateTimeFormatter FORMATO_MARCA_TIEMPO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final Pattern CARACTERES_NO_SLUG = Pattern.compile("[^a-z0-9]+");

    private final Map<FormatoReporte, GeneradorReporte> generadoresPorFormato;

    public ServicioExportacionImpl(List<GeneradorReporte> generadores) {
        this.generadoresPorFormato = new EnumMap<>(FormatoReporte.class);
        for (GeneradorReporte generador : generadores) {
            GeneradorReporte previo = this.generadoresPorFormato.put(generador.formato(), generador);
            if (previo != null) {
                throw new IllegalStateException(
                        "Hay más de un GeneradorReporte registrado para el formato " + generador.formato());
            }
        }
        for (FormatoReporte formato : FormatoReporte.values()) {
            if (!this.generadoresPorFormato.containsKey(formato)) {
                throw new IllegalStateException("Falta un GeneradorReporte para el formato " + formato);
            }
        }
    }

    @Override
    public <T> DocumentoGenerado exportar(ModeloReporte<T> modelo, FormatoReporte formato) {
        int totalFilas = modelo.getFilas().size();
        if (totalFilas > formato.topeFilas()) {
            throw new ExcepcionReglaNegocio(
                    "El reporte '" + modelo.getTitulo() + "' tiene " + totalFilas + " filas, más de las "
                            + formato.topeFilas() + " que admite una exportación en " + formato
                            + ". Acote los filtros aplicados.");
        }

        GeneradorReporte generador = generadoresPorFormato.get(formato);
        DocumentoGenerado documento = generador.generar(modelo);

        log.info("Reporte '{}' exportado en {}: {} filas, {} bytes",
                modelo.getTitulo(), formato, totalFilas, documento.contenido().length);

        String nombreArchivo = nombreArchivo(modelo.getTitulo(), formato);
        return new DocumentoGenerado(documento.contenido(), documento.contentType(), nombreArchivo);
    }

    private String nombreArchivo(String titulo, FormatoReporte formato) {
        String slug = CARACTERES_NO_SLUG.matcher(titulo.toLowerCase(Locale.ROOT)).replaceAll("_");
        slug = slug.replaceAll("^_+|_+$", "");
        if (slug.isEmpty()) {
            slug = "reporte";
        }
        String marcaTiempo = FORMATO_MARCA_TIEMPO.format(
                java.time.LocalDateTime.now(FormateadorValores.zona()));
        return slug + "_" + marcaTiempo + "." + formato.extension();
    }
}
