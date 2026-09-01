import { Routes } from '@angular/router';
import { guestGuard } from './core/guards/guest.guard';
import { authGuard } from './core/guards/auth.guard';
import { panelGatePermissions } from './core/config/nav.config';
import { CATALOGO_BASE_PATH } from './features/catalogo/catalogo.config';

export const routes: Routes = [
  // ─── Fuera del cascarón: sin sesión, sin encabezado ni menú ───
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadChildren: () => import('./features/seguridad/seguridad.routes').then(m => m.SEGURIDAD_ROUTES)
  },
  {
    path: 'no-autorizado',
    loadComponent: () => import('./pages/public/unauthorized/unauthorized.component').then(m => m.UnauthorizedComponent)
  },
  {
    path: 'acceso-requerido',
    loadComponent: () => import('./pages/public/acceso-requerido/acceso-requerido.component').then(m => m.AccesoRequeridoComponent)
  },

  // Catálogo público (M3): vitrina de servicios navegable sin sesión, con su
  // propio cascarón ligero (sin sidebar). NO puede llevar `path: ''`: eso
  // reproduciría el mismo fallo que documenta el redirect de abajo (casaría
  // con la URL vacía y dejaría la app en blanco al no encontrar hijo), así
  // que va con segmento propio.
  {
    path: 'explorar',
    loadComponent: () => import('./layouts/public-shell/public-shell.component').then(m => m.PublicShellComponent),
    providers: [{ provide: CATALOGO_BASE_PATH, useValue: '/explorar' }],
    loadChildren: () => import('./features/catalogo/catalogo.routes').then(m => m.CATALOGO_ROUTES)
  },

  // Este redirect DEBE ir antes del cascarón. El cascarón es `path: ''` sin
  // `pathMatch`, así que casaría con la URL vacía y luego fallaría al no
  // encontrar hijo, dejando la aplicación en blanco en vez de ir al catálogo.
  { path: '', redirectTo: '/explorar', pathMatch: 'full' },

  // ─── Cascarón: toda ruta autenticada cuelga de aquí ───
  // Ruta sin segmento propio: aporta el encabezado y el menú sin alterar
  // ninguna URL. Antes el cascarón se montaba con `loadComponent` en cuatro
  // de las nueve ramas, y las cinco restantes (/pedido, /legal, /profile...)
  // se renderizaban desnudas — de ahí que a mitad de un flujo de pedido
  // desapareciera la navegación.
  //
  // No lleva `data`: con `paramsInheritanceStrategy: 'emptyOnly'` (el valor
  // por defecto), un padre de path vacío propaga su `data` a TODOS sus hijos,
  // así que cualquier permiso puesto aquí se le exigiría también a /legal y
  // /profile.
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layouts/app-shell/app-shell.component').then(m => m.AppShellComponent),
    children: [
      {
        // La puerta del panel era una lista fija de cinco nombres de rol, así
        // que un rol creado por el administrador no podía entrar aunque se le
        // hubieran concedido permisos administrativos. Ahora basta con tener
        // alguno: qué páginas ve dentro lo deciden los guards de cada ruta hija.
        path: 'admin',
        canActivate: [authGuard],
        data: { permissions: panelGatePermissions('admin') },
        loadChildren: () => import('./features/administracion/administracion.routes').then(m => m.ADMINISTRACION_ROUTES)
      },
      {
        // Panel base de la aplicación (catálogo público, pedidos propios,
        // perfil): cualquier usuario autenticado. Antes exigía el rol CLIENTE
        // o CREADOR, lo que dejaba fuera a cualquier rol nuevo sin necesidad.
        path: 'dashboard',
        loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
      },
      {
        path: 'creador',
        canActivate: [authGuard],
        data: { permissions: panelGatePermissions('creador') },
        loadChildren: () => import('./features/creador/creador.routes').then(m => m.CREADOR_ROUTES)
      },
      {
        // Panel de cuenta: notificaciones y configuración propia, sin exigir
        // ningún permiso. Es el destino de resolvePanel() para cualquier
        // usuario sin permisos asignados (rol nuevo o rol conocido vaciado) —
        // ver nav.config.ts#resolvePanel. Al no llevar `data.permissions`,
        // cualquier usuario autenticado puede entrar, no solo el que tiene
        // este panel activo.
        path: 'cuenta',
        loadChildren: () => import('./features/cuenta/cuenta.routes').then(m => m.CUENTA_ROUTES)
      },

      // Las tres siguientes son las que hasta ahora perdían el cascarón. Sus
      // URLs no cambian, así que los routerLink/navigate del resto de la
      // aplicación que ya apuntan aquí siguen siendo válidos sin tocarlos.
      {
        path: 'pedido',
        loadChildren: () => import('./features/pedido/pedido.routes').then(m => m.PEDIDO_ROUTES)
      },
      {
        path: 'legal',
        loadChildren: () => import('./features/legal/legal.routes').then(m => m.LEGAL_ROUTES)
      },
      {
        path: 'profile',
        loadChildren: () => import('./features/perfil/perfil.routes').then(m => m.PERFIL_ROUTES)
      }
    ]
  },

  { path: '**', redirectTo: '/explorar' }
];
