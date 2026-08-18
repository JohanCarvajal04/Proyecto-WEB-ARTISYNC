import { describe, expect, it } from 'vitest';
import {
  NAV_CATALOG, NavItem, PanelId, resolvePanel,
  ADMIN_PANEL_PERMISSIONS, CREADOR_PANEL_PERMISSIONS
} from './nav.config';

/**
 * Permisos de cada rol del seed (db/seed.sql + V10__permisos_navegacion.sql).
 * Sirven para comprobar que el menú por permisos reproduce lo que cada rol veía
 * cuando la navegación se indexaba por nombre de rol.
 */
const PERMISOS_SEED: Record<string, string[]> = {
  ADMIN: [], // se rellena abajo con todos los permisos existentes
  MODERADOR: [
    'PORTAFOLIO_MODERAR', 'CERTIFICADO_REVISAR', 'CATEGORIA_GESTIONAR', 'SERVICIO_MODERAR',
    'MENSAJE_MODERAR', 'NOTIFICACION_ENVIAR', 'COMENTARIO_MODERAR', 'PAIS_VER', 'ROL_VER',
    'PANEL_MODERACION_VER'
  ],
  SOPORTE: [
    'USUARIO_VER', 'USUARIO_SUSPENDER', 'ROL_VER', 'PERMISO_VER', 'SESION_REVOCAR',
    'TICKET_REVISAR', 'TICKET_RESOLVER', 'SALA_VER', 'NOTIFICACION_ENVIAR', 'PAIS_VER'
  ],
  AUDITOR_FINANCIERO: [
    'CONTRATO_VER', 'PAGO_AUDITAR', 'FONDOS_LIBERAR', 'TRANSACCION_VER', 'PAIS_VER', 'ROL_VER'
  ],
  CREADOR: [
    'PORTAFOLIO_CREAR', 'SERVICIO_CREAR', 'PEDIDO_GESTIONAR', 'TICKET_REVISAR',
    'CONTRATO_VER', 'CONTRATO_FIRMAR', 'SALA_VER', 'MENSAJE_ENVIAR', 'SORTEO_CREAR'
  ],
  CLIENTE: [
    'PEDIDO_CREAR', 'TICKET_REVISAR', 'CONTRATO_VER', 'CONTRATO_FIRMAR',
    'SALA_VER', 'MENSAJE_ENVIAR'
  ]
};
PERMISOS_SEED['ADMIN'] = [
  ...new Set([
    ...ADMIN_PANEL_PERMISSIONS, ...CREADOR_PANEL_PERMISSIONS,
    ...Object.values(PERMISOS_SEED).flat()
  ])
];

/**
 * Réplica de AuthService.visibleNavItems. El panel se obtiene de la función
 * real `resolvePanel`, no de una copia: una copia puede seguir pasando mientras
 * el código de producción está roto, que es justo lo que ocurrió con el salto
 * de panel al editar permisos.
 */
function menuDe(rol: string, permisos: string[]): NavItem[] {
  const panel = resolvePanel([rol], permisos);
  return NAV_CATALOG.filter(i =>
    i.panel === panel && (!i.permissions?.length || i.permissions.some(p => permisos.includes(p)))
  );
}

const menuDeRolDelSeed = (rol: string) => menuDe(rol, PERMISOS_SEED[rol]);

