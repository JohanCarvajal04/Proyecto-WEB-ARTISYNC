/**
 * Presentación de roles en la UI (etiqueta legible + clases del badge).
 *
 * Los roles son datos: el administrador puede crear los que quiera desde
 * "Roles y Permisos", así que este mapa NO es la lista de roles válidos —
 * solo cubre los seis del seed para conservar su color y su nombre en
 * castellano. Cualquier rol que no esté aquí degrada a un estilo neutro
 */
export interface RoleDisplay {
  label: string;
  classes: string;
}

const CLASES_NEUTRAS = 'bg-slate-100 text-slate-700 font-medium';

const ROLE_DISPLAY: Record<string, RoleDisplay> = {
  ADMINISTRADOR: { label: 'Administrador', classes: 'bg-fuchsia-100 text-fuchsia-700 font-bold' },
  ADMIN: { label: 'Administrador', classes: 'bg-fuchsia-100 text-fuchsia-700 font-bold' },
  MODERADOR: { label: 'Moderador', classes: 'bg-purple-100 text-purple-700 font-bold' },
  SOPORTE: { label: 'Soporte Técnico', classes: 'bg-cyan-100 text-cyan-700 font-bold' },
  AUDITOR_FINANCIERO: { label: 'Auditor Financiero', classes: 'bg-emerald-100 text-emerald-700 font-bold' },
  CREADOR: { label: 'Creador', classes: 'bg-amber-100 text-amber-700 font-bold' },
  CLIENTE: { label: 'Cliente', classes: 'bg-indigo-100 text-indigo-700 font-bold' }
};

/** Quita el prefijo `ROLE_` y normaliza a mayúsculas. */
export function normalizeRoleName(role: string): string {
  return role.replace(/^ROLE_/, '').toUpperCase();
}

/** `AUDITOR_FINANCIERO` → `Auditor financiero`. Para roles que no están en el mapa. */
export function humanizeRoleName(role: string): string {
  const limpio = normalizeRoleName(role).replace(/_/g, ' ').toLowerCase();
  return limpio.charAt(0).toUpperCase() + limpio.slice(1);
}

/** Etiqueta + clases del badge; degrada al nombre formateado si el rol es nuevo. */
export function getRoleDisplay(role: string): RoleDisplay {
  return ROLE_DISPLAY[normalizeRoleName(role)] ?? { label: humanizeRoleName(role), classes: CLASES_NEUTRAS };
}

/** Solo la etiqueta legible del rol. */
export function getRoleLabel(role: string): string {
  return getRoleDisplay(role).label;
}
