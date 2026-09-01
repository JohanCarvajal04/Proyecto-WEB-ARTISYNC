/**
 * Catálogo de navegación de la aplicación.
 *
 * Antes esto era `NAV_CONFIG: Record<nombreRol, ...>`: el menú se elegía
 * indexando por el nombre del rol. Como los roles son datos que el
 * administrador crea a voluntad, un rol nuevo no tenía entrada y el layout
 * caía a `NAV_CONFIG['ADMINISTRADOR']`, es decir, le enseñaba el menú
 * completo de administración a un rol al que nadie le había dado esos
 * permisos.
 *
 * Ahora hay un único catálogo plano: cada página declara a qué panel
 * pertenece y qué permiso la habilita. Quien manda es el permiso, y los
 * permisos sí viajan por usuario en el claim `permisos` del JWT. Asignar un
 * permiso a un rol se refleja en el menú sin tocar código.
 */

/**
 * 'cuenta' es el panel sin permisos: notificaciones y configuración de la
 * propia cuenta, nada más. Es a donde `resolvePanel` manda a cualquier
 * usuario sin ningún permiso asignado, sea cual sea su rol.
 */
export type PanelId = 'admin' | 'creador' | 'cliente' | 'cuenta';

export interface NavItem {
  label: string;
  icon: string;
  route: string;
  panel: PanelId;
  /**
   * Permisos que habilitan la página, con semántica "cualquiera de": basta con
   * tener UNO. Si se omite, es una *página base* del panel: visible para
   * cualquiera que tenga acceso a ese panel (overview, perfil,
   * notificaciones...).
   *
   * Antes era un único permiso, y eso dejaba fuera casos evidentes: la pantalla
   * de países declaraba solo PAIS_VER, así que un rol con PAIS_CREAR,
   * PAIS_EDITAR y PAIS_ELIMINAR —tres permisos de países— no veía la opción en
   * el menú. Quien puede crear o borrar un país necesita poder abrir su
   * pantalla.
   */
  permissions?: readonly string[];
  /**
   * Si se define, el enlace usa esta base en vez de PANEL_BASE_PATH[panel
   * activo]. Sirve para un ítem que aparece en el menú de varios paneles pero
   * siempre apunta a la misma página, como "Mi Cuenta" → /cuenta/configuracion
   * sin importar si el panel activo es admin, creador o cliente: la página es
   * una sola, no una copia por panel.
   */
  basePath?: string;
  /**
   * Si es true, este ítem puede aparecer en el menú de paneles distintos al
   * suyo cuando el usuario tiene el permiso requerido. El enlace usará
   * PANEL_BASE_PATH[item.panel] como basePath para apuntar a la ruta real.
   */
  crossPanel?: boolean;
}

/**
 * Permisos que dan acceso a cada pantalla, en un único sitio.
 *
 * El catálogo de navegación y los `data.permissions` de las rutas leen de aquí,
 * de modo que no puedan divergir: si difirieran, el menú enseñaría un enlace
 * que al pulsarlo llevaría a /no-autorizado (o al revés, una pantalla
 * alcanzable sin figurar en el menú).
 */
