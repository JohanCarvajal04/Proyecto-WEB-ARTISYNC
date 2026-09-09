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

/** Topes de filas por archivo según FormatoReporte.java en backend */
export const TOPES_FORMATO: Record<FormatoReporte, number> = {
  PDF: 5_000,
  CSV: 50_000,
  XLSX: 100_000,
};

export interface OpcionesExportacion {
  formato: FormatoReporte;
  page?: number;
  size?: number;
  todasLasPartes?: boolean;
}

export type TipoGraficaReporte = 'ROL' | 'PAIS' | 'AMBAS' | 'NINGUNA';

export interface OpcionGraficaReporte {
  valor: TipoGraficaReporte;
  etiqueta: string;
  descripcion: string;
}

export const OPCIONES_GRAFICA_REPORTE: readonly OpcionGraficaReporte[] = [
  { valor: 'AMBAS', etiqueta: 'Ambas gráficas', descripcion: 'Por Rol y País (Informe analítico completo)' },
  { valor: 'ROL', etiqueta: 'Por Rol', descripcion: 'Distribución según roles de usuario' },
  { valor: 'PAIS', etiqueta: 'Por País', descripcion: 'Concentración geográfica por país' },
  { valor: 'NINGUNA', etiqueta: 'Solo lista', descripcion: 'Exportación tradicional sin gráficos' }
] as const;