describe('resolución del panel', () => {
  it('asigna a cada rol del seed su panel', () => {
    expect(resolvePanel(['ADMIN'], PERMISOS_SEED['ADMIN'])).toBe('admin');
    expect(resolvePanel(['MODERADOR'], PERMISOS_SEED['MODERADOR'])).toBe('admin');
    expect(resolvePanel(['SOPORTE'], PERMISOS_SEED['SOPORTE'])).toBe('admin');
    expect(resolvePanel(['AUDITOR_FINANCIERO'], PERMISOS_SEED['AUDITOR_FINANCIERO'])).toBe('admin');
    expect(resolvePanel(['CREADOR'], PERMISOS_SEED['CREADOR'])).toBe('creador');
    expect(resolvePanel(['CLIENTE'], PERMISOS_SEED['CLIENTE'])).toBe('cliente');
  });

  it('tolera el prefijo ROLE_ que añade Spring Security', () => {
    expect(resolvePanel(['ROLE_MODERADOR'], [])).toBe('admin');
  });

  // ── Regresión: el rol no debe cambiar al editarle los permisos ────────────

  it('mantiene a un MODERADOR sin permisos en el panel de administración', () => {
    // El bug: al quitarle los permisos, dejaban de cumplirse las condiciones de
    // admin y de creador, y caía en el panel de CLIENTE.
    expect(resolvePanel(['MODERADOR'], [])).toBe('admin');
  });

  it('no convierte a un MODERADOR en CREADOR por un permiso suelto de creador', () => {
    // Peor variante: si tras la poda le quedaba SORTEO_CREAR o PEDIDO_GESTIONAR
    // —discriminantes del panel de creador— el moderador aparecía como creador.
    expect(resolvePanel(['MODERADOR'], ['SORTEO_CREAR'])).toBe('admin');
    expect(resolvePanel(['MODERADOR'], ['PEDIDO_GESTIONAR'])).toBe('admin');
  });

  it('mantiene el panel de cada rol del seed aunque se le vacíen los permisos', () => {
    expect(resolvePanel(['ADMIN'], [])).toBe('admin');
    expect(resolvePanel(['SOPORTE'], [])).toBe('admin');
    expect(resolvePanel(['AUDITOR_FINANCIERO'], [])).toBe('admin');
    expect(resolvePanel(['CREADOR'], [])).toBe('creador');
    expect(resolvePanel(['CLIENTE'], [])).toBe('cliente');
  });

  it('deja el menú vacío, no otro panel, cuando el rol se queda sin permisos', () => {
    // Sin ítems visibles, homeRoute() manda a /no-autorizado, que explica que el
    // rol no tiene permisos asignados. Un moderador sin permisos es un moderador
    // sin permisos, no un cliente.
    const menu = menuDe('MODERADOR', []);
    expect(menu.every(i => i.panel === 'admin')).toBe(true);
    expect(menu.some(i => i.permissions?.length)).toBe(false);
  });

  // ── Roles personalizados: ahí sí se infiere ───────────────────────────────

  it('infiere el panel de un rol personalizado a partir de sus permisos', () => {
    expect(resolvePanel(['SUPERVISOR'], ['USUARIO_VER'])).toBe('admin');
    expect(resolvePanel(['SUPERVISOR'], ['SERVICIO_CREAR'])).toBe('creador');
    expect(resolvePanel(['SUPERVISOR'], [])).toBe('cliente');
  });

  it('aplica la precedencia admin > creador > cliente con varios roles', () => {
    expect(resolvePanel(['CLIENTE', 'MODERADOR'], [])).toBe('admin');
    expect(resolvePanel(['CLIENTE', 'CREADOR'], [])).toBe('creador');
  });

  it('no confunde a un cliente con un creador ni con un administrador', () => {
    const compartidos = ['TICKET_REVISAR', 'CONTRATO_VER', 'CONTRATO_FIRMAR', 'SALA_VER', 'MENSAJE_ENVIAR'];
    for (const permiso of compartidos) {
      expect(ADMIN_PANEL_PERMISSIONS).not.toContain(permiso);
      expect(CREADOR_PANEL_PERMISSIONS).not.toContain(permiso);
    }
  });
});

