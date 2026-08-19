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
  flujos: ['FLUJO_GESTIONAR'],
  // "Mis Servicios" es el CRUD propio del creador; SERVICIO_MODERAR pertenece a
  // la moderación del catálogo, que es otra pantalla y otro panel.
  servicios: ['SERVICIO_CREAR'],
  comisiones: ['PEDIDO_GESTIONAR'],
  sorteos: ['SORTEO_CREAR'],
  portafolioPropio: ['PORTAFOLIO_CREAR'],
  pedidosCliente: ['PEDIDO_CREAR', 'PEDIDO_GESTIONAR'],
  pedidoCrear: ['PEDIDO_CREAR']
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
 * Permisos que, en el seed, solo poseen los roles del panel de administración
 * (ADMIN, MODERADOR, SOPORTE, AUDITOR_FINANCIERO). Se han excluido a
 * conciencia los que CREADOR o CLIENTE también tienen —TICKET_REVISAR,
 * CONTRATO_VER, CONTRATO_FIRMAR, SALA_VER, MENSAJE_ENVIAR—, porque si no un
 * cliente acabaría clasificado como administrador.
 */
export const ADMIN_PANEL_PERMISSIONS: readonly string[] = [
  'USUARIO_VER', 'USUARIO_CREAR', 'USUARIO_EDITAR', 'USUARIO_ELIMINAR', 'USUARIO_SUSPENDER',
  'ROL_VER', 'ROL_GESTIONAR', 'PERMISO_VER', 'ROL_ASIGNAR_PERMISO', 'SESION_REVOCAR',
  'PAIS_VER', 'PAIS_CREAR', 'PAIS_EDITAR', 'PAIS_ELIMINAR',
  'PORTAFOLIO_MODERAR', 'CERTIFICADO_REVISAR', 'CATEGORIA_GESTIONAR', 'SERVICIO_MODERAR',
  'MENSAJE_MODERAR', 'COMENTARIO_MODERAR', 'NOTIFICACION_ENVIAR', 'TICKET_RESOLVER',
  'PAGO_AUDITAR', 'FONDOS_LIBERAR', 'TRANSACCION_VER',
  'PANEL_MODERACION_VER', 'INFRACCION_GESTIONAR', 'FLUJO_GESTIONAR'
];

/** Permisos exclusivos del rol CREADOR frente a CLIENTE. */
export const CREADOR_PANEL_PERMISSIONS: readonly string[] = [
  'SERVICIO_CREAR', 'PORTAFOLIO_CREAR', 'PEDIDO_GESTIONAR', 'SORTEO_CREAR'
];

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

  if (ADMIN_PANEL_PERMISSIONS.some(p => permisos.includes(p))) return 'admin';
  if (CREADOR_PANEL_PERMISSIONS.some(p => permisos.includes(p))) return 'creador';
  return 'cliente';
}

/**
 * El orden importa: la primera entrada visible de un panel es la página de
 * aterrizaje tras iniciar sesión (`AuthService.homeRoute`). Está dispuesto de
 * forma que cada rol del seed conserve exactamente la que tenía.
 */
export const NAV_CATALOG: readonly NavItem[] = [
  // ─── Panel de administración ───
  { label: 'Gestión de Usuarios', icon: 'group', route: 'users', panel: 'admin', permissions: PAGE_PERMISSIONS.users },
  { label: 'Panel de Moderación', icon: 'dashboard', route: 'mod-overview', panel: 'admin', permissions: PAGE_PERMISSIONS.panelModeracion },
  { label: 'Verificaciones', icon: 'verified', route: 'verificaciones', panel: 'admin', permissions: PAGE_PERMISSIONS.verificaciones },
  { label: 'Portafolios', icon: 'palette', route: 'mod-portafolios', panel: 'admin', permissions: PAGE_PERMISSIONS.portafoliosModeracion },
  { label: 'Comentarios', icon: 'chat', route: 'mod-comentarios', panel: 'admin', permissions: PAGE_PERMISSIONS.comentariosModeracion },
  // Antes este ítem no declaraba permiso pero su ruta ya exigía
  // CATEGORIA_GESTIONAR: se veía en el menú y llevaba a /no-autorizado.
  { label: 'Categorías', icon: 'category', route: 'mod-categorias', panel: 'admin', permissions: PAGE_PERMISSIONS.categorias },
  { label: 'Gestión de Países', icon: 'public', route: 'paises', panel: 'admin', permissions: PAGE_PERMISSIONS.paises },
  { label: 'Roles y Permisos', icon: 'lock_person', route: 'roles-permissions', panel: 'admin', permissions: PAGE_PERMISSIONS.rolesPermisos },
  { label: 'Infracciones', icon: 'gavel', route: 'infracciones', panel: 'admin', permissions: PAGE_PERMISSIONS.infracciones },
  { label: 'Flujos de Trabajo', icon: 'account_tree', route: 'flujos', panel: 'admin', permissions: PAGE_PERMISSIONS.flujos },
  { label: 'Notificaciones', icon: 'notifications', route: 'notificaciones', panel: 'admin' },
  // Configuración de la cuenta propia: contraseña, 2FA, preferencias. Es la
  // misma página que ven creador y cliente — ver NavItem.basePath.
  { label: 'Mi Cuenta', icon: 'account_circle', route: 'configuracion', panel: 'admin', basePath: '/cuenta' },

  // ─── Panel de creador ───
  { label: 'Overview', icon: 'dashboard', route: 'overview', panel: 'creador' },
  { label: 'Mis Servicios', icon: 'storefront', route: 'servicios', panel: 'creador', permissions: PAGE_PERMISSIONS.servicios },
  { label: 'Comisiones', icon: 'shopping_bag', route: 'comisiones', panel: 'creador', permissions: PAGE_PERMISSIONS.comisiones },
  { label: 'Briefings', icon: 'assignment', route: 'briefings', panel: 'creador' },
  { label: 'Notificaciones', icon: 'notifications', route: 'notificaciones', panel: 'creador' },
  { label: 'Reseñas', icon: 'rate_review', route: 'resenas', panel: 'creador' },
  { label: 'Sorteos', icon: 'celebration', route: 'sorteos', panel: 'creador', permissions: PAGE_PERMISSIONS.sorteos },
  { label: 'Portafolio', icon: 'folder_special', route: 'portafolio', panel: 'creador', permissions: PAGE_PERMISSIONS.portafolioPropio },
  { label: 'Certificados IA', icon: 'verified', route: 'certificados', panel: 'creador' },
  // "Mi Perfil" (perfil de negocio: biografía, red social, verificación) y
  // "Mi Cuenta" (roles/permisos vigentes, contraseña, 2FA) son cosas
  // distintas: la primera es de dominio de creador, la segunda es la misma
  // página que ven admin y cliente. Conviven en el menú, ver NavItem.basePath.
  { label: 'Mi Perfil', icon: 'person', route: 'perfil', panel: 'creador' },
  { label: 'Mi Cuenta', icon: 'account_circle', route: 'configuracion', panel: 'creador', basePath: '/cuenta' },

  // ─── Panel de cliente ───
  { label: 'Overview', icon: 'dashboard', route: 'overview', panel: 'cliente' },
  { label: 'Explorar', icon: 'storefront', route: 'explorar', panel: 'cliente' },
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
