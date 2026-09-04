import { Component, input, computed } from '@angular/core';

@Component({
  selector: 'app-avatar',
  standalone: true,
  template: `
    <div 
      [class]="containerClasses()"
      class="rounded-full overflow-hidden bg-surface-container shrink-0 border border-outline-variant flex items-center justify-center text-on-surface-variant font-medium select-none">
      @if (imageUrl()) {
        <img [src]="imageUrl()" [alt]="name()" [attr.width]="sizePx()" [attr.height]="sizePx()" decoding="async" class="w-full h-full object-cover">
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
      // Los tres usos de 'lg' son tarjetas de identidad con banner (Mi Cuenta,
      // Mi Perfil de cliente y de creador): el avatar se solapa sobre el
      // banner con un margen negativo en el contenedor, y con items-end el
      // bloque de nombre/correo queda a la misma altura que el avatar. A 48px
      // (el tamaño anterior) el avatar y el texto tenían la misma altura, así
      // que el nombre quedaba tan metido en el banner como el propio avatar,
      // ilegible en su mitad superior. Un avatar más alto que el bloque de
      // texto es lo que hace que, alineados por abajo, solo el avatar
      // sobresalga hacia el banner y el nombre quede dentro del área blanca.
      case 'lg': return 'w-20 h-20 text-2xl';
      case 'md':
      default: return 'w-10 h-10 text-sm';
    }
  });

  sizePx = computed(() => {
    switch (this.size()) {
      case 'sm': return 32;
      case 'lg': return 80;
      case 'md':
      default: return 40;
    }
  });
}
