import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RespaldoService } from '../../services/respaldo.service';
import { Respaldo, ResumenRespaldos, RespaldoPolitica } from '../../models/respaldo.model';
import { Pagina, paginaVacia } from '../../../../shared/models/pagina.model';
import { ToastService } from '../../../../core/services/toast.service';
import { AuthService } from '../../../seguridad/services/auth.service';
import { descargarRespuesta, mensajeErrorBlob } from '../../../../shared/utils/descarga-archivo';

import { NavIconComponent } from '../../../../shared/components/nav-icon/nav-icon.component';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

interface RespaldoRef {
  id: number;
  nombre: string;
}

const CRON_CHARS_VALIDOS = /^[\d*/,\-?LWlw ]+$/;

@Component({
  selector: 'app-respaldos',
  standalone: true,
  imports: [CommonModule, FormsModule, NavIconComponent, ConfirmDialogComponent],
  templateUrl: './respaldos.component.html'
})
export class RespaldosComponent implements OnInit {

  private respaldoService = inject(RespaldoService);
  private toastService = inject(ToastService);
  private authService = inject(AuthService);

  readonly pagina = signal<Pagina<Respaldo>>(paginaVacia());
  readonly resumen = signal<ResumenRespaldos | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly errorCarga = signal<string | null>(null);

  readonly filtroTipo = signal<string>('TODOS');
  readonly filtroCategoria = signal<string>('TODAS');

  readonly puedeCrear = signal<boolean>(false);
  readonly puedeEliminar = signal<boolean>(false);
  readonly puedeRestaurar = signal<boolean>(false);
  readonly puedeConfigurar = signal<boolean>(false);

  readonly showConfigModal = signal<boolean>(false);
  readonly configPolitica = signal<RespaldoPolitica>({
    idPolitica: 1, cronFull: '', retencionDiasFull: 0, cronDiario: '', retencionDiasDiario: 0
  });
  readonly cronErrorFull = signal<string | null>(null);
  readonly cronErrorDiario = signal<string | null>(null);
  readonly guardandoPolitica = signal<boolean>(false);

  // --- Estados de la UI del constructor Cron ---
  readonly cronFullUi = signal<{modo: 'DIARIO'|'SEMANAL'|'MENSUAL'|'AVANZADO', hora: string, diasSemana: string[], diaMes: number, avanzado: string}>({
    modo: 'SEMANAL', hora: '03:00', diasSemana: ['SUN'], diaMes: 1, avanzado: '0 0 3 * * SUN'
  });
  
  readonly cronDiarioUi = signal<{modo: 'DIARIO'|'SEMANAL'|'MENSUAL'|'AVANZADO', hora: string, diasSemana: string[], diaMes: number, avanzado: string}>({
    modo: 'DIARIO', hora: '02:00', diasSemana: [], diaMes: 1, avanzado: '0 0 2 * * ?'
  });

  readonly diasSemanaList = [
    { value: 'MON', label: 'Lu' },
    { value: 'TUE', label: 'Ma' },
    { value: 'WED', label: 'Mi' },
    { value: 'THU', label: 'Ju' },
    { value: 'FRI', label: 'Vi' },
    { value: 'SAT', label: 'Sá' },
    { value: 'SUN', label: 'Do' }
  ];

  readonly categoriaManual = signal<'FULL' | 'DIARIO'>('FULL');
  readonly creandoManual = signal<boolean>(false);
  readonly mostrarConfirmarCrear = signal<boolean>(false);
  readonly respaldoAEliminar = signal<RespaldoRef | null>(null);
  readonly respaldoARestaurarAviso = signal<RespaldoRef | null>(null);
  readonly respaldoARestaurarTexto = signal<RespaldoRef | null>(null);
  readonly textoConfirmacionRestaurar = signal<string>('');

  readonly importandoArchivo = signal<boolean>(false);

  /** Alerta persistente (no un toast que desaparece a los 5s) cuando una
   *  restauración falla: la BD puede haber quedado a medio restaurar. */
  readonly alertaRestauracionFallida = signal<string | null>(null);

