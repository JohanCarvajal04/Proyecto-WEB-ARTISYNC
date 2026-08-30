import { PanelId } from '../../core/config/nav.config';

/**
 * Textos del cascarón que dependen del panel activo. Los COLORES no están
 * aquí: viven en src/styles.css como variables CSS sobre [data-panel], para
 * que cambiar un color no obligue a tocar TypeScript (ver AppShellComponent).
 */
export const SHELL_COPY: Record<PanelId, { buscador: string; rolPorDefecto: string }> = {
  admin:   { buscador: 'Buscar...',                       rolPorDefecto: 'Administrador' },
  creador: { buscador: 'Buscar comisiones, servicios...', rolPorDefecto: 'Creador' },
  cliente: { buscador: 'Buscar pedidos, servicios...',    rolPorDefecto: 'Cliente' },
  cuenta:  { buscador: 'Buscar...',                       rolPorDefecto: 'Usuario' }
};
