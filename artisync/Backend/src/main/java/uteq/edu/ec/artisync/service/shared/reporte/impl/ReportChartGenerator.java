package uteq.edu.ec.artisync.service.shared.reporte.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera gráficas estadísticas en formato PNG de alta fidelidad para reportes PDF y Excel
 * utilizando Java2D nativo (Graphics2D) en modo headless, sin dependencias externas pesadas.
 * Utiliza la paleta de colores oficial de Artisync (#7B39B2, #9C5CC5, #1E0F38).
 */
@Slf4j
@Component
public class ReportChartGenerator {

    private static final int ANCHO = 560;
    private static final int ALTO = 300;

    // Paleta corporativa Artisync
    private static final Color COLOR_TEXTO_TITULO = new Color(30, 15, 56);       // #1E0F38
    private static final Color COLOR_TEXTO_MUTED = new Color(100, 116, 139);     // #64748B
    private static final Color COLOR_BORDE_TARJETA = new Color(233, 213, 255);    // #E9D5FF
    private static final Color COLOR_FONDO_BARRA = new Color(241, 245, 249);     // #F1F5F9

    private static final Color MORADO_PRIMARIO = new Color(123, 57, 178);        // #7B39B2
    private static final Color MORADO_CLARO = new Color(156, 92, 197);           // #9C5CC5

    private static final Color[] PALETA_ROLES = {
            new Color(123, 57, 178), // Creador - #7B39B2
            new Color(59, 130, 246),  // Cliente - #3B82F6
            new Color(16, 185, 129),  // Admin - #10B981
            new Color(245, 158, 11),  // Moderador - #F59E0B
            new Color(236, 72, 153),  // Soporte - #EC4899
            new Color(139, 92, 246),  // Auditor - #8B5CF6
            new Color(99, 102, 241),  // Índigo
            new Color(20, 184, 166)   // Teal
    };

    /**
     * Genera una gráfica moderna tipo Donut de usuarios agrupados por rol.
     */
    public byte[] generarGraficaRol(Map<String, Long> datosRol) {
        BufferedImage imagen = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = configurarGraphics(imagen);

        try {
            dibujarFondoTarjeta(g, "Distribución de Usuarios por Role", "Proporción de usuarios según su rol asignado");

            long total = datosRol.values().stream().mapToLong(Long::longValue).sum();
            if (total == 0) {
                dibujarMensajeSinDatos(g);
                return convertirAPng(imagen);
            }

            // Dimensiones y posición del Donut
            int centroX = 140;
            int centroY = 175;
            int radioExterior = 85;
            int radioInterior = 52;

            double anguloInicial = 90.0;
            int colorIdx = 0;

            List<Map.Entry<String, Long>> entradas = new ArrayList<>(datosRol.entrySet());
            entradas.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

            for (Map.Entry<String, Long> entry : entradas) {
                double porcion = (double) entry.getValue() / total;
                double extension = porcion * 360.0;
                Color color = PALETA_ROLES[colorIdx % PALETA_ROLES.length];

                // Dibujar sector exterior
                g.setColor(color);
                Arc2D sector = new Arc2D.Double(
                        centroX - radioExterior, centroY - radioExterior,
                        radioExterior * 2, radioExterior * 2,
                        anguloInicial, -extension, Arc2D.PIE);
                g.fill(sector);

                // Separador blanco entre arcos si hay más de una categoría
                if (entradas.size() > 1) {
                    g.setColor(Color.WHITE);
                    g.setStroke(new BasicStroke(2.0f));
                    g.draw(sector);
                }

                anguloInicial -= extension;
                colorIdx++;
            }

            // Agujero central para efecto Donut
            g.setColor(Color.WHITE);
            g.fillOval(centroX - radioInterior, centroY - radioInterior, radioInterior * 2, radioInterior * 2);
            g.setColor(new Color(243, 232, 255)); // Borde sutil al agujero
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(centroX - radioInterior, centroY - radioInterior, radioInterior * 2, radioInterior * 2);

            // Texto central: Total de usuarios
            g.setColor(COLOR_TEXTO_TITULO);
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            String totalTexto = String.valueOf(total);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(totalTexto, centroX - (fm.stringWidth(totalTexto) / 2), centroY + 2);

            g.setColor(COLOR_TEXTO_MUTED);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            fm = g.getFontMetrics();
            g.drawString("USUARIOS", centroX - (fm.stringWidth("USUARIOS") / 2), centroY + 17);

            // Leyenda a la derecha
            int leyendaX = 265;
            int leyendaY = 100;
            int altoFila = 26;
            colorIdx = 0;

            int maxItems = Math.min(entradas.size(), 6);
            for (int i = 0; i < maxItems; i++) {
                Map.Entry<String, Long> entry = entradas.get(i);
                Color color = PALETA_ROLES[colorIdx % PALETA_ROLES.length];
                int yFila = leyendaY + (i * altoFila);

                // Indicador circular
                g.setColor(color);
                g.fillOval(leyendaX, yFila, 12, 12);

                // Nombre del rol
                g.setColor(COLOR_TEXTO_TITULO);
                g.setFont(new Font("SansSerif", Font.BOLD, 11));
                String nombreLegible = formatearNombreRol(entry.getKey());
                g.drawString(nombreLegible, leyendaX + 18, yFila + 10);

                // Conteo y porcentaje
                double pct = ((double) entry.getValue() / total) * 100.0;
                String info = String.format("%d (%.1f%%)", entry.getValue(), pct);
                g.setColor(COLOR_TEXTO_MUTED);
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g.drawString(info, leyendaX + 175, yFila + 10);

                colorIdx++;
            }

            return convertirAPng(imagen);
        } finally {
            g.dispose();
        }
    }

    /**
     * Genera una gráfica moderna de barras horizontales de usuarios agrupados por país.
     */
    public byte[] generarGraficaPais(Map<String, Long> datosPais) {
        BufferedImage imagen = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = configurarGraphics(imagen);

        try {
            dibujarFondoTarjeta(g, "Distribución de Usuarios por País", "Concentración geográfica de los usuarios registrados");

            long total = datosPais.values().stream().mapToLong(Long::longValue).sum();
            if (total == 0) {
                dibujarMensajeSinDatos(g);
                return convertirAPng(imagen);
            }

            // Ordenar por cantidad descendente
            List<Map.Entry<String, Long>> entradas = new ArrayList<>(datosPais.entrySet());
            entradas.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

            // Limitar a los 6 primeros si hay más, agrupando el resto en "Otros"
            Map<String, Long> paisesFinales = new LinkedHashMap<>();
            long otros = 0;
            for (int i = 0; i < entradas.size(); i++) {
                if (i < 5) {
                    paisesFinales.put(entradas.get(i).getKey(), entradas.get(i).getValue());
                } else {
                    otros += entradas.get(i).getValue();
                }
            }
            if (otros > 0) {
                paisesFinales.put("Otros", otros);
            }

            long maxValor = paisesFinales.values().stream().mapToLong(Long::longValue).max().orElse(1L);

            int inicioX = 145;
            int inicioY = 95;
            int anchoBarraMax = 320;
            int altoBarra = 18;
            int espaciado = 30;

            int fila = 0;
            for (Map.Entry<String, Long> entry : paisesFinales.entrySet()) {
                int y = inicioY + (fila * espaciado);
                String pais = entry.getKey() == null || entry.getKey().isBlank() ? "Sin especificar" : entry.getKey();
                long cantidad = entry.getValue();

                // Nombre del país alineado a la izquierda
                g.setColor(COLOR_TEXTO_TITULO);
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                FontMetrics fm = g.getFontMetrics();
                int textoX = inicioX - fm.stringWidth(pais) - 12;
                g.drawString(pais, Math.max(16, textoX), y + 13);

                // Barra de fondo (escala completa)
                g.setColor(COLOR_FONDO_BARRA);
                g.fillRoundRect(inicioX, y, anchoBarraMax, altoBarra, 8, 8);

                // Barra con valor proporcional y degradado morado
                int anchoCalculado = (int) Math.round(((double) cantidad / maxValor) * anchoBarraMax);
                int anchoFinal = Math.max(10, anchoCalculado);

                GradientPaint gradiente = new GradientPaint(
                        inicioX, y, MORADO_CLARO,
                        inicioX + anchoFinal, y, MORADO_PRIMARIO);
                g.setPaint(gradiente);
                g.fillRoundRect(inicioX, y, anchoFinal, altoBarra, 8, 8);

                // Conteo y porcentaje al final de la barra
                double pct = ((double) cantidad / total) * 100.0;
                String etiquetaValor = String.format("%d (%.1f%%)", cantidad, pct);
                g.setColor(COLOR_TEXTO_TITULO);
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                g.drawString(etiquetaValor, inicioX + anchoFinal + 8, y + 13);

                fila++;
            }

            return convertirAPng(imagen);
        } finally {
            g.dispose();
        }
    }

    private Graphics2D configurarGraphics(BufferedImage imagen) {
        Graphics2D g = imagen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g;
    }

    private void dibujarFondoTarjeta(Graphics2D g, String titulo, String subtitulo) {
        // Fondo blanco con borde redondeado suave
        g.setColor(Color.WHITE);
        g.fillRoundRect(2, 2, ANCHO - 4, ALTO - 4, 16, 16);

        g.setColor(COLOR_BORDE_TARJETA);
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(2, 2, ANCHO - 4, ALTO - 4, 16, 16);

        // Barra decorativa morada superior
        GradientPaint barraTop = new GradientPaint(16, 12, MORADO_PRIMARIO, 150, 12, MORADO_CLARO);
        g.setPaint(barraTop);
        g.fillRoundRect(16, 12, 34, 4, 2, 2);

        // Título de la gráfica
        g.setColor(COLOR_TEXTO_TITULO);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(titulo, 16, 36);

        // Subtítulo
        g.setColor(COLOR_TEXTO_MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.drawString(subtitulo, 16, 52);

        // Línea sutil divisoria
        g.setColor(new Color(241, 245, 249));
        g.drawLine(16, 62, ANCHO - 16, 62);
    }

    private void dibujarMensajeSinDatos(Graphics2D g) {
        g.setColor(COLOR_TEXTO_MUTED);
        g.setFont(new Font("SansSerif", Font.ITALIC, 12));
        String msg = "No hay datos suficientes para graficar.";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(msg, (ANCHO - fm.stringWidth(msg)) / 2, ALTO / 2);
    }

    private String formatearNombreRol(String rol) {
        if (rol == null) return "Sin Role";
        return switch (rol.toUpperCase()) {
            case "ADMIN", "ROLE_ADMIN" -> "Administrador";
            case "CREADOR", "ROLE_CREADOR" -> "Creador";
            case "CLIENTE", "ROLE_CLIENTE" -> "Cliente";
            case "MODERADOR", "ROLE_MODERADOR" -> "Moderador";
            case "SOPORTE", "ROLE_SOPORTE" -> "Soporte";
            case "AUDITOR_FINANCIERO", "ROLE_AUDITOR_FINANCIERO" -> "Auditor Financiero";
            default -> rol.replace("ROLE_", "");
        };
    }

    private byte[] convertirAPng(BufferedImage imagen) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error al exportar gráfica a formato PNG", e);
            throw new IllegalStateException("Error al renderizar la gráfica: " + e.getMessage(), e);
        }
    }
}