export const PAGE_PERMISSIONS = {
  // Todas las acciones sobre usuarios requieren poder abrir la pantalla.
  users: ['USUARIO_VER', 'USUARIO_CREAR', 'USUARIO_EDITAR', 'USUARIO_ELIMINAR', 'USUARIO_SUSPENDER'],
  // Los cuatro permisos del módulo de países.
  paises: ['PAIS_VER', 'PAIS_CREAR', 'PAIS_EDITAR', 'PAIS_ELIMINAR'],
  // Solo los dos que gestionan la matriz. ROL_VER y PERMISO_VER son permisos de
  // consulta que MODERADOR, SOPORTE y AUDITOR usan para poblar desplegables en
  // otras pantallas; incluirlos aquí les abriría una pantalla que hoy no ven.
  rolesPermisos: ['ROL_GESTIONAR', 'ROL_ASIGNAR_PERMISO'],
  panelModeracion: ['PANEL_MODERACION_VER'],
  verificaciones: ['CERTIFICADO_REVISAR'],
  portafoliosModeracion: ['PORTAFOLIO_MODERAR'],
  comentariosModeracion: ['COMENTARIO_MODERAR'],
  categorias: ['CATEGORIA_GESTIONAR'],
  infracciones: ['INFRACCION_GESTIONAR'],
  // Antes un único `flujos: [FLUJO_GESTIONAR, FLUJO_MODERAR]` protegía a la vez
  // la pantalla del panel admin y la del panel creador. Al ser "cualquiera de"
  // los dos, un creador con solo FLUJO_GESTIONAR también veía en su menú el
  // ítem cross-panel de admin (y viceversa un moderador con FLUJO_MODERAR veía
  // el de creador): cada permiso debe abrir solo SU pantalla.
  // FLUJO_GESTIONAR: gestiona los flujos propios (panel creador).
  flujosPropios: ['FLUJO_GESTIONAR'],
  // FLUJO_MODERAR: ve/gestiona los de todos los creadores (panel admin, p. ej.
  // el selector de flujo en Categorías).
  flujosModeracion: ['FLUJO_MODERAR'],
  // Montaje heredado en /pedido/flujos (no figura en NAV_CATALOG): sirve el
  // mismo componente y el alcance real (propios vs todos) lo decide el
  // backend en FlujoTrabajoControlador.puedeVerTodos, así que esta ruta debe
  // admitir a los dos públicos.
  flujosCompat: ['FLUJO_GESTIONAR', 'FLUJO_MODERAR'],
  // "Mis Servicios" es el CRUD propio del creador; SERVICIO_MODERAR pertenece a
  // la moderación del catálogo, que es otra pantalla y otro panel.
  servicios: ['SERVICIO_CREAR'],
  comisiones: ['PEDIDO_GESTIONAR'],
  sorteos: ['SORTEO_CREAR'],
  portafolioPropio: ['PORTAFOLIO_CREAR'],
  pedidosCliente: ['PEDIDO_CREAR', 'PEDIDO_GESTIONAR'],
  pedidoCrear: ['PEDIDO_CREAR'],
  // Bitácora de auditoría transversal (V15__modulo_auditoria.sql). Exportar
  // es un permiso aparte (AUDITORIA_EXPORTAR): no abre la pantalla por sí
  // solo, así que no va en esta lista — ver EXTRA_PANEL_PERMISSIONS más abajo.
  auditoria: ['AUDITORIA_VER'],
  // Igual criterio que auditoria: TRANSACCION_VER abre la pantalla y
  // muestra el reporte; REPORTE_*_EXPORTAR (V19__permisos_reportes.sql)
  // habilita solo el botón de exportar, no va aquí. Ambos reportes usan
  // TRANSACCION_VER (y no CONTRATO_VER) para la vista: CONTRATO_VER lo
  // tienen también CREADOR y CLIENTE (ver SHARED_NON_ADMIN_PERMISSIONS más abajo),
  // así que abriría esta pantalla admin a cualquiera con un contrato propio.
  reportesFinanzas: ['TRANSACCION_VER'],
  reportesContratos: ['TRANSACCION_VER'],
  // Supervisión de pagos en escrow. PAGO_AUDITAR estaba asignado a
  // AUDITOR_FINANCIERO desde el seed inicial sin ninguna pantalla que lo usara.
  pagosGarantia: ['PAGO_AUDITAR']
} as const satisfies Record<string, readonly string[]>;

export const PANEL_BASE_PATH: Record<PanelId, string> = {
  admin: '/admin',
  creador: '/creador',
  cliente: '/dashboard',
  cuenta: '/cuenta'
};

/**
 * URL de un ítem del menú. `basePath` gana al prefijo del panel: hay páginas
 * montadas fuera de su panel —"Mi Cuenta" figura en el menú de admin, creador y
 * cliente pero vive en /cuenta— y concatenar el prefijo del panel produciría
 * /admin/configuracion, que no existe.
 *
 * Existe como función porque el cálculo estaba repetido en los dos layouts,
 * en findNavLabel y en AuthService.homeRoute, y homeRoute se quedó sin
 * actualizar al introducir basePath.
 */
export function navItemPath(item: NavItem, basePathDelPanel: string): string {
  return `${item.basePath ?? basePathDelPanel}/${item.route}`;
}

/**
 * Commands de `[routerLink]` para un ítem del menú.
 *
 * `item.route` puede tener varios segmentos (p. ej. "explorar/creadores").
 * Pasar ese string tal cual como UN elemento del array de `routerLink` no lo
 * divide en segmentos: Angular lo trata como un único segmento literal y la
 * barra interna queda url-encodeada (`%2F`), así que el enlace apunta a una
 * ruta que no existe y el clic no navega a ningún sitio. Separar aquí por
 * "/" antes de devolver el array es lo que hace que cada segmento se navegue
 * de verdad.
 */
