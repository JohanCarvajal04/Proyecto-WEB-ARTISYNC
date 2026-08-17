import { Component, input, computed } from '@angular/core';

@Component({
  selector: 'app-avatar',
  standalone: true,
  template: `
    <div 
      [class]="containerClasses()"
      class="rounded-full overflow-hidden bg-surface-container shrink-0 border border-outline-variant flex items-center justify-center text-on-surface-variant font-medium select-none">
      @if (imageUrl()) {
        <img [src]="imageUrl()" [alt]="name()" class="w-full h-full object-cover">
      } @else {
        <span>{{ initials() }}</span>
      }
    </div>
  `
})
export class AvatarComponent {
  name = input<string>('');
  imageUrl = input<string | undefined | null>(null);
  size = input<'sm' | 'md' | 'lg'>('md');

  initials = computed(() => {
    const n = this.name().trim();
    if (!n) return 'U';
    const parts = n.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return n.substring(0, 2).toUpperCase();
  });

  containerClasses = computed(() => {
    switch (this.size()) {
      case 'sm': return 'w-8 h-8 text-xs';
      case 'lg': return 'w-12 h-12 text-base';
      case 'md':
      default: return 'w-10 h-10 text-sm';
    }
  });
}
