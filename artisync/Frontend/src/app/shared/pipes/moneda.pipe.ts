import { Pipe, PipeTransform } from '@angular/core';

/**
 * `new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })`
 * estaba copiado literal en 10 componentes (revisión técnica, 2026-09-01).
 * Se consolida aquí, mismo patrón que `shared/utils/params-desde-filtro.ts`.
 */
const FORMATO_USD = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

@Pipe({
  name: 'moneda',
  standalone: true
})
export class MonedaPipe implements PipeTransform {
  transform(valor: number | null | undefined): string {
    if (valor === null || valor === undefined || Number.isNaN(valor)) return '';
    return FORMATO_USD.format(valor);
  }
}