  ngOnInit(): void {
    const isAdmin = this.authService.userRoles().includes('ROLE_ADMIN') || this.authService.primaryRole() === 'ADMINISTRADOR';
    const perms = this.authService.userPermissions();

    this.puedeCrear.set(isAdmin || perms.includes('RESPALDO_CREAR'));
    this.puedeEliminar.set(isAdmin || perms.includes('RESPALDO_ELIMINAR'));
    this.puedeRestaurar.set(isAdmin || perms.includes('RESPALDO_RESTAURAR'));
    this.puedeConfigurar.set(isAdmin || perms.includes('RESPALDO_CONFIGURAR'));

    this.cargarResumen();
    this.cargar(0);
  }

  cargar(page: number): void {
    this.isLoading.set(true);
    this.errorCarga.set(null);
    this.respaldoService.listar(page, this.filtroTipo(), this.filtroCategoria()).subscribe({
      next: (pagina) => {
        this.pagina.set(pagina);
        this.isLoading.set(false);
      },
      error: (err) => {
        // Antes esto caía en pagina=vacia() sin distinción, y la tabla mostraba
        // "No se encontraron respaldos" igual que si de verdad no hubiera
        // ninguno — imposible saber si fue un 401/403 pasajero o 0 resultados
        // reales. Se guarda el motivo aparte para mostrarlo explícitamente.
        const mensaje = err.error?.detail || err.error?.message
          || (err.status === 401 || err.status === 403
              ? 'No tienes permisos para ver los respaldos, o tu sesión expiró. Intenta recargar la página.'
              : 'No se pudieron cargar los respaldos');
        this.errorCarga.set(mensaje);
        this.toastService.error(mensaje);
        this.pagina.set(paginaVacia());
        this.isLoading.set(false);
      }
    });
  }

  cargarResumen(): void {
    this.respaldoService.obtenerResumen().subscribe({
      next: (resumen) => this.resumen.set(resumen),
      error: () => console.error('No se pudo cargar el resumen')
    });
  }

  cambiarFiltro(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.filtroTipo.set(value);
    this.cargar(0);
  }

  cambiarFiltroCategoria(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.filtroCategoria.set(value);
    this.cargar(0);
  }

  irAPagina(numero: number): void {
    if (numero < 0 || numero >= this.pagina().totalPaginas) return;
    this.cargar(numero);
  }

  // --- Crear respaldo manual ---

  crearManual(): void {
    this.mostrarConfirmarCrear.set(true);
  }

  confirmarCrearManual(): void {
    this.mostrarConfirmarCrear.set(false);
    this.creandoManual.set(true);
    this.respaldoService.crearManual(this.categoriaManual()).subscribe({
      next: () => {
        this.creandoManual.set(false);
        this.toastService.success('Respaldo manual completado con éxito');
        this.cargarResumen();
        this.cargar(0);
      },
      error: (err) => {
        this.creandoManual.set(false);
        this.toastService.error(err.error?.detail || err.error?.message || 'Error al crear respaldo manual');
      }
    });
  }

