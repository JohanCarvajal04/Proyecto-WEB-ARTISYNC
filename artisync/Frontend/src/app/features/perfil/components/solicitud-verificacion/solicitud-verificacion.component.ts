import { Component, inject, signal, computed } from '@angular/core';
import {
  RespuestaVerificacion,
  TipoDocumentoVerificacion,
  VerificacionService
} from '../../services/verificacion.service';
import { ToastService } from '../../../../core/services/toast.service';

/**
 * Espejo de PreprocesadorImagenIa.validarFormato: solo JPEG/PNG y 5 MB. El
 * documento se analiza como imagen, por eso no se aceptan PDF.
 */
const TIPOS_DOCUMENTO = ['image/jpeg', 'image/png'];
const MAX_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-solicitud-verificacion',
  standalone: true,
  imports: [],
  templateUrl: './solicitud-verificacion.component.html'
})
export class SolicitudVerificacionComponent {

  private verificacionService = inject(VerificacionService);
  private toast = inject(ToastService);

  readonly tipo = signal<TipoDocumentoVerificacion>('IDENTIDAD');
  readonly documento = signal<File | null>(null);
  readonly enviando = signal<boolean>(false);
  readonly error = signal<string>('');
  readonly resultado = signal<RespuestaVerificacion | null>(null);

  readonly tiposAceptados = TIPOS_DOCUMENTO.join(',');

  readonly puedeEnviar = computed(() => this.documento() !== null && !this.enviando());

  seleccionarTipo(tipo: TipoDocumentoVerificacion): void {
    this.tipo.set(tipo);
  }

  onDocumento(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0] ?? null;
    if (!archivo) {
      this.documento.set(null);
      return;
    }

    if (!TIPOS_DOCUMENTO.includes(archivo.type)) {
      this.error.set(`Formato no soportado: ${archivo.type || 'desconocido'}. Se aceptan JPG o PNG.`);
      input.value = '';
      this.documento.set(null);
      return;
    }
    if (archivo.size > MAX_BYTES) {
      this.error.set('El documento supera el máximo de 5 MB.');
      input.value = '';
      this.documento.set(null);
      return;
    }

    this.error.set('');
    this.documento.set(archivo);
  }

  enviar(): void {
    const archivo = this.documento();
    if (!archivo || this.enviando()) return;

    this.enviando.set(true);
    this.error.set('');

    this.verificacionService.solicitar(this.tipo(), archivo).subscribe({
      next: (respuesta) => {
        this.resultado.set(respuesta);
        this.documento.set(null);
        this.enviando.set(false);
        this.toast.success('Documento enviado. Un moderador lo revisará.');
      },
      error: (err) => {
        this.error.set(err.error?.message || 'No se pudo enviar el documento');
        this.enviando.set(false);
      }
    });
  }

  nuevaSolicitud(): void {
    this.resultado.set(null);
    this.error.set('');
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
