import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { ToastService } from '../../../../core/services/toast.service';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { ServicioService } from '../../services/servicio.service';
import { CatalogoService } from '../../services/catalogo.service';
import { PerfilRequeridoComponent } from '../../components/perfil-requerido.component';
import {
  RespuestaServicio,
  RespuestaSubcategoria,
  RespuestaEtiqueta,
  RespuestaAtributo,
  PeticionCrearServicio,
  PeticionActualizarServicio,
  EstadoPublicacion,
  TipoItem
} from '../../models/creador.model';
import { mensajeError } from '../../utils/formato';

@Component({
  selector: 'app-servicio-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, PerfilRequeridoComponent],
  templateUrl: './servicio-form.component.html',
  styleUrl: './servicio-form.component.css'
})
export class ServicioFormComponent implements OnInit {

  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private servicioService = inject(ServicioService);
  private catalogoService = inject(CatalogoService);
  private contexto = inject(CreadorContextoService);
  private toast = inject(ToastService);

  readonly idServicio = signal<number | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly isSaving = signal<boolean>(false);
  readonly error = signal<string>('');

  readonly subcategorias = signal<RespuestaSubcategoria[]>([]);
  readonly etiquetas = signal<RespuestaEtiqueta[]>([]);
  readonly etiquetasElegidas = signal<number[]>([]);
  readonly nuevaEtiqueta = signal<string>('');
  readonly creandoEtiqueta = signal<boolean>(false);

  // Atributos: solo disponibles al editar, porque cuelgan de un servicio existente.
  readonly atributos = signal<RespuestaAtributo[]>([]);
  readonly atributoEnEdicion = signal<number | null>(null);
  readonly guardandoAtributo = signal<boolean>(false);

  readonly perfilFaltante = this.contexto.perfilFaltante;

  readonly esEdicion = computed(() => this.idServicio() !== null);
  readonly tiposItem: TipoItem[] = ['SERVICIO', 'PRODUCTO'];
  readonly estados: EstadoPublicacion[] = ['BORRADOR', 'ACTIVO', 'PAUSADO'];
  readonly tiposDato = ['TEXTO', 'NUMERO', 'BOOLEANO', 'FECHA'];

  /** Subcategorías agrupadas por categoría para el `<optgroup>` del selector. */
  subcategoriasAgrupadas = computed(() => {
    const grupos = new Map<string, RespuestaSubcategoria[]>();
    for (const sub of this.subcategorias()) {
      const lista = grupos.get(sub.nombreCategoria) || [];
      lista.push(sub);
      grupos.set(sub.nombreCategoria, lista);
    }
    return Array.from(grupos, ([categoria, items]) => ({ categoria, items }));
  });

