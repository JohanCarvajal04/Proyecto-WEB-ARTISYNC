import { describe, expect, it } from 'vitest';
import {
  NAV_CATALOG, NavItem, PanelId, resolvePanel, navItemPath, findNavLabel, PANEL_BASE_PATH,
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
    expect(resolvePanel(['ROLE_MODERADOR'], ['PANEL_MODERACION_VER'])).toBe('admin');
  });

  // ── Regresión: el rol no debe cambiar al editarle los permisos ────────────

  it('mantiene a un MODERADOR con permisos en el panel de administración', () => {
    // El bug original: al quitarle los permisos administrativos, dejaban de
    // cumplirse las condiciones de admin y de creador, y caía en el panel de
    // CLIENTE. Con al menos un permiso, el rol sigue mandando.
    expect(resolvePanel(['MODERADOR'], ['PANEL_MODERACION_VER'])).toBe('admin');
  });

  it('no convierte a un MODERADOR en CREADOR por un permiso suelto de creador', () => {
    // Si tras editarle los permisos le queda suelto un SORTEO_CREAR o un
    // PEDIDO_GESTIONAR —discriminantes del panel de creador—, el moderador no
    // debe aparecer como creador: el rol sigue mandando mientras tenga algún
    // permiso.
    expect(resolvePanel(['MODERADOR'], ['SORTEO_CREAR'])).toBe('admin');
    expect(resolvePanel(['MODERADOR'], ['PEDIDO_GESTIONAR'])).toBe('admin');
  });

  it('manda al panel de cuenta a cualquier rol del seed que se quede sin permisos', () => {
    // Antes esta regla no existía: un rol conocido vaciado de permisos se
    // quedaba fijo en su panel, pero la PUERTA de ese panel (app.routes.ts)
    // exige tener al menos uno, así que homeRoute() lo mandaba a una página
    // que la propia puerta le negaba — un rebote en bucle contra
    // /no-autorizado. 'cuenta' es alcanzable sin ningún permiso.
    expect(resolvePanel(['ADMIN'], [])).toBe('cuenta');
    expect(resolvePanel(['MODERADOR'], [])).toBe('cuenta');
    expect(resolvePanel(['SOPORTE'], [])).toBe('cuenta');
    expect(resolvePanel(['AUDITOR_FINANCIERO'], [])).toBe('cuenta');
    expect(resolvePanel(['CREADOR'], [])).toBe('cuenta');
    expect(resolvePanel(['CLIENTE'], [])).toBe('cuenta');
  });

  it('el menú del panel de cuenta trae solo notificaciones y configuración propia', () => {
    // Un moderador sin permisos deja de ser tratado como moderador y pasa a
    // ser una cuenta activa sin funciones asignadas: ni su menú ni el de
    // ningún otro panel, solo lo mínimo que no exige permiso.
    const menu = menuDe('MODERADOR', []);
    expect(menu.every(i => i.panel === 'cuenta')).toBe(true);
    expect(menu.map(i => i.route)).toEqual(['notificaciones', 'configuracion']);
    expect(menu.some(i => i.permissions?.length)).toBe(false);
  });

  // ── Roles personalizados: ahí sí se infiere ───────────────────────────────

  it('infiere el panel de un rol personalizado a partir de sus permisos', () => {
    expect(resolvePanel(['SUPERVISOR'], ['USUARIO_VER'])).toBe('admin');
    expect(resolvePanel(['SUPERVISOR'], ['SERVICIO_CREAR'])).toBe('creador');
  });

  it('manda al panel de cuenta a un rol personalizado sin permisos, no al de cliente', () => {
    // El bug reportado: antes caía en el `return 'cliente'` final y aparecía
    // con el menú completo de cliente, pero el backend le negaba con 403 cada
    // acción porque no tiene el rol CLIENTE ni ningún permiso.
    expect(resolvePanel(['SUPERVISOR'], [])).toBe('cuenta');
  });

  it('aplica la precedencia admin > creador > cliente con varios roles', () => {
    expect(resolvePanel(['CLIENTE', 'MODERADOR'], ['PANEL_MODERACION_VER'])).toBe('admin');
    expect(resolvePanel(['CLIENTE', 'CREADOR'], ['SERVICIO_CREAR'])).toBe('creador');
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

  it('"Mi Cuenta" está siempre en el menú, sin importar el panel activo', () => {
    // El bug reportado: un rol que ganaba un par de permisos administrativos
    // dejaba de estar en el panel 'cuenta' y perdía la única forma de llegar a
    // cambiar su contraseña o el 2FA. "Mi Cuenta" debe sobrevivir en los
    // cuatro paneles, apuntando siempre a la misma página compartida.
    for (const rol of Object.keys(PERMISOS_SEED)) {
      const miCuenta = menuDeRolDelSeed(rol).find(i => i.route === 'configuracion');
      expect(miCuenta, `${rol} se quedó sin "Mi Cuenta" en el menú`).toBeDefined();
      expect(miCuenta?.basePath, `${rol}: "Mi Cuenta" no apunta a /cuenta`).toBe('/cuenta');
    }
    // También para un rol personalizado con solo un par de permisos sueltos.
    const miCuentaSupervisor = menuDe('SUPERVISOR', ['PAIS_VER', 'PAIS_EDITAR'])
      .find(i => i.route === 'configuracion');
    expect(miCuentaSupervisor).toBeDefined();
    expect(miCuentaSupervisor?.basePath).toBe('/cuenta');
  });
});