export function navItemLinkCommands(item: NavItem, basePathDelPanel: string): string[] {
  return [item.basePath ?? basePathDelPanel, ...item.route.split('/')];
}

/**
 * Nombre visible de la sección a la que apunta una URL, o null si no está en el
 * catálogo.
 *
 * Sirve para redactar avisos en el idioma del usuario ("No tienes acceso a
 * Gestión de Países") sin nombrar códigos de permiso: la etiqueta es texto que
 * el usuario ya vería en el menú, mientras que los códigos son nomenclatura
 * interna del sistema de autorización.
 */
export function findNavLabel(url: string): string | null {
  const limpia = url.split('?')[0].split('#')[0].replace(/\/+$/, '');

  const exacta = NAV_CATALOG.find(i => limpia === navItemPath(i, PANEL_BASE_PATH[i.panel]));
  if (exacta) return exacta.label;

  // Rutas montadas fuera de su panel (p. ej. /pedido/mis-pedidos): se compara
  // por el último segmento, que es el que identifica la pantalla.
  const segmento = limpia.split('/').filter(Boolean).pop();
  return NAV_CATALOG.find(i => i.route === segmento)?.label ?? null;
}

/**
 * Panel al que pertenece cada rol conocido.
 *
 * El panel es una propiedad del ROL —de la función que cumple la persona—, no
 * de la bolsa de permisos que tenga en un momento dado. Deducirlo solo de los
 * permisos provocaba que un rol cambiara de panel al editarle los permisos: a
 * un MODERADOR al que se le quitaban los permisos administrativos dejaban de
 * cumplirse las dos primeras condiciones y acababa en el panel de CLIENTE; si
 * le quedaba suelto un SORTEO_CREAR o un PEDIDO_GESTIONAR, aparecía como
 * CREADOR. El usuario "se saltaba" a otro rol sin que nadie se lo hubiera
 * cambiado.
 */
export const ROLE_PANEL: Record<string, PanelId> = {
  ADMIN: 'admin',
  ADMINISTRADOR: 'admin',
  MODERADOR: 'admin',
  SOPORTE: 'admin',
  AUDITOR_FINANCIERO: 'admin',
  CREADOR: 'creador',
  CLIENTE: 'cliente'
};

/** Precedencia cuando un usuario acumula varios roles. */
const PRECEDENCIA_PANEL: readonly PanelId[] = ['admin', 'creador', 'cliente'];

/**
 * Resuelve el panel del usuario.
 *
 * 0. Sin ningún permiso, no hay panel al que pertenecer: ni el de su rol ni
 *    uno inferido. Antes esta regla no existía y abría dos huecos:
 *      - Un rol conocido (MODERADOR, CREADOR...) vaciado de permisos se
 *        quedaba fijo en su panel por la regla 1, y `homeRoute` lo mandaba a
 *        la única página sin permiso de ese panel. Pero la PUERTA del panel
 *        (app.routes.ts, `data.permissions`) sí exige tener alguno, así que
 *        el usuario rebotaba en bucle contra /no-autorizado en cuanto
 *        intentaba entrar a su propio "inicio".
 *      - Un rol personalizado sin permisos no encajaba en ninguna regla y
 *        caía en el `return 'cliente'` final: un impostor con el menú
 *        completo de cliente pero sin poder usar ninguna de sus acciones (el
 *        backend las exige por rol o por permiso y responde 403). Se veía
 *        como "el usuario se salta al rol de cliente".
 *    'cuenta' es la única zona que no depende de ningún permiso —
 *    notificaciones y la configuración de la propia cuenta— y es honesta con
 *    lo que el usuario puede hacer hasta que un administrador le asigne
 *    permisos.
 * 1. Si tiene algún rol conocido, manda el rol: es estable y no cambia porque
 *    se editen permisos (mientras conserve al menos uno).
 * 2. Si todos sus roles son personalizados (creados desde "Roles y Permisos",
 *    que no pueden estar en el mapa de arriba), se infiere de los permisos.
 *    Para un rol nuevo no hay expectativa previa que romper.
 */
