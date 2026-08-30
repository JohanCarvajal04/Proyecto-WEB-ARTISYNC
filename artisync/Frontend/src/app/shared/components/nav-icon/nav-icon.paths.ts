/**
 * Mapa único de iconos de navegación: nombre lógico (el `icon` que declara cada
 * `NavItem` en nav.config.ts) -> atributo `d` de un `<path>` SVG.
 *
 * Antes esta cadena de `@if/@else if` vivía duplicada dentro de
 * dashboard-layout y client-dashboard-layout, y cada copia solo cubría los
 * iconos que su propio panel usaba. Como consecuencia, seis entradas del menú
 * (`public`, `gavel`, `account_tree`, `description`, `assignment`,
 * `notifications`) no tenían rama en NINGÚN layout y se dibujaban como el
 * icono de respaldo (una hamburguesa), sin que nadie lo notara porque cada
 * panel solo veía sus propios iconos "funcionar".
 *
 * Todos los paths comparten el mismo trazo: viewBox 0 0 24 24, fill="none",
 * stroke="currentColor", stroke-width="2", stroke-linecap/linejoin="round" —
 * exactamente lo que ya usaban los 22 SVG de los dos layouts. Ver
 * NavIconComponent para el envoltorio que los renderiza.
 */

const DASHBOARD = 'M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z';
const GRUPO = 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z';
const CANDADO = 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z';
const AJUSTES = 'M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z';
/** Escudo con check (Heroicons `shield-check`). Gana sobre el escudo simplificado
 *  que dibujaba a mano el panel cliente para el mismo nombre: misma semántica
 *  (verificación), y es el trazo oficial de Heroicons. */
const VERIFICADO = 'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z';
const PALETA = 'M4.098 19.902a3.75 3.75 0 005.304 0l6.401-6.402M6.75 21A3.75 3.75 0 013 17.25V4.125C3 3.504 3.504 3 4.125 3h5.25c.621 0 1.125.504 1.125 1.125v4.072M6.75 21a3.75 3.75 0 003.75-3.75V8.197M6.75 21h13.125c.621 0 1.125-.504 1.125-1.125v-5.25c0-.621-.504-1.125-1.125-1.125h-4.072M10.5 8.197l2.88-2.88c.438-.439 1.15-.439 1.59 0l3.712 3.713c.44.44.44 1.152 0 1.59l-2.879 2.88M6.75 17.25h.008v.008H6.75v-.008z';
const CHAT = 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z';
const CATEGORIA = 'M7.5 7.5h-.75A2.25 2.25 0 004.5 9.75v7.5a2.25 2.25 0 002.25 2.25h7.5a2.25 2.25 0 002.25-2.25v-7.5a2.25 2.25 0 00-2.25-2.25h-.75m-6 3.75l3 3m0 0l3-3m-3 3V1.5m6 9h.75a2.25 2.25 0 012.25 2.25v7.5a2.25 2.25 0 01-2.25 2.25h-7.5A2.25 2.25 0 019 21v-.75';
/** Bocadillo con líneas (reseña). Gana sobre el path que el panel admin tenía
 *  bajo el mismo nombre (en realidad un "clipboard-document-check"): ningún
 *  ítem admin usaba `rate_review`, así que era rama muerta — se recicla como
 *  ASIGNACION más abajo, cubriendo uno de los seis huecos sin renombrar nada
 *  en NAV_CATALOG. */
const RESENA = 'M8 12h8m-8 4h5m3 5l-3-3H6a2 2 0 01-2-2V6a2 2 0 012-2h12a2 2 0 012 2v10a2 2 0 01-2 2h-2l-2 3z';
const SOPORTE = 'M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z';
const RECIBO = 'M2.25 18.75a60.07 60.07 0 0115.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 013 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 00-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 01-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 003 15h-.75M15 10.5a3 3 0 11-6 0 3 3 0 016 0zm3 0h.008v.008H18V10.5zm-12 0h.008v.008H6V10.5z';
const CUENTA = 'M17.982 18.725A7.488 7.488 0 0012 15.75a7.488 7.488 0 00-5.982 2.975m11.963 0a9 9 0 10-11.963 0m11.963 0A8.966 8.966 0 0112 21a8.966 8.966 0 01-5.982-2.275M15 9.75a3 3 0 11-6 0 3 3 0 016 0z';
const BOLSA = 'M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z';
const PERSONA = 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z';
const TIENDA = 'M3 9l1.5-5h15L21 9M3 9h18M3 9v10a1 1 0 001 1h16a1 1 0 001-1V9M9 13h6';
const CELEBRACION = 'M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.3 6.3L22 12l-6.7 2.7L13 21l-2.3-6.3L4 12l6.7-2.7L13 3z';
const CARPETA = 'M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V7z';

