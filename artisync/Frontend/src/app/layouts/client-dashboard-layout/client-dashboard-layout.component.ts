import { Component, inject, computed, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../features/seguridad/services/auth.service';
import { NAV_CONFIG, NavItem } from '../../core/config/nav.config';
import { AvatarComponent } from '../../shared/components/avatar/avatar.component';

@Component({
  selector: 'app-client-dashboard-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AvatarComponent],
  templateUrl: './client-dashboard-layout.component.html'
})
export class ClientDashboardLayoutComponent {
  authService = inject(AuthService);

  readonly isMobileMenuOpen = signal<boolean>(false);

  userEmail = computed(() => this.authService.currentUser()?.email || this.authService.currentUser()?.sub || 'cliente@artisync.com');
  userName = computed(() => {
    const email = this.userEmail();
    const prefix = email.split('@')[0];
    return prefix.charAt(0).toUpperCase() + prefix.slice(1);
  });
  userRole = computed(() => this.authService.primaryRole() || 'Cliente');

  navItems = computed<NavItem[]>(() => {
    const role = this.authService.primaryRole() || 'CLIENTE';
    const config = NAV_CONFIG[role] || NAV_CONFIG['CLIENTE'];
    return config.items;
  });

  navBasePath = computed(() => {
    const role = this.authService.primaryRole() || 'CLIENTE';
    return NAV_CONFIG[role]?.basePath || '/dashboard';
  });

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
  }

  logout(): void {
    this.authService.logout();
  }
}