export function resolvePanel(roles: readonly string[], permisos: readonly string[]): PanelId {
  if (permisos.length === 0) return 'cuenta';

  const panelesPorRol = roles
    .map(r => ROLE_PANEL[r.replace(/^ROLE_/, '').toUpperCase()])
    .filter((p): p is PanelId => p !== undefined);

  if (panelesPorRol.length > 0) {
    return PRECEDENCIA_PANEL.find(p => panelesPorRol.includes(p)) ?? 'cliente';
  }

  if (panelGatePermissions('admin').some(p => permisos.includes(p))) return 'admin';
  if (panelGatePermissions('creador').some(p => permisos.includes(p))) return 'creador';
  return 'cliente';
}

/**
 * El orden importa: la primera entrada visible de un panel es la página de
 * aterrizaje tras iniciar sesión (`AuthService.homeRoute`). Está dispuesto de
 * forma que cada rol del seed conserve exactamente la que tenía.
 */
export const NAV_CATALOG: readonly NavItem[] = [
  // ─── Panel de administración ───
  { label: 'Gestión de Usuarios', icon: 'group', route: 'users', panel: 'admin', permissions: PAGE_PERMISSIONS.users, crossPanel: true },
  // Antes de "Panel de Moderación" a propósito: es la única pantalla que ve
  // AUDITOR_FINANCIERO antes de este punto en la lista, así que de otro modo
  // su página de aterrizaje sería "Gestión de Países" (más abajo).
  { label: 'Pagos y Garantías', icon: 'account_balance', route: 'pagos-garantia', panel: 'admin', permissions: PAGE_PERMISSIONS.pagosGarantia, crossPanel: true },
  { label: 'Panel de Moderación', icon: 'dashboard', route: 'mod-overview', panel: 'admin', permissions: PAGE_PERMISSIONS.panelModeracion, crossPanel: true },
  { label: 'Verificaciones', icon: 'verified', route: 'verificaciones', panel: 'admin', permissions: PAGE_PERMISSIONS.verificaciones, crossPanel: true },
  { label: 'Portafolios', icon: 'palette', route: 'mod-portafolios', panel: 'admin', permissions: PAGE_PERMISSIONS.portafoliosModeracion, crossPanel: true },
  { label: 'Comentarios', icon: 'chat', route: 'mod-comentarios', panel: 'admin', permissions: PAGE_PERMISSIONS.comentariosModeracion, crossPanel: true },
  // Antes este ítem no declaraba permiso pero su ruta ya exigía
  // CATEGORIA_GESTIONAR: se veía en el menú y llevaba a /no-autorizado.
  { label: 'Categorías', icon: 'category', route: 'mod-categorias', panel: 'admin', permissions: PAGE_PERMISSIONS.categorias, crossPanel: true },
  { label: 'Gestión de Países', icon: 'public', route: 'paises', panel: 'admin', permissions: PAGE_PERMISSIONS.paises, crossPanel: true },
  { label: 'Roles y Permisos', icon: 'lock_person', route: 'roles-permissions', panel: 'admin', permissions: PAGE_PERMISSIONS.rolesPermisos, crossPanel: true },
  { label: 'Infracciones', icon: 'gavel', route: 'infracciones', panel: 'admin', permissions: PAGE_PERMISSIONS.infracciones, crossPanel: true },
  { label: 'Flujos de Trabajo (Plantillas)', icon: 'account_tree', route: 'flujos', panel: 'admin', permissions: PAGE_PERMISSIONS.flujosModeracion, crossPanel: true },
  // 'receipt_long' ya tiene rama SVG en dashboard-layout.component.html
  // (compartida con 'account_balance'): no hace falta tocar el layout.
  { label: 'Auditoría', icon: 'receipt_long', route: 'auditoria', panel: 'admin', permissions: PAGE_PERMISSIONS.auditoria, crossPanel: true },
  { label: 'Reporte financiero', icon: 'account_balance', route: 'reportes-finanzas', panel: 'admin', permissions: PAGE_PERMISSIONS.reportesFinanzas, crossPanel: true },
  { label: 'Reporte de contratos', icon: 'description', route: 'reportes-contratos', panel: 'admin', permissions: PAGE_PERMISSIONS.reportesContratos, crossPanel: true },
  { label: 'Notificaciones', icon: 'notifications', route: 'notificaciones', panel: 'admin' },
  // Configuración de la cuenta propia: contraseña, 2FA, preferencias. Es la
  // misma página que ven creador y cliente — ver NavItem.basePath.
  { label: 'Mi Cuenta', icon: 'account_circle', route: 'configuracion', panel: 'admin', basePath: '/cuenta' },

  // ─── Panel de creador ───
  { label: 'Overview', icon: 'dashboard', route: 'overview', panel: 'creador' },
  { label: 'Mis Servicios', icon: 'storefront', route: 'servicios', panel: 'creador', permissions: PAGE_PERMISSIONS.servicios, crossPanel: true },
  { label: 'Comisiones', icon: 'shopping_bag', route: 'comisiones', panel: 'creador', permissions: PAGE_PERMISSIONS.comisiones, crossPanel: true },
  { label: 'Briefings', icon: 'assignment', route: 'briefings', panel: 'creador' },
  { label: 'Notificaciones', icon: 'notifications', route: 'notificaciones', panel: 'creador' },
  { label: 'Reseñas', icon: 'rate_review', route: 'resenas', panel: 'creador' },
  { label: 'Seguidores', icon: 'group', route: 'seguidores', panel: 'creador' },
  { label: 'Sorteos', icon: 'celebration', route: 'sorteos', panel: 'creador', permissions: PAGE_PERMISSIONS.sorteos, crossPanel: true },
  // Desde V25 (flujos_por_creador) cada FlujoTrabajo es propiedad de un
  // creador (filtrado por su propio id_usuario en el backend, que exige
  // hasRole('CREADOR') sin mirar este permiso). Antes solo existía la entrada
  // de panel:'admin' de más arriba — invisible para un CREADOR aunque se le
  // asignara FLUJO_GESTIONAR, porque el sidebar filtra por panel activo antes
  // de mirar permisos.
  { label: 'Mis Flujos de Trabajo', icon: 'account_tree', route: 'flujos', panel: 'creador', permissions: PAGE_PERMISSIONS.flujosPropios, crossPanel: true },
  { label: 'Portafolio', icon: 'folder_special', route: 'portafolio', panel: 'creador', permissions: PAGE_PERMISSIONS.portafolioPropio, crossPanel: true },
  // "Mi Perfil" (perfil de negocio: biografía, red social, verificación) y
  // "Mi Cuenta" (roles/permisos vigentes, contraseña, 2FA) son cosas
  // distintas: la primera es de dominio de creador, la segunda es la misma
  // página que ven admin y cliente. Conviven en el menú, ver NavItem.basePath.
  { label: 'Mi Perfil', icon: 'person', route: 'perfil', panel: 'creador' },
  { label: 'Mi Cuenta', icon: 'account_circle', route: 'configuracion', panel: 'creador', basePath: '/cuenta' },

  // ─── Panel de cliente ───
  { label: 'Overview', icon: 'dashboard', route: 'overview', panel: 'cliente' },
  { label: 'Explorar', icon: 'storefront', route: 'explorar', panel: 'cliente' },
  { label: 'Creadores', icon: 'group', route: 'explorar/creadores', panel: 'cliente' },
  { label: 'Mis Pedidos', icon: 'shopping_bag', route: 'mis-pedidos', panel: 'cliente' },
  { label: 'Notificaciones', icon: 'notifications', route: 'notificaciones', panel: 'cliente' },
  { label: 'Sorteos', icon: 'celebration', route: 'sorteos', panel: 'cliente' },
  { label: 'Mi Perfil', icon: 'person', route: 'perfil', panel: 'cliente' },
  { label: 'Mi Cuenta', icon: 'account_circle', route: 'configuracion', panel: 'cliente', basePath: '/cuenta' },

  // ─── Panel de cuenta (sin permisos asignados) ───
  // El orden fija el aterrizaje: notificaciones es la primera página, igual
  // que en los otros tres paneles.
  { label: 'Notificaciones', icon: 'notifications', route: 'notificaciones', panel: 'cuenta' },
  { label: 'Mi Cuenta', icon: 'account_circle', route: 'configuracion', panel: 'cuenta' }
];

