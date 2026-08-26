/**
 * Formatos soportados por el motor común de exportación de reportes
 * (Backend: service/shared/reporte, enum FormatoReporte). El valor viaja
 * literal como query param `?formato=` — Spring lo convierte al enum por
 * nombre de constante, así que estos strings deben coincidir exactamente.
 */
export type FormatoReporte = 'CSV' | 'XLSX' | 'PDF';

export interface OpcionFormatoReporte {
  valor: FormatoReporte;
  etiqueta: string;
  /** Clases Tailwind para el badge de color del ícono en el desplegable (fondo + texto). */
  acento: string;
}

export const FORMATOS_REPORTE: readonly OpcionFormatoReporte[] = [
  { valor: 'CSV', etiqueta: 'CSV', acento: 'bg-sky-50 text-sky-600' },
  { valor: 'XLSX', etiqueta: 'Excel', acento: 'bg-emerald-50 text-emerald-600' },
  { valor: 'PDF', etiqueta: 'PDF', acento: 'bg-rose-50 text-rose-600' }
] as const satisfies readonly OpcionFormatoReporte[];
