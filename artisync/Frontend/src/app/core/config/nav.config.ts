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
      { label: 'Gestión de Países', icon: 'public', route: 'paises', permission: 'PAIS_VER' },
      { label: 'Roles y Permisos', icon: 'lock_person', route: 'roles-permissions', permission: 'ROL_GESTIONAR' },
      { label: 'Categorías', icon: 'category', route: 'mod-categorias' },
      { label: 'Infracciones', icon: 'gavel', route: 'infracciones' },
      { label: 'Flujos de Trabajo', icon: 'account_tree', route: 'flujos' },
      { label: 'Notificaciones', icon: 'notifications', route: 'notificaciones' },
      { label: 'Configuración', icon: 'settings', route: 'settings' }
    ]
  },
  ADMIN: {
    basePath: '/admin',
    items: [
      { label: 'Overview', icon: 'dashboard', route: 'users' },
      { label: 'Gestión de Usuarios', icon: 'group', route: 'users', permission: 'USUARIO_VER' },
      { label: 'Gestión de Países', icon: 'public', route: 'paises', permission: 'PAIS_VER' },
      { label: 'Roles y Permisos', icon: 'lock_person', route: 'roles-permissions', permission: 'ROL_GESTIONAR' },
      { label: 'Categorías', icon: 'category', route: 'mod-categorias' },
      { label: 'Infracciones', icon: 'gavel', route: 'infracciones' },
      { label: 'Flujos de Trabajo', icon: 'account_tree', route: 'flujos' },
      { label: 'Notificaciones', icon: 'notifications', route: 'notificaciones' },
      { label: 'Configuración', icon: 'settings', route: 'settings' }
    ]
  },
  MODERADOR: {
    basePath: '/admin',
    items: [
      { label: 'Overview', icon: 'dashboard', route: 'mod-overview' },
      { label: 'Verificaciones', icon: 'verified', route: 'verificaciones', permission: 'CERTIFICADO_REVISAR' },
      { label: 'Portafolios', icon: 'palette', route: 'mod-portafolios', permission: 'PORTAFOLIO_MODERAR' },
      { label: 'Comentarios', icon: 'chat', route: 'mod-comentarios', permission: 'COMENTARIO_MODERAR' },
      { label: 'Categorías', icon: 'category', route: 'mod-categorias', permission: 'CATEGORIA_GESTIONAR' },
      { label: 'Notificaciones', icon: 'notifications', route: 'notificaciones' }
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
  },
  CLIENTE: {
    basePath: '/dashboard',
    items: [
      { label: 'Overview', icon: 'dashboard', route: 'overview' },
      { label: 'Explorar', icon: 'storefront', route: 'explorar' },
      { label: 'Mis Pedidos', icon: 'shopping_bag', route: 'mis-pedidos' },
      { label: 'Notificaciones', icon: 'notifications', route: 'notificaciones' },
      { label: 'Sorteos', icon: 'celebration', route: 'sorteos' },
      { label: 'Mi Perfil', icon: 'person', route: 'perfil' }
    ]
  },
  CREADOR: {
    basePath: '/creador',
    items: [
      { label: 'Overview', icon: 'dashboard', route: 'overview' },
      { label: 'Mis Servicios', icon: 'storefront', route: 'servicios' },
      { label: 'Comisiones', icon: 'shopping_bag', route: 'comisiones' },
      { label: 'Briefings', icon: 'assignment', route: 'briefings' },
      { label: 'Notificaciones', icon: 'notifications', route: 'notificaciones' },
      { label: 'Reseñas', icon: 'rate_review', route: 'resenas' },
      { label: 'Sorteos', icon: 'celebration', route: 'sorteos' },
      { label: 'Portafolio', icon: 'folder_special', route: 'portafolio' },
      { label: 'Certificados IA', icon: 'verified', route: 'certificados' },
      { label: 'Mi Perfil', icon: 'person', route: 'perfil' }
    ]
  }
};