/** Globo (Heroicons v1 `globe`) — Gestión de Países. */
const GLOBO = 'M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c2.485 0 4.5-4.03 4.5-9S14.485 3 12 3m0 18c-2.485 0-4.5-4.03-4.5-9S9.515 3 12 3m-9 9a9 9 0 019-9';
/** Balanza (Heroicons v2 `scale`) — Infracciones. Heroicons no tiene mazo; la
 *  balanza es el símbolo de sanción/arbitraje más cercano del set. */
const BALANZA = 'M12 3v17.25m0 0c-1.472 0-2.882.265-4.185.75M12 20.25c1.472 0 2.882.265 4.185.75M18.75 4.97A48.416 48.416 0 0012 4.5c-2.291 0-4.545.16-6.75.47m13.5 0c1.01.143 2.01.317 3 .52m-3-.52l2.62 10.726c.122.499-.106 1.028-.589 1.202a5.988 5.988 0 01-2.031.352 5.988 5.988 0 01-2.031-.352c-.483-.174-.711-.703-.59-1.202L18.75 4.971zm-16.5.52c.99-.203 1.99-.377 3-.52m0 0l2.62 10.726c.122.499-.106 1.028-.589 1.202a5.989 5.989 0 01-2.031.352 5.989 5.989 0 01-2.031-.352c-.483-.174-.711-.703-.59-1.202L5.25 4.971z';
/** Bloques encadenados (Heroicons v1 `template`) — Flujos de Trabajo. */
const FLUJO = 'M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z';
/** Documento con texto (Heroicons v1 `document-text`) — Reporte de contratos. */
const DOCUMENTO = 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z';
/** Portapapeles con check — Briefings. Es el path que en dashboard-layout
 *  estaba (sin uso real) bajo el nombre `rate_review`. */
const ASIGNACION = 'M11.35 3.836c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m8.9-4.414c.376.023.75.05 1.124.08 1.131.094 1.976 1.057 1.976 2.192V16.5A2.25 2.25 0 0118 18.75h-2.25m-7.5-10.5H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V18.75m-7.5-10.5h6.375c.621 0 1.125.504 1.125 1.125v9.375m-8.25-3l1.5 1.5 3-3.75';
/** Campana — Notificaciones. Mismo path que usa la campana de la barra
 *  superior del shell: se consume de aquí para no mantener dos copias. */
const CAMPANA = 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9';

/** Cromo del propio shell (no aparece en NAV_CATALOG): buscador, hamburguesa,
 *  cerrar (X), flechas de colapsar/expandir y logout. Se centralizan aquí para
 *  que no quede un solo `<svg>` inline en app-shell.component.html. */
const BUSCAR = 'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z';
const MENU = 'M4 6h16M4 12h16M4 18h16';
const CERRAR_X = 'M6 18L18 6M6 6l12 12';
const FLECHA_IZQ = 'M15.75 19.5L8.25 12l7.5-7.5';
const FLECHA_DER = 'M8.25 4.5l7.5 7.5-7.5 7.5';
const LOGOUT = 'M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1';

export const NAV_ICON_PATHS: Readonly<Record<string, string>> = {
  dashboard: DASHBOARD,
  group: GRUPO, no_accounts: GRUPO,
  lock_person: CANDADO, shield_lock: CANDADO, lock: CANDADO,
  settings: AJUSTES, tune: AJUSTES,
  verified: VERIFICADO,
  palette: PALETA,
  chat: CHAT,
  category: CATEGORIA,
  rate_review: RESENA,
  support_agent: SOPORTE,
  account_balance: RECIBO, receipt_long: RECIBO,
  account_circle: CUENTA,
  shopping_bag: BOLSA,
  person: PERSONA,
  storefront: TIENDA,
  celebration: CELEBRACION,
  folder_special: CARPETA,

  // Antes caían en el respaldo: ninguno tenía rama en los layouts viejos.
  public: GLOBO,
  gavel: BALANZA,
  account_tree: FLUJO,
  description: DOCUMENTO,
  assignment: ASIGNACION,
  notifications: CAMPANA,

  // Cromo del shell, sin entrada en NAV_CATALOG.
  search: BUSCAR,
  menu: MENU,
  close: CERRAR_X,
  chevron_left: FLECHA_IZQ,
  chevron_right: FLECHA_DER,
  logout: LOGOUT
};

/** Icono de respaldo: la rama `@else` que hasta ahora tenían los dos layouts.
 *  Se exporta para que el spec pueda comprobar que ningún icono de
 *  NAV_CATALOG cae aquí. */
export const NAV_ICON_FALLBACK = MENU;
