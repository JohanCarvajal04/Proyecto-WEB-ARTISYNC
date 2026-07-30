import { Component } from '@angular/core';

@Component({
  selector: 'app-spinner',
  standalone: true,
  template: `
    <div class="flex items-center justify-center p-4">
      <div class="animate-spin rounded-full h-6 w-6 border-2 border-primary border-t-transparent"></div>
    </div>
  `
})
export class LoadingSpinnerComponent {}
