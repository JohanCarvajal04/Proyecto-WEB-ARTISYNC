import { Component, Input, OnInit, inject, signal, computed } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { ResenaClienteService } from '../../services/resena-cliente.service';
import { RespuestaResena } from '../../models/social.model';
import { ToastService } from '../../../../core/services/toast.service';

/** Tope declarado por @Size en PeticionCrearResena. */
const MAX_TEXTO = 2000;

@Component({
  selector: 'app-resena-form',
  standalone: true,
  imports: [NgTemplateOutlet],
  templateUrl: './resena-form.component.html'
})
export class ResenaFormComponent implements OnInit {

  @Input({ required: true }) idPedido!: number;

  private resenaService = inject(ResenaClienteService);
  private toast = inject(ToastService);

  readonly calificacion = signal<number>(0);
  readonly hover = signal<number>(0);
  readonly texto = signal<string>('');
  readonly enviando = signal<boolean>(false);
  readonly error = signal<string>('');
  readonly enviada = signal<RespuestaResena | null>(null);
  readonly cargando = signal<boolean>(true);
  readonly editando = signal<boolean>(false);
  readonly eliminando = signal<boolean>(false);

  readonly maxTexto = MAX_TEXTO;
  readonly estrellas = [1, 2, 3, 4, 5];

  readonly puedeEnviar = computed(() => this.calificacion() > 0 && !this.enviando());

  ngOnInit(): void {
    this.resenaService.obtenerMiResena(this.idPedido).subscribe(resena => {
      if (resena) {
        this.enviada.set(resena);
        this.calificacion.set(resena.calificacionEstrellas);
        this.texto.set(resena.textoResena ?? '');
      }
      this.cargando.set(false);
    });
  }

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

  /** Muestra el formulario precargado con la reseña actual para editarla. */
  editar(): void {
    const actual = this.enviada();
    if (!actual) return;
    this.calificacion.set(actual.calificacionEstrellas);
    this.texto.set(actual.textoResena ?? '');
    this.error.set('');
    this.editando.set(true);
  }

  cancelarEdicion(): void {
    this.editando.set(false);
  }

  enviar(): void {
    if (!this.puedeEnviar()) return;

    this.enviando.set(true);
    this.error.set('');

    const textoResena = this.texto().trim();
    const peticion = {
      calificacionEstrellas: this.calificacion(),
      textoResena: textoResena.length > 0 ? textoResena : null
    };

    const esEdicion = this.enviada() !== null;
    const solicitud = esEdicion
      ? this.resenaService.actualizarResena(this.idPedido, peticion)
      : this.resenaService.crearResena(this.idPedido, peticion);

    solicitud.subscribe({
      next: (resena) => {
        this.enviada.set(resena);
        this.enviando.set(false);
        this.editando.set(false);
        this.toast.success(esEdicion ? 'Reseña actualizada' : '¡Gracias por tu reseña!');
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudo publicar la reseña');
        this.enviando.set(false);
      }
    });
  }

  eliminar(): void {
    if (this.eliminando()) return;
    if (!confirm('¿Seguro que quieres eliminar tu reseña?')) return;

    this.eliminando.set(true);
    this.resenaService.eliminarResena(this.idPedido).subscribe({
      next: () => {
        this.enviada.set(null);
        this.calificacion.set(0);
        this.texto.set('');
        this.eliminando.set(false);
        this.toast.success('Reseña eliminada');
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'No se pudo eliminar la reseña');
        this.eliminando.set(false);
      }
    });
  }
}
