import { Component, inject, signal, OnInit } from '@angular/core';
import { ToastService } from '../../../../core/services/toast.service';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { PerfilRequeridoComponent } from '../../components/perfil-requerido.component';
import {
  RespuestaVerificacion,
  VerificacionService
} from '../../../perfil/services/verificacion.service';
import { formatDateTime, mensajeError } from '../../utils/formato';

/**
 * Espejo de PreprocesadorImagenIa.validarFormato: el certificado se analiza
 * como imagen, así que solo JPEG/PNG y 5 MB.
 */
const TIPOS_CERTIFICADO = ['image/jpeg', 'image/png'];
const MAX_BYTES = 5 * 1024 * 1024;

/**
 * Carga de certificados profesionales (RF-07).
 *
 * La versión anterior llamaba a `POST /v1/certificados` y
 * `GET /v1/certificados/perfil/{id}`: ambos exigen ADMIN o CERTIFICADO_REVISAR,
 * así que un CREADOR recibía 403 en las dos. Además el formulario pedía teclear
 * la URL del documento y dejaba auto-asignarse el estado de verificación y el
 * puntaje de la IA, que son salidas del moderador y del análisis, no entradas
 * del usuario.
 *
 * El camino correcto para el creador es `POST /v1/verificaciones` con
 * `tipo=CERTIFICADO`, que acepta a cualquier usuario autenticado y encola el
 * documento para revisión.
 */
@Component({
  selector: 'app-certificados',
  standalone: true,
  imports: [PerfilRequeridoComponent],
  templateUrl: './certificados.component.html',
  styleUrl: './certificados.component.css'
})
export class CertificadosComponent implements OnInit {

  private verificacionService = inject(VerificacionService);
  private contexto = inject(CreadorContextoService);
  private toast = inject(ToastService);

  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string>('');

  readonly documento = signal<File | null>(null);
  readonly enviando = signal<boolean>(false);

  /**
   * Solicitudes hechas en esta sesión. No hay endpoint «mis verificaciones»
   * en el backend, así que no se puede reconstruir el histórico al recargar.
   */
  readonly solicitudes = signal<RespuestaVerificacion[]>([]);

  readonly perfilFaltante = this.contexto.perfilFaltante;

  readonly tiposAceptados = TIPOS_CERTIFICADO.join(',');

  ngOnInit(): void {
    this.contexto.obtenerPerfil().subscribe({
      next: () => this.isLoading.set(false),
      error: (err) => {
        this.error.set(mensajeError(err, 'No se pudo cargar tu perfil de creador'));
        this.isLoading.set(false);
      }
    });
  }

  onDocumento(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0] ?? null;
    if (!archivo) {
      this.documento.set(null);
      return;
    }

    if (!TIPOS_CERTIFICADO.includes(archivo.type)) {
      this.toast.warning(`Formato no soportado: ${archivo.type || 'desconocido'}. Se aceptan JPG o PNG.`);
      input.value = '';
      this.documento.set(null);
      return;
    }
    if (archivo.size > MAX_BYTES) {
      this.toast.warning('El documento supera el máximo de 5 MB.');
      input.value = '';
      this.documento.set(null);
      return;
    }

    this.documento.set(archivo);
  }

  enviar(): void {
    const archivo = this.documento();
    if (!archivo || this.enviando()) return;

    this.enviando.set(true);
    this.verificacionService.solicitar('CERTIFICADO', archivo).subscribe({
      next: (respuesta) => {
        this.solicitudes.update(lista => [respuesta, ...lista]);
        this.documento.set(null);
        this.enviando.set(false);
        this.toast.success('Certificado enviado a revisión');
      },
      error: (err) => {
        this.enviando.set(false);
        this.toast.error(mensajeError(err, 'No se pudo enviar el certificado'));
      }
    });
  }

  badgeEstado(nombre: string): string {
    const lower = (nombre || '').toLowerCase();
    if (lower.includes('verific') || lower.includes('aprob')) return 'cr-badge cr-badge--ok';
    if (lower.includes('rechaz')) return 'cr-badge cr-badge--danger';
    return 'cr-badge cr-badge--warn';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  formatDateTime = formatDateTime;
}