/**
 * Permisos que deben abrir la puerta de un panel PERO no abren ninguna página
 * del catálogo, así que la derivación desde NAV_CATALOG no puede encontrarlos.
 *
 * Solo tres formas legítimas de estar aquí:
 *  1. Habilitan un botón dentro de una pantalla que ya se abre con otro permiso
 *     (los cuatro *_EXPORTAR).
 *  2. Pueblan un desplegable de otra pantalla (ROL_VER, PERMISO_VER).
 *  3. Habilitan una acción de servidor sin pantalla propia.
 *
 * Un permiso que SÍ abre una pantalla NO va aquí: se declara en el ítem de
 * NAV_CATALOG y la derivación lo recoge sola. Ese es justo el fallo que esto
 * hace imposible: antes ADMIN_PANEL_PERMISSIONS era una copia a mano de los
 * permisos del catálogo, y las dos listas podían divergir sin que nada lo
 * avisara (el bug de FLUJO_MODERAR: el catálogo lo exigía, la lista de la
 * puerta tardó dos migraciones en incluirlo).
 */
const EXTRA_PANEL_PERMISSIONS: Partial<Record<PanelId, readonly string[]>> = {
  admin: [
    'AUDITORIA_EXPORTAR', 'REPORTE_FINANCIERO_EXPORTAR', 'REPORTE_CONTRATO_EXPORTAR', 'USUARIO_EXPORTAR',
    'ROL_VER', 'PERMISO_VER', 'SESION_REVOCAR',
    'SERVICIO_MODERAR', 'MENSAJE_MODERAR', 'NOTIFICACION_ENVIAR', 'TICKET_RESOLVER', 'FONDOS_LIBERAR'
  ]
};

