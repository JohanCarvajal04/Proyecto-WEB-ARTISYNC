import { Component, Input, inject, signal, computed } from '@angular/core';
import { ResenaClienteService } from '../../services/resena-cliente.service';
import { RespuestaResena } from '../../models/social.model';
import { ToastService } from '../../../../core/services/toast.service';

/** Tope declarado por @Size en PeticionCrearResena. */
const MAX_TEXTO = 2000;

@Component({
  selector: 'app-resena-form',
  standalone: true,
  imports: [],
  templateUrl: './resena-form.component.html'
})
export class ResenaFormComponent {

  @Input({ required: true }) idPedido!: number;

  private resenaService = inject(ResenaClienteService);
  private toast = inject(ToastService);

  readonly calificacion = signal<number>(0);
  readonly hover = signal<number>(0);
  readonly texto = signal<string>('');
  readonly enviando = signal<boolean>(false);
  readonly error = signal<string>('');
  readonly enviada = signal<RespuestaResena | null>(null);

  readonly maxTexto = MAX_TEXTO;
  readonly estrellas = [1, 2, 3, 4, 5];

  readonly puedeEnviar = computed(() => this.calificacion() > 0 && !this.enviando());

  /** La estrella se pinta si está bajo el cursor o bajo la nota ya elegida. */
  estrellaActiva(n: number): boolean {
    return n <= (this.hover() || this.calificacion());
  }

  seleccionar(n: number): void {
    this.calificacion.set(n);
  }

  onHover(n: number): void {
    this.hover.set(n);
  }

  onTexto(evento: Event): void {
    this.texto.set((evento.target as HTMLTextAreaElement).value);
  }

  enviar(): void {
    if (!this.puedeEnviar()) return;

    this.enviando.set(true);
    this.error.set('');

    const textoResena = this.texto().trim();

    this.resenaService.crearResena(this.idPedido, {
      calificacionEstrellas: this.calificacion(),
      textoResena: textoResena.length > 0 ? textoResena : null
    }).subscribe({
      next: (resena) => {
        this.enviada.set(resena);
        this.enviando.set(false);
        this.toast.success('¡Gracias por tu reseña!');
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudo publicar la reseña');
        this.enviando.set(false);
      }
    });
  }
}
