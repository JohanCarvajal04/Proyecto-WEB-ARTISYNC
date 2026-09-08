import MarkdownIt from 'markdown-it';
import TurndownService from 'turndown';

/**
 * El admin escribe el texto legal en Markdown (normal, sin etiquetas) en vez
 * de HTML crudo. El backend (ContratoServicioImpl / PdfGeneracionServicioImpl)
 * sigue esperando exactamente lo mismo que antes: un documento HTML completo,
 * bien formado como XML estricto (openhtmltopdf no acepta fragmentos sueltos
 * ni etiquetas vacías sin cerrar, ver V30__fix_plantilla_contrato_xhtml.sql).
 * Por eso la conversión ocurre aquí, en el propio formulario del admin, y el
 * backend no cambia: sigue recibiendo `cuerpoHtmlPlantilla` como siempre.
 */

// xhtmlOut: cierra las etiquetas vacías (<br />, <hr />) como exige el parser
// XML estricto de openhtmltopdf. html:false: cualquier etiqueta que el admin
// escriba a mano dentro del Markdown se escapa como texto en vez de colarse
// como HTML sin validar — evita producir un documento mal formado.
const md = new MarkdownIt({ xhtmlOut: true, html: false, breaks: false });

const turndown = new TurndownService({ headingStyle: 'atx', hr: '---', emDelimiter: '_' });

/** Placeholders que ContratoServicioImpl sustituye al generar el contrato de un pedido. */
export const PLACEHOLDERS_PLANTILLA_CONTRATO = [
  '{{nombre_creador}}',
  '{{nombre_cliente}}',
  '{{descripcion_servicio}}',
  '{{precio_pactado}}',
  '{{limite_revisiones}}',
  '{{fecha_entrega}}',
  '{{fecha_actual}}'
];

/** Markdown → documento HTML completo, para guardar como `cuerpoHtmlPlantilla`. */
export function markdownAHtmlPlantilla(markdown: string): string {
  const fragmento = md.render(markdown);
  return `<!DOCTYPE html>\n<html lang="es">\n<head><meta charset="UTF-8"><title>Plantilla de contrato</title></head>\n<body>\n${fragmento}</body>\n</html>`;
}

/** Markdown → HTML sin el envoltorio de documento, solo para la vista previa en pantalla. */
export function markdownAHtmlFragmento(markdown: string): string {
  return md.render(markdown);
}

/**
 * HTML (el `cuerpoHtmlPlantilla` ya guardado) → Markdown, para poder editar
 * una plantilla existente en el mismo cuadro de texto "normal". La
 * reconstrucción es la mejor posible, no perfecta byte a byte: si la
 * plantilla no se creó desde este editor (p. ej. la sembrada por V13/V30),
 * el resultado puede no ser idéntico al ver la vista previa, pero conserva
 * títulos, párrafos y negritas.
 */
export function htmlPlantillaAMarkdown(html: string): string {
  const markdown = turndown.turndown(soloCuerpoDelDocumento(html));
  // Turndown escapa "_" a "\_" en cualquier texto (para que no se confunda
  // con énfasis al re-renderizar), pero nuestros placeholders siempre son
  // "intraword" (letras a ambos lados, sin espacios) y CommonMark ya los deja
  // como texto literal sin necesidad de escape (ver PLACEHOLDERS_PLANTILLA_CONTRATO).
  // Sin este des-escapado el admin vería "{{nombre\_creador}}" en pantalla:
  // técnicamente inofensivo (markdownAHtmlPlantilla lo revierte al guardar),
  // pero se ve roto para alguien que "solo quiere escribir normal".
  return markdown.replace(/\\_/g, '_');
}

/**
 * Si `html` es un documento completo (`<html><head>...</head><body>...`),
 * se queda solo con el contenido de <body> antes de convertir: sin esto,
 * Turndown también intenta convertir el texto de <title> (invisible en un
 * navegador real, pero Turndown no aplica CSS/layout, así que lo trata como
 * contenido normal y lo cuela al inicio del Markdown).
 */
function soloCuerpoDelDocumento(html: string): string {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  return doc.body ? doc.body.innerHTML : html;
}