/**
 * Permisos que CREADOR o CLIENTE también poseen en el seed. Ninguna puerta de
 * panel (admin ni creador) debe contenerlos: si uno colara ahí, un cliente
 * corriente con ese único permiso quedaría clasificado como admin o creador.
 */
export const SHARED_NON_ADMIN_PERMISSIONS: readonly string[] = [
  'TICKET_REVISAR', 'CONTRATO_VER', 'CONTRATO_FIRMAR', 'SALA_VER', 'MENSAJE_ENVIAR'
];

/** Paneles con puerta de permisos. 'cliente' y 'cuenta' no la tienen. */
const PANELES_CON_PUERTA: readonly PanelId[] = ['admin', 'creador'];

function derivarPuerta(panel: PanelId): readonly string[] {
  const dePaginas = NAV_CATALOG.filter(i => i.panel === panel).flatMap(i => i.permissions ?? []);
  return Object.freeze([...new Set([...dePaginas, ...(EXTRA_PANEL_PERMISSIONS[panel] ?? [])])]);
}

/**
 * Permisos que exige la puerta de cada panel (data.permissions de la ruta
 * padre en app.routes.ts, y la comprobación cross-panel de
 * AuthService.visibleNavItems).
 *
 * Antes eran ADMIN_PANEL_PERMISSIONS/CREADOR_PANEL_PERMISSIONS: listas
 * escritas a mano que había que acordarse de actualizar cada vez que se
 * tocaba un permiso en NAV_CATALOG.
 */
const PANEL_GATE_PERMISSIONS: Partial<Record<PanelId, readonly string[]>> =
  Object.fromEntries(PANELES_CON_PUERTA.map(p => [p, derivarPuerta(p)]));

/** Permisos que exige la puerta de `panel`. Vacío si el panel no tiene puerta. */
export function panelGatePermissions(panel: PanelId): readonly string[] {
  return PANEL_GATE_PERMISSIONS[panel] ?? [];
}

/**
 * Ítems de menú visibles para un panel activo y un conjunto de permisos.
 *
 * Además del permiso propio del ítem, un ítem `crossPanel` debe cumplir
 * también la puerta del panel al que pertenece: sin esto, un permiso que abre
 * un ítem pero no la puerta de su panel (p. ej. FLUJO_GESTIONAR abre "Mis
 * Flujos de Trabajo" del panel creador sin dar ninguno de los permisos que
 * exige `/creador`) mostraba en el menú un enlace que la puerta del panel
 * rechazaba antes de llegar a la ruta hija.
 */
export function computeVisibleNavItems(panel: PanelId, permisos: readonly string[]): NavItem[] {
  const tienePermiso = (item: NavItem) =>
    !item.permissions?.length || item.permissions.some(p => permisos.includes(p));

  const propios = NAV_CATALOG.filter(i => i.panel === panel && tienePermiso(i));

  const cruzados = NAV_CATALOG
    .filter(i => {
      if (i.panel === panel || i.crossPanel !== true) return false;
      if (!i.permissions?.length || !i.permissions.some(p => permisos.includes(p))) return false;
      const puerta = panelGatePermissions(i.panel);
      return puerta.length === 0 || puerta.some(p => permisos.includes(p));
    })
    .map(i => ({ ...i, basePath: i.basePath ?? PANEL_BASE_PATH[i.panel] }));

  return [...propios, ...cruzados];
}
