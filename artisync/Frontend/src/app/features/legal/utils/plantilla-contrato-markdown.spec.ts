import { describe, expect, it } from 'vitest';
import {
  htmlPlantillaAMarkdown,
  markdownAHtmlFragmento,
  markdownAHtmlPlantilla,
  PLACEHOLDERS_PLANTILLA_CONTRATO
} from './plantilla-contrato-markdown';

describe('markdownAHtmlPlantilla', () => {
  it('envuelve el resultado en un documento HTML con <html><body>, como espera ContratoServicioImpl', () => {
    const html = markdownAHtmlPlantilla('# Título\n\nUn párrafo.');
    expect(html).toContain('<html');
    expect(html).toContain('<body>');
    expect(html).toContain('</body>');
    expect(html).toContain('<h1>Título</h1>');
    expect(html).toContain('<p>Un párrafo.</p>');
  });

  it('conserva los placeholders {{...}} sin alterarlos, incluido el guion bajo', () => {
    const markdown = 'Entre **{{nombre_creador}}** y **{{nombre_cliente}}**, el {{fecha_actual}}.';
    const html = markdownAHtmlPlantilla(markdown);

    for (const placeholder of PLACEHOLDERS_PLANTILLA_CONTRATO) {
      if (markdown.includes(placeholder)) {
        expect(html).toContain(placeholder);
      }
    }
    expect(html).toContain('<strong>{{nombre_creador}}</strong>');
  });

  it('cierra las etiquetas vacías (xhtmlOut) para el parser XML estricto de openhtmltopdf', () => {
    const html = markdownAHtmlPlantilla('Antes\n\n---\n\nDespués');
    expect(html).toContain('<hr />');
    expect(html).not.toContain('<hr>');
  });

  it('escapa cualquier etiqueta que el admin escriba a mano, en vez de dejarla pasar sin validar', () => {
    const html = markdownAHtmlPlantilla('Texto con <script>alert(1)</script> incrustado');
    expect(html).not.toContain('<script>');
    expect(html).toContain('&lt;script&gt;');
  });
});

describe('markdownAHtmlFragmento', () => {
  it('no incluye el envoltorio de documento (solo el fragmento, para la vista previa)', () => {
    const fragmento = markdownAHtmlFragmento('Hola');
    expect(fragmento).not.toContain('<html');
    expect(fragmento).toContain('<p>Hola</p>');
  });
});

describe('htmlPlantillaAMarkdown', () => {
  it('reconstruye títulos, párrafos y negritas a partir de un documento HTML completo', () => {
    const html = `<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"></head><body>
      <h1>Contrato de Prestación de Servicios Creativos</h1>
      <p>Entre <strong>{{nombre_creador}}</strong> y <strong>{{nombre_cliente}}</strong>.</p>
    </body></html>`;

    const markdown = htmlPlantillaAMarkdown(html);

    expect(markdown).toContain('# Contrato de Prestación de Servicios Creativos');
    expect(markdown).toContain('**{{nombre_creador}}**');
    expect(markdown).toContain('**{{nombre_cliente}}**');
  });

  it('ida y vuelta: markdown -> html -> markdown conserva títulos, párrafos y placeholders', () => {
    const original = '# Contrato\n\nEntre **{{nombre_creador}}** y **{{nombre_cliente}}**.\n\n## Precio\n\nEl monto es {{precio_pactado}} USD.';

    const html = markdownAHtmlPlantilla(original);
    const reconstruido = htmlPlantillaAMarkdown(html);

    expect(reconstruido).toContain('# Contrato');
    expect(reconstruido).toContain('## Precio');
    expect(reconstruido).toContain('**{{nombre_creador}}**');
    expect(reconstruido).toContain('{{precio_pactado}}');
  });
});
