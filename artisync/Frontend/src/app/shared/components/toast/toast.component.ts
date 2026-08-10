import { Component, inject } from '@angular/core';
import { ToastService, Toast } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  template: `
    <div class="fixed top-5 right-5 z-50 flex flex-col gap-3 max-w-sm w-full pointer-events-none">
      @for (toast of toastService.toasts(); track toast.id) {
        <div 
          class="pointer-events-auto flex items-center justify-between p-4 rounded-2xl shadow-md border text-sm font-medium transition-all duration-300 transform translate-y-0 opacity-100 backdrop-blur-sm"
          [style]="getStyle(toast.type)">
          <div class="flex items-center gap-3">
            <span class="shrink-0 flex items-center justify-center">
              @if (toast.type === 'success') {
                <svg class="w-5 h-5 text-[#2D5A1E]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              } @else if (toast.type === 'warning') {
                <svg class="w-5 h-5 text-[#7A3E00]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              } @else if (toast.type === 'error') {
                <svg class="w-5 h-5 text-[#7A1C28]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              } @else {
                <svg class="w-5 h-5 text-[#2D1B4E]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              }
            </span>
            <span>{{ toast.message }}</span>
          </div>
          <button (click)="toastService.remove(toast.id)" class="opacity-60 hover:opacity-100 transition-opacity p-1 ml-2">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      }
    </div>
  `
})
export class ToastComponent {
  toastService = inject(ToastService);

  getStyle(type: Toast['type']): Record<string, string> {
    switch (type) {
      case 'success':
        return {
          'background-color': '#E2F0CB',
          'color': '#2D5A1E',
          'border-color': 'rgba(130, 180, 80, 0.4)'
        };
      case 'warning':
        return {
          'background-color': '#FFD8B1',
          'color': '#7A3E00',
          'border-color': 'rgba(230, 150, 80, 0.4)'
        };
      case 'error':
        return {
          'background-color': '#FDE2E4',
          'color': '#7A1C28',
          'border-color': 'rgba(220, 140, 150, 0.4)'
        };
      case 'info':
      default:
        return {
          'background-color': '#EAEFF9',
          'color': '#2D1B4E',
          'border-color': 'rgba(160, 120, 200, 0.3)'
        };
    }
  }
}
