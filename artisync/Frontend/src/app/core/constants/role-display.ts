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

const CLASES_NEUTRAS = 'bg-surface-container-highest text-on-surface font-medium';

const ROLE_DISPLAY: Record<string, RoleDisplay> = {
  ADMINISTRADOR: { label: 'Administrador', classes: 'bg-error-container text-on-error-container font-medium' },
  ADMIN: { label: 'Administrador', classes: 'bg-error-container text-on-error-container font-medium' },
  MODERADOR: { label: 'Moderador', classes: 'bg-primary-container text-on-primary-container font-medium' },
  SOPORTE: { label: 'Soporte Técnico', classes: 'bg-tertiary-container text-on-tertiary-container font-medium' },
  AUDITOR_FINANCIERO: { label: 'Auditor Financiero', classes: CLASES_NEUTRAS },
  CREADOR: { label: 'Creador', classes: 'bg-secondary-container text-on-secondary-container font-medium' },
  CLIENTE: { label: 'Cliente', classes: 'bg-surface-container-high text-on-surface-variant' }
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
