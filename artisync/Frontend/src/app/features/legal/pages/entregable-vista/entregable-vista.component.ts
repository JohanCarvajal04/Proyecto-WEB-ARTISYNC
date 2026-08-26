import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { EntregableService } from '../../services/entregable.service';
import { RespuestaEntregable } from '../../models/legal.model';
import { AuthService } from '../../../seguridad/services/auth.service';
import { PedidoService } from '../../../pedido/services/pedido.service';
import { RespuestaPedido } from '../../../pedido/models/pedido.model';
import { ACEPTA_ENTREGABLE, formatSize, validarEntregable } from '../../utils/archivo-entregable';
import { descargarBlob } from '../../../../shared/utils/descarga-archivo';

type TipoPrevisualizacion = 'imagen' | 'video' | 'otro';

@Component({
  selector: 'app-entregable-vista',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './entregable-vista.component.html'
})
export class EntregableVistaComponent implements OnInit, OnDestroy {
  entregable: RespuestaEntregable | null = null;
  /** Solo para saber quién es el cliente/creador de este pedido (ver esCliente/esCreador). */
  pedido: RespuestaPedido | null = null;
  loading = true;
  error = '';
  successMsg = '';

  // Subida (Creador)
  archivoMarcaAgua: File | null = null;
  archivoLimpia: File | null = null;
  subiendo = false;

  // Previsualización con marca de agua
  previewUrl: string | null = null;
  previewTipo: TipoPrevisualizacion = 'otro';
  cargandoPreview = false;

  // Aprobar (Cliente)
  aprobando = false;

  idPedido = 0;

  readonly tiposAceptados = ACEPTA_ENTREGABLE;
  readonly formatSize = formatSize;

  constructor(
    private entregableService: EntregableService,
    private pedidoService: PedidoService,
    public authService: AuthService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.idPedido = Number(this.route.snapshot.paramMap.get('idPedido'));
    this.cargarEntregable();

    // RespuestaEntregable no trae quién es el cliente/creador (y ni siquiera
    // existe hasta que el creador sube algo), así que se carga el pedido
    // aparte para saberlo desde ya — ver esCliente/esCreador.
    this.pedidoService.obtenerPedido(this.idPedido)
      .pipe(catchError(() => of(null)))
      .subscribe(pedido => {
        this.pedido = pedido;
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.liberarPreview();
  }

  cargarEntregable(): void {
    this.loading = true;
    this.entregableService.obtenerEntregable(this.idPedido).subscribe({
      next: (ent) => {
        this.entregable = ent;
        this.loading = false;
        this.cargarPreview();
        this.cdr.markForCheck();
      },
      error: () => {
        this.entregable = null;
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * El blob viaja con el JWT que pone el interceptor; el object URL resultante
   * sí se puede montar en un <img> o <video>.
   */
  private cargarPreview(): void {
    if (!this.entregable?.urlVersionMarcaAgua) return;

    this.liberarPreview();
    this.cargandoPreview = true;

    this.entregableService.descargarVersionMarcaAgua(this.idPedido).subscribe({
      next: (blob) => {
        this.previewUrl = URL.createObjectURL(blob);
        this.previewTipo = this.tipoDe(blob.type);
        this.cargandoPreview = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.previewUrl = null;
        this.cargandoPreview = false;
        this.cdr.markForCheck();
      }
    });
  }

  private tipoDe(contentType: string): TipoPrevisualizacion {
    if (contentType.startsWith('image/')) return 'imagen';
    if (contentType.startsWith('video/')) return 'video';
    return 'otro';
  }

  private liberarPreview(): void {
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = null;
    }
  }

  seleccionarMarcaAgua(evento: Event): void {
    this.archivoMarcaAgua = this.leerArchivo(evento);
  }

  seleccionarLimpia(evento: Event): void {
    this.archivoLimpia = this.leerArchivo(evento);
  }

  private leerArchivo(evento: Event): File | null {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0] ?? null;
    if (!archivo) return null;

    const validacion = validarEntregable(archivo);
    if (validacion) {
      this.error = validacion;
      input.value = '';
      return null;
    }

    this.error = '';
    return archivo;
  }

  subirEntregable(): void {
    if (!this.archivoMarcaAgua || !this.archivoLimpia) {
      this.error = 'Debes seleccionar ambos archivos: la versión con marca de agua y la versión limpia.';
      return;
    }

    this.subiendo = true;
    this.error = '';

    this.entregableService.subirEntregable(this.idPedido, this.archivoMarcaAgua, this.archivoLimpia).subscribe({
      next: (ent) => {
        this.entregable = ent;
        this.subiendo = false;
        this.successMsg = 'Entregable subido exitosamente';
        this.archivoMarcaAgua = null;
        this.archivoLimpia = null;
        this.cargarPreview();
        this.cdr.markForCheck();
        setTimeout(() => { this.successMsg = ''; this.cdr.markForCheck(); }, 4000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al subir entregable';
        this.subiendo = false;
        this.cdr.markForCheck();
      }
    });
  }

  aprobarEntrega(): void {
    if (!confirm('¿Estás seguro? Al aprobar se liberarán los fondos al creador.')) return;

    this.aprobando = true;
    this.error = '';

    this.entregableService.aprobarEntrega(this.idPedido).subscribe({
      next: () => {
        this.aprobando = false;
        this.successMsg = '¡Entrega aprobada! Fondos liberados al creador.';
        this.cargarEntregable();
        this.cdr.markForCheck();
        setTimeout(() => { this.successMsg = ''; this.cdr.markForCheck(); }, 5000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al aprobar la entrega';
        this.aprobando = false;
        this.cdr.markForCheck();
      }
    });
  }

  descargarLimpia(): void {
    this.entregableService.descargarVersionLimpia(this.idPedido).subscribe({
      next: (blob) => descargarBlob(blob, `entregable_${this.idPedido}_limpio`),
      error: (err) => {
        this.error = err.error?.message || 'El entregable no está disponible hasta que el pago sea liberado';
        this.cdr.markForCheck();
      }
    });
  }

  descargarMarcaAgua(): void {
    this.entregableService.descargarVersionMarcaAgua(this.idPedido).subscribe({
      next: (blob) => descargarBlob(blob, `vista_previa_pedido_${this.idPedido}`),
      error: (err) => {
        this.error = err.error?.message || 'No se pudo descargar la previsualización';
        this.cdr.markForCheck();
      }
    });
  }

  // Por identidad, no por rol global — mismo motivo que en pedido-detalle:
  // el backend (EntregableServicioImpl) tampoco da bypass a ADMIN, y una
  // cuenta con ambos roles CLIENTE y CREADOR veía botones de la parte que no
  // le correspondía en este pedido.
  get esCreador(): boolean {
    return this.pedido?.idCreador === this.authService.getCurrentUserId();
  }

  get esCliente(): boolean {
    return this.pedido?.idCliente === this.authService.getCurrentUserId();
  }
}
