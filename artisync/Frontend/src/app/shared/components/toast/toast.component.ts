import { Component, inject } from '@angular/core';
import { ToastService, Toast } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  template: `
    <div class="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none">
      @for (toast of toastService.toasts(); track toast.id) {
        <div 
          class="pointer-events-auto flex items-center justify-between p-4 rounded-lg shadow-lg border text-sm font-medium transition-all duration-300 transform translate-y-0 opacity-100"
          [class]="getClasses(toast.type)">
          <div class="flex items-center gap-3">
            <span class="material-symbols-outlined text-[20px]">{{ getIcon(toast.type) }}</span>
            <span>{{ toast.message }}</span>
          </div>
          <button (click)="toastService.remove(toast.id)" class="opacity-70 hover:opacity-100 transition-opacity p-1">
            <span class="material-symbols-outlined text-[18px]">close</span>
          </button>
        </div>
      }
    </div>
  `
})
export class ToastComponent {
  toastService = inject(ToastService);

  getClasses(type: Toast['type']): string {
    switch (type) {
      case 'success':
        return 'bg-surface-container-lowest border-green-600 text-green-800 shadow-sm';
      case 'error':
        return 'bg-error-container border-error text-on-error-container shadow-sm';
      case 'warning':
        return 'bg-yellow-50 border-yellow-500 text-yellow-800 shadow-sm';
      case 'info':
      default:
        return 'bg-surface-container-lowest border-outline text-on-surface shadow-sm';
    }
  }

  getIcon(type: Toast['type']): string {
    switch (type) {
      case 'success': return 'check_circle';
      case 'error': return 'error';
      case 'warning': return 'warning';
      case 'info': default: return 'info';
    }
  }
}