  // --- Importar respaldo externo ---

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo) return;

    if (!archivo.name.toLowerCase().endsWith('.dump')) {
      this.toastService.error('Solo se aceptan archivos .dump');
      return;
    }

    this.importandoArchivo.set(true);
    this.respaldoService.importarExterno(archivo).subscribe({
      next: () => {
        this.importandoArchivo.set(false);
        this.toastService.success('Respaldo externo importado. Ya puede restaurarlo desde la tabla.');
        this.cargarResumen();
        this.cargar(0);
      },
      error: (err) => {
        this.importandoArchivo.set(false);
        this.toastService.error(err.error?.detail || err.error?.message || 'Error al importar el archivo');
      }
    });
  }

  // --- Eliminar respaldo ---

  eliminar(id: number, nombre: string): void {
    this.respaldoAEliminar.set({ id, nombre });
  }

  confirmarEliminar(): void {
    const ref = this.respaldoAEliminar();
    if (!ref) return;
    this.respaldoAEliminar.set(null);

    this.respaldoService.eliminar(ref.id).subscribe({
      next: () => {
        this.toastService.success('Respaldo eliminado');
        this.cargarResumen();
        this.cargar(0);
      },
      error: () => this.toastService.error('Error al eliminar respaldo')
    });
  }

  // --- Restaurar respaldo (dos pasos: aviso + confirmación por texto) ---

  restaurar(id: number, nombre: string): void {
    this.respaldoARestaurarAviso.set({ id, nombre });
  }

  avanzarARestaurarConTexto(): void {
    const ref = this.respaldoARestaurarAviso();
    if (!ref) return;
    this.respaldoARestaurarAviso.set(null);
    this.textoConfirmacionRestaurar.set('');
    this.respaldoARestaurarTexto.set(ref);
  }

  confirmarRestaurarTexto(): void {
    const ref = this.respaldoARestaurarTexto();
    if (!ref) return;

    if (this.textoConfirmacionRestaurar().trim().toUpperCase() !== 'RESTAURAR') {
      this.toastService.error('Debe escribir "RESTAURAR" exactamente para continuar');
      return;
    }

    this.respaldoARestaurarTexto.set(null);
    this.alertaRestauracionFallida.set(null);
    this.isLoading.set(true);
    this.respaldoService.restaurar(ref.id).subscribe({
      next: () => {
        this.toastService.success('Restauración completada con éxito. Es posible que deba iniciar sesión nuevamente.');
        setTimeout(() => window.location.reload(), 3000);
      },
      error: (err) => {
        this.isLoading.set(false);
        const mensaje = err.error?.detail || err.error?.message || 'Error grave al restaurar';
        this.toastService.error(mensaje);
        // Alerta persistente (no un toast de 5s): una restauración fallida
        // puede dejar la BD a medio restaurar, y el admin necesita verlo
        // hasta que decida explícitamente descartar el aviso.
        this.alertaRestauracionFallida.set(mensaje);
      }
    });
  }

  cerrarAlertaRestauracion(): void {
    this.alertaRestauracionFallida.set(null);
  }

  cancelarAccion(): void {
    this.mostrarConfirmarCrear.set(false);
    this.respaldoAEliminar.set(null);
    this.respaldoARestaurarAviso.set(null);
    this.respaldoARestaurarTexto.set(null);
    this.textoConfirmacionRestaurar.set('');
  }

  descargar(id: number, nombreArchivo: string): void {
    this.respaldoService.descargar(id).subscribe({
      next: (respuesta) => descargarRespuesta(respuesta, nombreArchivo),
      error: async (err) => {
        const mensaje = await mensajeErrorBlob(err, 'No se pudo descargar el archivo');
        this.toastService.error(mensaje);
      }
    });
  }

  // --- Configuración de política ---

  abrirConfiguracion(): void {
    this.respaldoService.obtenerPolitica().subscribe({
      next: (pol) => {
        this.configPolitica.set(pol);
        this.cronFullUi.set(this.parseCronToUi(pol.cronFull));
        this.cronDiarioUi.set(this.parseCronToUi(pol.cronDiario));
        this.cronErrorFull.set(null);
        this.cronErrorDiario.set(null);
        this.showConfigModal.set(true);
      },
      error: () => this.toastService.error('Error al cargar la política')
    });
  }

  cerrarConfiguracion(): void {
    this.showConfigModal.set(false);
    this.cronErrorFull.set(null);
    this.cronErrorDiario.set(null);
  }

  validarCronCliente(cron: string): string | null {
    const partes = cron.trim().split(/\s+/);
    if (partes.length !== 6) {
      return 'La expresión cron debe tener 6 campos: segundo minuto hora día-mes mes día-semana';
    }
    if (!CRON_CHARS_VALIDOS.test(cron.trim())) {
      return 'La expresión cron contiene caracteres no válidos';
    }
    return null;
  }

  onCronFullInput(valor: string): void {
    this.configPolitica.update((pol) => ({ ...pol, cronFull: valor }));
    this.cronErrorFull.set(this.validarCronCliente(valor));
  }

  onCronDiarioInput(valor: string): void {
    this.configPolitica.update((pol) => ({ ...pol, cronDiario: valor }));
    this.cronErrorDiario.set(this.validarCronCliente(valor));
  }

  // --- Métodos del constructor visual de Cron ---

  parseCronToUi(cron: string): any {
    const defaultState = { modo: 'AVANZADO', hora: '00:00', diasSemana: [], diaMes: 1, avanzado: cron };
    if (!cron) return defaultState;
    const parts = cron.trim().split(/\s+/);
    if (parts.length !== 6) return defaultState;
    
    const [sec, min, hr, dom, mon, dow] = parts;
    if (sec !== '0') return defaultState; 
    if (mon !== '*') return defaultState; 
    
    const hora = `${hr.padStart(2, '0')}:${min.padStart(2, '0')}`;
    
    if (dom === '*' && dow === '?') {
      return { modo: 'DIARIO', hora, diasSemana: [], diaMes: 1, avanzado: cron };
    }
    if (dom === '?' && dow !== '*' && dow !== '?') {
      const days = dow.split(',');
      const valid = ['SUN','MON','TUE','WED','THU','FRI','SAT'];
      if (days.every(d => valid.includes(d))) {
        return { modo: 'SEMANAL', hora, diasSemana: days, diaMes: 1, avanzado: cron };
      }
    }
    if (dow === '?' && dom !== '*' && dom !== '?') {
      const d = parseInt(dom, 10);
      if (!isNaN(d) && d >= 1 && d <= 31 && dom === d.toString()) {
        return { modo: 'MENSUAL', hora, diasSemana: [], diaMes: d, avanzado: cron };
      }
    }
    
    return defaultState;
  }

  generateCronFromUi(ui: any): string {
    if (ui.modo === 'AVANZADO') return ui.avanzado;
    
    const [hr, min] = ui.hora.split(':');
    const hrNum = parseInt(hr, 10) || 0;
    const minNum = parseInt(min, 10) || 0;
    
    if (ui.modo === 'DIARIO') {
      return `0 ${minNum} ${hrNum} * * ?`;
    }
    if (ui.modo === 'SEMANAL') {
      const dow = ui.diasSemana.length > 0 ? ui.diasSemana.join(',') : '*';
      return `0 ${minNum} ${hrNum} ? * ${dow}`;
    }
    if (ui.modo === 'MENSUAL') {
      const dia = ui.diaMes || 1;
      return `0 ${minNum} ${hrNum} ${dia} * ?`;
    }
    return ui.avanzado;
  }

  actualizarUiFull(cambios: Partial<any>): void {
    const newState = { ...this.cronFullUi(), ...cambios };
    this.cronFullUi.set(newState);
    const newCron = this.generateCronFromUi(newState);
    this.onCronFullInput(newCron);
  }

  actualizarUiDiario(cambios: Partial<any>): void {
    const newState = { ...this.cronDiarioUi(), ...cambios };
    this.cronDiarioUi.set(newState);
    const newCron = this.generateCronFromUi(newState);
    this.onCronDiarioInput(newCron);
  }

  toggleDiaSemana(target: 'FULL' | 'DIARIO', dia: string): void {
    const state = target === 'FULL' ? this.cronFullUi() : this.cronDiarioUi();
    let dias = [...state.diasSemana];
    if (dias.includes(dia)) {
      dias = dias.filter(d => d !== dia);
    } else {
      dias.push(dia);
    }
    if (dias.length === 0) dias = ['SUN']; // Prevent empty days
    if (target === 'FULL') {
      this.actualizarUiFull({ diasSemana: dias });
    } else {
      this.actualizarUiDiario({ diasSemana: dias });
    }
  }

  guardarConfiguracion(): void {
    const errorFull = this.validarCronCliente(this.configPolitica().cronFull);
    const errorDiario = this.validarCronCliente(this.configPolitica().cronDiario);
    this.cronErrorFull.set(errorFull);
    this.cronErrorDiario.set(errorDiario);
    if (errorFull || errorDiario) return;

    const peticion = {
      cronFull: this.configPolitica().cronFull,
      retencionDiasFull: this.configPolitica().retencionDiasFull,
      cronDiario: this.configPolitica().cronDiario,
      retencionDiasDiario: this.configPolitica().retencionDiasDiario
    };

    this.guardandoPolitica.set(true);
    this.respaldoService.actualizarPolitica(peticion).subscribe({
      next: () => {
        this.guardandoPolitica.set(false);
        this.toastService.success('Política de respaldos actualizada');
        this.cerrarConfiguracion();
        this.cargarResumen();
      },
      error: (err) => {
        this.guardandoPolitica.set(false);
        this.toastService.error(err.error?.detail || err.error?.message || 'Error al guardar la política');
      }
    });
  }

  formatFecha(fecha: string | null): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  copiarHash(hash: string): void {
    navigator.clipboard.writeText(hash).then(() => {
      this.toastService.success('Hash copiado al portapapeles');
    });
  }
}
