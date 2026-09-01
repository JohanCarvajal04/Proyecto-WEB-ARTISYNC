import { describe, expect, it } from 'vitest';
import { NAV_CATALOG } from '../../../core/config/nav.config';
import { NAV_ICON_PATHS, NAV_ICON_FALLBACK } from './nav-icon.paths';

describe('mapa de iconos de navegación', () => {
  it('cubre todos los iconos declarados en NAV_CATALOG', () => {
    // Sin esto, `public`, `gavel`, `account_tree`, `description`, `assignment`
    // y `notifications` caían en el @else de los layouts viejos y seis
    // entradas del menú se dibujaban como una hamburguesa idéntica.
    const faltantes = [...new Set(NAV_CATALOG.map(i => i.icon))].filter(n => !(n in NAV_ICON_PATHS));
    expect(faltantes).toEqual([]);
  });

  it('ningún icono del catálogo resuelve al respaldo', () => {
    const alRespaldo = NAV_CATALOG.filter(i => NAV_ICON_PATHS[i.icon] === NAV_ICON_FALLBACK);
    expect(alRespaldo.map(i => i.label)).toEqual([]);
  });

  it('todo path es un atributo `d` válido en una sola línea', () => {
    for (const [nombre, d] of Object.entries(NAV_ICON_PATHS)) {
      expect(d.startsWith('M'), nombre).toBe(true);
      expect(d.includes('\n'), nombre).toBe(false);
    }
  });
});
