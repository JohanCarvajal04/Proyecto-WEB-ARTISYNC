export interface NavItem {
  label: string;
  icon: string;
  route: string;
  permission?: string;
}

export interface RoleNavConfig {
  basePath: string;
  items: NavItem[];
}

export const NAV_CONFIG: Record<string, RoleNavConfig> = {
  ADMINISTRADOR: {
    basePath: '/admin',
    items: [
      { label: 'Overview', icon: 'dashboard', route: 'users' },
      { label: 'Gestión de Usuarios', icon: 'group', route: 'users', permission: 'USUARIO_VER' },
      { label: 'Roles y Permisos', icon: 'lock_person', route: 'roles-permissions', permission: 'ROL_GESTIONAR' },
      { label: 'Configuración', icon: 'settings', route: 'settings' }
    ]
  },
  ADMIN: {
    basePath: '/admin',
    items: [
      { label: 'Overview', icon: 'dashboard', route: 'users' },
      { label: 'Gestión de Usuarios', icon: 'group', route: 'users', permission: 'USUARIO_VER' },
      { label: 'Roles y Permisos', icon: 'lock_person', route: 'roles-permissions', permission: 'ROL_GESTIONAR' },
      { label: 'Configuración', icon: 'settings', route: 'settings' }
    ]
  },
  MODERADOR: {
    basePath: '/admin',
    items: [
      { label: 'Moderación Portafolios', icon: 'rate_review', route: 'users', permission: 'PORTAFOLIO_MODERAR' },
      { label: 'Revisión Certificados IA', icon: 'verified', route: 'users', permission: 'CERTIFICADO_REVISAR' }
    ]
  },
  SOPORTE: {
    basePath: '/admin',
    items: [
      { label: 'Consulta Usuarios', icon: 'group', route: 'users', permission: 'USUARIO_VER' },
      { label: 'Tickets de Soporte', icon: 'support_agent', route: 'users', permission: 'TICKET_REVISAR' }
    ]
  },
  AUDITOR_FINANCIERO: {
    basePath: '/admin',
    items: [
      { label: 'Auditoría de Pagos', icon: 'account_balance', route: 'users', permission: 'PAGO_AUDITAR' },
      { label: 'Historial Transacciones', icon: 'receipt_long', route: 'users', permission: 'TRANSACCION_VER' }
    ]
  }
};
