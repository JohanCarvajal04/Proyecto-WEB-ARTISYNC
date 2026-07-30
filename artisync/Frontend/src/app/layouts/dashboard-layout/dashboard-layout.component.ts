import { Component, inject, computed, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../features/seguridad/services/auth.service';
import { NAV_CONFIG, NavItem } from '../../core/config/nav.config';
import { AvatarComponent } from '../../shared/components/avatar/avatar.component';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AvatarComponent],
  templateUrl: './dashboard-layout.component.html'
})
export class DashboardLayoutComponent {
  authService = inject(AuthService);
  
  readonly isMobileMenuOpen = signal<boolean>(false);

  userEmail = computed(() => this.authService.currentUser()?.email || this.authService.currentUser()?.sub || 'admin@artisync.com');
  userName = computed(() => {
    const email = this.userEmail();
    const prefix = email.split('@')[0];
    return prefix.charAt(0).toUpperCase() + prefix.slice(1);
  });
  userRole = computed(() => this.authService.primaryRole() || 'Administrador');

  navItems = computed<NavItem[]>(() => {
    const role = this.authService.primaryRole() || 'ADMINISTRADOR';
    const rawItems = NAV_CONFIG[role]?.items || NAV_CONFIG['ADMINISTRADOR'].items;
    const isAdmin = role === 'ADMINISTRADOR' || role === 'ADMIN';
    return rawItems.filter(item => !item.permission || isAdmin || this.authService.hasPermission(item.permission));
  });

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
  }

  logout(): void {
    this.authService.logout();
  }
}
