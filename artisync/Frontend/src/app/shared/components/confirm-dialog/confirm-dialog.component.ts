import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  template: `
    @if (isOpen()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-fadeIn">
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 max-w-md w-full shadow-xl flex flex-col gap-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-error-container text-error flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined">warning</span>
            </div>
            <div>
              <h3 class="font-headline text-lg font-bold text-primary">{{ title() }}</h3>
              <p class="text-sm text-on-surface-variant mt-1">{{ message() }}</p>
            </div>
          </div>
          <div class="flex justify-end gap-3 mt-2">
            <button 
              type="button" 
              (click)="onCancel.emit()"
              class="px-4 py-2 rounded-lg border border-outline-variant text-on-surface text-sm font-medium hover:bg-surface-container transition-colors">
              {{ cancelText() }}
            </button>
            <button 
              type="button" 
              (click)="onConfirm.emit()"
              class="px-4 py-2 rounded-lg bg-error text-on-error text-sm font-medium hover:opacity-90 transition-opacity">
              {{ confirmText() }}
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class ConfirmDialogComponent {
  isOpen = input<boolean>(false);
  title = input<string>('Confirmar acción');
  message = input<string>('¿Estás seguro de que deseas continuar?');
  confirmText = input<string>('Continuar');
  cancelText = input<string>('Cancelar');

  onConfirm = output<void>();
  onCancel = output<void>();
}
