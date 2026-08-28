import { HttpParams } from '@angular/common/http';

/**
 * Serializa un objeto de filtro a `HttpParams`, omitiendo claves
 * `undefined`/`null`/`''` (para no mandar `?rol=&estado=` al backend).
 *
 * Antes esta lógica estaba triplicada como un `aParams` privado idéntico en
 * `AuditoriaService`, `ReporteContratoService` y `ReporteFinancieroService`
 * (hallazgo 1.5, INFORME-REVISION-COMPLETA.md) — se consolida aquí al
 * necesitar un cuarto uso para `AdminUserService` (1.3).
 */
export function paramsDesdeFiltro(filtro: object): HttpParams {
  let params = new HttpParams();
  for (const [clave, valor] of Object.entries(filtro)) {
    if (valor !== undefined && valor !== null && valor !== '') {
      params = params.set(clave, String(valor));
    }
  }
  return params;
}