describe('coherencia del catálogo', () => {
  it('no repite rutas dentro de un mismo panel', () => {
    for (const panel of ['admin', 'creador', 'cliente', 'cuenta'] as PanelId[]) {
      const rutas = NAV_CATALOG.filter(i => i.panel === panel).map(i => i.route);
      expect(new Set(rutas).size, `rutas duplicadas en el panel ${panel}`).toBe(rutas.length);
    }
  });

  it('no repite etiquetas dentro de un mismo panel (las plantillas usan track item.label)', () => {
    for (const panel of ['admin', 'creador', 'cliente', 'cuenta'] as PanelId[]) {
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

  it('la ruta de un ítem respeta basePath en vez del prefijo de su panel', () => {
    // "Mi Cuenta" figura en el menú de admin/creador/cliente pero vive en
    // /cuenta. Concatenando el prefijo del panel salía /admin/configuracion,
    // que no existe: quien aterrizara ahí caía en el wildcard -> /auth/login.
    const miCuentaAdmin = NAV_CATALOG.find(i => i.panel === 'admin' && i.route === 'configuracion')!;
    expect(navItemPath(miCuentaAdmin, PANEL_BASE_PATH['admin'])).toBe('/cuenta/configuracion');

    const paises = NAV_CATALOG.find(i => i.panel === 'admin' && i.route === 'paises')!;
    expect(navItemPath(paises, PANEL_BASE_PATH['admin'])).toBe('/admin/paises');
  });

  it('findNavLabel devuelve la etiqueta correcta para la URL de cada ítem', () => {
    // No comprueba que la ruta esté montada en el router —eso no se ve desde
    // aquí—, sino que el emparejamiento URL->sección no se equivoca de ítem
    // cuando dos paneles comparten el mismo `route`.
    for (const panel of ['admin', 'creador', 'cliente', 'cuenta'] as PanelId[]) {
      for (const item of NAV_CATALOG.filter(i => i.panel === panel)) {
        const url = navItemPath(item, PANEL_BASE_PATH[panel]);
        expect(findNavLabel(url), `${url} no resuelve a ninguna sección del catálogo`).toBe(item.label);
      }
    }
  });

  it('ninguna página del panel de cuenta exige permiso: es la zona sin permisos', () => {
    // Si alguna llevara `permissions`, un usuario sin ningún permiso —para
    // quien resolvePanel() elige precisamente este panel— se quedaría sin
    // ninguna página visible.
    const conPermiso = NAV_CATALOG.filter(i => i.panel === 'cuenta' && i.permissions?.length);
    expect(conPermiso).toEqual([]);
  });
});