describe('menú visible según permisos', () => {
  it('conserva la página de aterrizaje de cada rol del seed', () => {
    // El orden de NAV_CATALOG define el aterrizaje: primera entrada visible.
    expect(menuDeRolDelSeed('ADMIN')[0].route).toBe('users');
    expect(menuDeRolDelSeed('MODERADOR')[0].route).toBe('mod-overview');
    expect(menuDeRolDelSeed('SOPORTE')[0].route).toBe('users');
    expect(menuDeRolDelSeed('CREADOR')[0].route).toBe('overview');
    expect(menuDeRolDelSeed('CLIENTE')[0].route).toBe('overview');
  });

  it('muestra Gestión de Países con CUALQUIERA de los cuatro permisos del módulo', () => {
    // Un rol con los tres permisos de escritura pero sin PAIS_VER gestionaba
    // países y aun así no veía la opción en el menú.
    for (const permiso of ['PAIS_VER', 'PAIS_CREAR', 'PAIS_EDITAR', 'PAIS_ELIMINAR']) {
      const rutas = menuDe('SUPERVISOR', [permiso]).map(i => i.route);
      expect(rutas, `con ${permiso} debería verse la pantalla de países`).toContain('paises');
    }
  });

  it('muestra Gestión de Usuarios con cualquiera de los permisos de usuario', () => {
    for (const permiso of ['USUARIO_VER', 'USUARIO_CREAR', 'USUARIO_EDITAR', 'USUARIO_ELIMINAR', 'USUARIO_SUSPENDER']) {
      const rutas = menuDe('SUPERVISOR', [permiso]).map(i => i.route);
      expect(rutas, `con ${permiso} debería verse la pantalla de usuarios`).toContain('users');
    }
  });

  it('enseña una opción por cada módulo concedido, no solo una', () => {
    const rutas = menuDe('SUPERVISOR', ['PAIS_EDITAR', 'USUARIO_CREAR', 'CERTIFICADO_REVISAR']).map(i => i.route);

    expect(rutas).toContain('paises');
    expect(rutas).toContain('users');
    expect(rutas).toContain('verificaciones');
  });

  it('un rol nuevo con un único permiso solo ve esa página y las páginas base', () => {
    const menu = menuDe('SUPERVISOR', ['PAIS_VER']);
    const conPermiso = menu.filter(i => i.permissions?.length);

    expect(conPermiso.map(i => i.route)).toEqual(['paises']);
  });

  it('no mezcla ítems de paneles distintos', () => {
    for (const rol of Object.keys(PERMISOS_SEED)) {
      const paneles = new Set(menuDeRolDelSeed(rol).map(i => i.panel));
      expect(paneles.size, `el menú de ${rol} mezcla paneles`).toBeLessThanOrEqual(1);
    }
  });

  it('nunca enseña una página administrativa a un cliente', () => {
    expect(menuDeRolDelSeed('CLIENTE').some(i => i.panel === 'admin')).toBe(false);
  });
});

describe('coherencia del catálogo', () => {
  it('no repite rutas dentro de un mismo panel', () => {
    for (const panel of ['admin', 'creador', 'cliente'] as PanelId[]) {
      const rutas = NAV_CATALOG.filter(i => i.panel === panel).map(i => i.route);
      expect(new Set(rutas).size, `rutas duplicadas en el panel ${panel}`).toBe(rutas.length);
    }
  });

  it('no repite etiquetas dentro de un mismo panel (las plantillas usan track item.label)', () => {
    for (const panel of ['admin', 'creador', 'cliente'] as PanelId[]) {
      const labels = NAV_CATALOG.filter(i => i.panel === panel).map(i => i.label);
      expect(new Set(labels).size, `etiquetas duplicadas en el panel ${panel}`).toBe(labels.length);
    }
  });

  it('declara en ADMIN_PANEL_PERMISSIONS todos los permisos que exigen las páginas del panel admin', () => {
    // Un rol personalizado cuyo único permiso no abriera el panel se quedaría en
    // la puerta y jamás llegaría a la pantalla que ese permiso concede.
    const exigidos = NAV_CATALOG.filter(i => i.panel === 'admin').flatMap(i => i.permissions ?? []);
    for (const permiso of exigidos) {
      expect(ADMIN_PANEL_PERMISSIONS, `${permiso} no abre el panel admin`).toContain(permiso);
    }
  });
});