  form: FormGroup = this.fb.group({
    tituloServicio: ['', [Validators.required, Validators.maxLength(150)]],
    descripcionDetallada: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(2000)]],
    precioBase: [null as number | null, [Validators.required, Validators.min(0.01)]],
    idSubcategoria: [null as number | null, [Validators.required]],
    tipoItem: ['SERVICIO' as TipoItem, [Validators.required]],
    estadoPublicacion: ['BORRADOR' as EstadoPublicacion, [Validators.required]],
    urlMiniatura: ['', [Validators.maxLength(255)]],
    cargoRevisionAdicional: [null as number | null, [Validators.min(0)]],
    limiteRevisionesBase: [null as number | null, [Validators.min(0)]]
  });

  formAtributo: FormGroup = this.fb.group({
    nombreAtributo: ['', [Validators.required, Validators.maxLength(100)]],
    valorAsignado: ['', [Validators.required, Validators.maxLength(255)]],
    tipoDato: ['TEXTO', [Validators.required, Validators.maxLength(50)]]
  });

  descripcionLength = computed(() => (this.form.get('descripcionDetallada')?.value || '').length);

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.idServicio.set(idParam ? Number(idParam) : null);

    this.contexto.obtenerPerfil().subscribe({
      next: (perfil) => {
        if (!perfil) {
          this.isLoading.set(false);
          return;
        }
        this.cargarCatalogos();
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'No se pudo cargar tu perfil de creador'));
        this.isLoading.set(false);
      }
    });
  }

  private cargarCatalogos(): void {
    forkJoin({
      subcategorias: this.catalogoService.listarSubcategorias().pipe(catchError(() => of([] as RespuestaSubcategoria[]))),
      etiquetas: this.catalogoService.listarEtiquetas().pipe(catchError(() => of([] as RespuestaEtiqueta[])))
    }).subscribe({
      next: ({ subcategorias, etiquetas }) => {
        this.subcategorias.set(subcategorias);
        this.etiquetas.set(etiquetas);

        const id = this.idServicio();
        if (id) {
          this.cargarServicio(id);
        } else {
          this.isLoading.set(false);
        }
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'Error al cargar categorías y etiquetas'));
        this.isLoading.set(false);
      }
    });
  }

  private cargarServicio(id: number): void {
    this.servicioService.obtenerPorId(id).subscribe({
      next: (servicio: RespuestaServicio) => {
        this.form.patchValue({
          tituloServicio: servicio.tituloServicio,
          descripcionDetallada: servicio.descripcionDetallada,
          precioBase: servicio.precioBase,
          idSubcategoria: servicio.idSubcategoria,
          tipoItem: servicio.tipoItem,
          estadoPublicacion: servicio.estadoPublicacion,
          urlMiniatura: servicio.urlMiniatura || '',
          cargoRevisionAdicional: servicio.cargoRevisionAdicional,
          limiteRevisionesBase: servicio.limiteRevisionesBase
        });
        this.etiquetasElegidas.set((servicio.etiquetas || []).map(e => e.idEtiqueta));
        this.atributos.set(servicio.atributos || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(mensajeError(err, 'No se pudo cargar el servicio'));
        this.isLoading.set(false);
      }
    });
  }

  // ── Etiquetas ──

  alternarEtiqueta(idEtiqueta: number): void {
    this.etiquetasElegidas.update(actuales =>
      actuales.includes(idEtiqueta)
        ? actuales.filter(id => id !== idEtiqueta)
        : [...actuales, idEtiqueta]
    );
  }

  etiquetaActiva(idEtiqueta: number): boolean {
    return this.etiquetasElegidas().includes(idEtiqueta);
  }

  onNuevaEtiqueta(evento: Event): void {
    this.nuevaEtiqueta.set((evento.target as HTMLInputElement).value);
  }

  /** Alta rápida sin salir del formulario; queda seleccionada al crearse. */
  crearEtiqueta(): void {
    const nombre = this.nuevaEtiqueta().trim();
    if (!nombre || this.creandoEtiqueta()) return;

    const yaExiste = this.etiquetas()
      .some(e => e.nombreEtiqueta.toLowerCase() === nombre.toLowerCase());
    if (yaExiste) {
      this.toast.warning('Esa etiqueta ya existe en el catálogo.');
      return;
    }

    this.creandoEtiqueta.set(true);
    this.catalogoService.crearEtiqueta(nombre).subscribe({
      next: (etiqueta) => {
        this.etiquetas.update(lista => [...lista, etiqueta]);
        this.etiquetasElegidas.update(ids => [...ids, etiqueta.idEtiqueta]);
        this.nuevaEtiqueta.set('');
        this.creandoEtiqueta.set(false);
        this.toast.success(`Etiqueta «${etiqueta.nombreEtiqueta}» creada`);
      },
      error: (err) => {
        this.creandoEtiqueta.set(false);
        this.toast.error(mensajeError(err, 'No se pudo crear la etiqueta'));
      }
    });
  }

  // ── Guardado del servicio ──

  invalido(campo: string): boolean {
    const control = this.form.get(campo);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const perfil = this.contexto.perfil();
    if (!perfil) return;

    const val = this.form.getRawValue();
    const base: PeticionCrearServicio = {
      tituloServicio: val.tituloServicio!,
      descripcionDetallada: val.descripcionDetallada!,
      precioBase: Number(val.precioBase),
      idSubcategoria: Number(val.idSubcategoria),
      tipoItem: val.tipoItem as TipoItem,
      urlMiniatura: val.urlMiniatura || null,
      cargoRevisionAdicional: val.cargoRevisionAdicional !== null ? Number(val.cargoRevisionAdicional) : null,
      limiteRevisionesBase: val.limiteRevisionesBase !== null ? Number(val.limiteRevisionesBase) : null,
      etiquetaIds: this.etiquetasElegidas()
    };

    this.isSaving.set(true);
    this.error.set('');

    const id = this.idServicio();
    if (id) {
      const peticion: PeticionActualizarServicio = { ...base, estadoPublicacion: val.estadoPublicacion as EstadoPublicacion };
      this.servicioService.actualizar(id, peticion).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.toast.success('Servicio actualizado');
          this.router.navigate(['/creador/servicios']);
        },
        error: (err) => {
          this.isSaving.set(false);
          this.error.set(mensajeError(err, 'No se pudo actualizar el servicio'));
        }
      });
    } else {
      this.servicioService.crear(perfil.idPerfil, base).subscribe({
        next: (servicio) => {
          this.isSaving.set(false);
          this.toast.success('Servicio creado. Ahora puedes añadirle atributos.');
          this.router.navigate(['/creador/servicios', servicio.idServicio, 'editar']);
        },
        error: (err) => {
          this.isSaving.set(false);
          this.error.set(mensajeError(err, 'No se pudo crear el servicio'));
        }
      });
    }
  }

  // ── Atributos del servicio ──

  editarAtributo(atributo: RespuestaAtributo): void {
    this.atributoEnEdicion.set(atributo.idAtributo);
    this.formAtributo.setValue({
      nombreAtributo: atributo.nombreAtributo,
      valorAsignado: atributo.valorAsignado,
      tipoDato: atributo.tipoDato
    });
  }

  cancelarAtributo(): void {
    this.atributoEnEdicion.set(null);
    this.formAtributo.reset({ nombreAtributo: '', valorAsignado: '', tipoDato: 'TEXTO' });
  }

  guardarAtributo(): void {
    const id = this.idServicio();
    if (!id || this.formAtributo.invalid) {
      this.formAtributo.markAllAsTouched();
      return;
    }

    const peticion = this.formAtributo.getRawValue() as { nombreAtributo: string; valorAsignado: string; tipoDato: string };
    this.guardandoAtributo.set(true);

    const enEdicion = this.atributoEnEdicion();
    const peticion$ = enEdicion !== null
      ? this.servicioService.actualizarAtributo(id, enEdicion, peticion)
      : this.servicioService.agregarAtributo(id, peticion);

    peticion$.subscribe({
      next: (atributo) => {
        this.atributos.update(lista =>
          enEdicion !== null
            ? lista.map(a => a.idAtributo === enEdicion ? atributo : a)
            : [...lista, atributo]
        );
        this.guardandoAtributo.set(false);
        this.cancelarAtributo();
        this.toast.success(enEdicion !== null ? 'Atributo actualizado' : 'Atributo añadido');
      },
      error: (err) => {
        this.guardandoAtributo.set(false);
        this.toast.error(mensajeError(err, 'No se pudo guardar el atributo'));
      }
    });
  }

  eliminarAtributo(atributo: RespuestaAtributo): void {
    const id = this.idServicio();
    if (!id) return;
    this.servicioService.eliminarAtributo(id, atributo.idAtributo).subscribe({
      next: () => {
        this.atributos.update(lista => lista.filter(a => a.idAtributo !== atributo.idAtributo));
        this.toast.success('Atributo eliminado');
      },
      error: (err) => this.toast.error(mensajeError(err, 'No se pudo eliminar el atributo'))
    });
  }
}
