import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { ToastService } from '../../../../core/services/toast.service';
import { CreadorContextoService } from '../../services/creador-contexto.service';
import { ServicioService } from '../../services/servicio.service';
import { CatalogoService } from '../../services/catalogo.service';
import { PerfilRequeridoComponent } from '../../components/perfil-requerido.component';
import { FlujoTrabajoService } from '../../../pedido/services/flujo-trabajo.service';
import { RespuestaFlujoTrabajo, PeticionCrearFlujoTrabajo, PeticionEtapaConfig } from '../../../pedido/models/pedido.model';
import { PlantillaContratoService } from '../../../legal/services/plantilla-contrato.service';
import { RespuestaPlantillaContratoResumen } from '../../../legal/models/legal.model';
import { BriefingService } from '../../../comunicacion/services/briefing.service';
import { MAX_PREGUNTAS_PLANTILLA, PeticionCrearBriefingPlantilla, RespuestaBriefing } from '../../../comunicacion/models/comunicacion.model';
import {
  RespuestaServicio,
  RespuestaCategoria,
  RespuestaSubcategoria,
  RespuestaEtiqueta,
  RespuestaAtributo,
  PeticionCrearServicio,
  PeticionActualizarServicio,
  EstadoPublicacion,
  TipoItem
} from '../../models/creador.model';
import { mensajeError } from '../../utils/formato';

/** Espejo de PoliticaArchivo.PERFIL (backend): solo imagen, sin SVG, 5 MB. */
const TIPOS_MINIATURA_PERMITIDOS = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif'];
const MAX_BYTES_MINIATURA = 5 * 1024 * 1024;

@Component({
  selector: 'app-servicio-form',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, RouterLink, PerfilRequeridoComponent],
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
  private flujoService = inject(FlujoTrabajoService);
  private plantillaContratoService = inject(PlantillaContratoService);
  private briefingService = inject(BriefingService);

  readonly idServicio = signal<number | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly isSaving = signal<boolean>(false);
  readonly error = signal<string>('');

  readonly categorias = signal<RespuestaCategoria[]>([]);
  readonly subcategorias = signal<RespuestaSubcategoria[]>([]);
  readonly etiquetas = signal<RespuestaEtiqueta[]>([]);
  readonly etiquetasElegidas = signal<number[]>([]);
  readonly nuevaEtiqueta = signal<string>('');
  readonly creandoEtiqueta = signal<boolean>(false);

  // Subcategorías del servicio: varias, elegidas con un "+" (no un solo <select>).
  readonly subcategoriasElegidas = signal<number[]>([]);
  subcategoriaParaAgregar: number | null = null;

  // Categoría nueva: pantalla flotante propia, separada del selector de subcategorías.
  readonly mostrarFormCategoria = signal<boolean>(false);
  readonly creandoCategoria = signal<boolean>(false);
  nuevaCategoriaNombre = '';

  // Subcategoría nueva: alta liviana (solo nombre) dentro de este mismo formulario.
  readonly mostrarFormSubcategoria = signal<boolean>(false);
  readonly creandoSubcategoria = signal<boolean>(false);
  categoriaParaNuevaSubcategoria: number | null = null;
  nuevaSubcategoriaNombre = '';

  // Flujo de trabajo: el creador elige uno de los suyos, o crea uno nuevo sin salir del formulario.
  readonly flujos = signal<RespuestaFlujoTrabajo[]>([]);
  readonly mostrarFormFlujo = signal<boolean>(false);
  readonly creandoFlujo = signal<boolean>(false);
  formFlujo: PeticionCrearFlujoTrabajo = { nombreFlujo: '', descripcionFlujo: '', etapas: [] };
  nuevaEtapaFlujo: PeticionEtapaConfig = { nombreEtapa: '', numeroOrden: 1, esEtapaFinal: false, requiereEntregable: false };

  // Plantilla de acuerdo: catálogo curado por ADMIN (REQ-F-017 ampliado) más
  // las plantillas privadas del propio creador (V45). Opcional; sin elegir,
  // el acuerdo del pedido usa la predeterminada del catálogo general.
  readonly plantillasContrato = signal<RespuestaPlantillaContratoResumen[]>([]);
  readonly plantillasContratoGenerales = computed(() => this.plantillasContrato().filter(p => !p.esPropia));
  readonly plantillasContratoPropias = computed(() => this.plantillasContrato().filter(p => p.esPropia));

  // Cuestionario (briefing): entre los propios del creador (REQ-F-016
  // ampliado). Opcional; sin elegir, crear un pedido no pide preguntas.
  // Igual que el flujo de trabajo, se puede crear uno nuevo sin salir de
  // este formulario en vez de ir primero a "Mis Cuestionarios".
  readonly cuestionarios = signal<RespuestaBriefing[]>([]);
  readonly mostrarFormCuestionario = signal<boolean>(false);
  readonly creandoCuestionario = signal<boolean>(false);
  readonly maxPreguntasCuestionario = MAX_PREGUNTAS_PLANTILLA;
  formCuestionario: PeticionCrearBriefingPlantilla = { nombrePlantilla: '', preguntas: [] };
  nuevaPreguntaCuestionario = '';

  // Miniatura: se sube el archivo y el campo del form solo guarda la URL resultante.
  // La vista previa vive en un signal aparte (no en el valor del form) porque
  // esta app es zoneless: un patchValue() dentro de un subscribe no dispara
  // detección de cambios por sí solo, así que un <img [src]> atado
  // directamente al form se quedaría con la miniatura vieja hasta el próximo
  // evento de plantilla que sí la dispare.
  readonly previewMiniatura = signal<string>('');
  readonly subiendoMiniatura = signal<boolean>(false);

  // Atributos: solo disponibles al editar, porque cuelgan de un servicio existente.
  readonly atributos = signal<RespuestaAtributo[]>([]);
  readonly atributoEnEdicion = signal<number | null>(null);
  readonly guardandoAtributo = signal<boolean>(false);

  readonly perfilFaltante = this.contexto.perfilFaltante;

  readonly esEdicion = computed(() => this.idServicio() !== null);
  readonly tiposItem: TipoItem[] = ['SERVICIO', 'PRODUCTO'];
  readonly estados: EstadoPublicacion[] = ['BORRADOR', 'ACTIVO', 'PAUSADO'];
  readonly tiposDato = ['TEXTO', 'NUMERO', 'BOOLEANO', 'FECHA'];

  // ── Formulario por secciones ──
  // El form reactivo sigue siendo uno solo (una sola llamada al backend al
  // guardar); esto solo controla qué sección se muestra.
  readonly pasos = [
    { titulo: 'Información básica', hint: 'Título, descripción y precio' },
    { titulo: 'Categoría y etiquetas', hint: 'Dónde aparecerá en el catálogo' },
    { titulo: 'Flujo y contrato', hint: 'Etapas, contrato y cuestionario' },
    { titulo: 'Revisiones y estado', hint: 'Ajustes finales' }
  ];
  private readonly camposPorPaso: string[][] = [
    ['tituloServicio', 'descripcionDetallada', 'precioBase', 'tipoItem', 'urlMiniatura'],
    [],
    ['idFlujo', 'idPlantillaContrato', 'idBriefingPlantilla'],
    ['limiteRevisionesBase', 'cargoRevisionAdicional', 'estadoPublicacion']
  ];
  readonly pasoActual = signal(0);
  readonly esPrimerPaso = computed(() => this.pasoActual() === 0);
  readonly esUltimoPaso = computed(() => this.pasoActual() === this.pasos.length - 1);

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

  /** Subcategorías ya elegidas, con su nombre, para pintar los chips. */
  chipsSubcategorias = computed(() => {
    const elegidas = this.subcategoriasElegidas();
    return this.subcategorias().filter(s => elegidas.includes(s.idSubcategoria));
  });

  form: FormGroup = this.fb.group({
    tituloServicio: ['', [Validators.required, Validators.maxLength(150)]],
    descripcionDetallada: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(2000)]],
    precioBase: [null as number | null, [Validators.required, Validators.min(0.01)]],
    tipoItem: ['SERVICIO' as TipoItem, [Validators.required]],
    estadoPublicacion: ['BORRADOR' as EstadoPublicacion, [Validators.required]],
    urlMiniatura: ['', [Validators.maxLength(255)]],
    cargoRevisionAdicional: [null as number | null, [Validators.min(0)]],
    limiteRevisionesBase: [null as number | null, [Validators.min(0)]],
    idFlujo: [null as number | null],
    idPlantillaContrato: [null as number | null],
    idBriefingPlantilla: [null as number | null]
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
      categorias: this.catalogoService.listarCategorias().pipe(catchError(() => of([] as RespuestaCategoria[]))),
      subcategorias: this.catalogoService.listarSubcategorias().pipe(catchError(() => of([] as RespuestaSubcategoria[]))),
      etiquetas: this.catalogoService.listarEtiquetas().pipe(catchError(() => of([] as RespuestaEtiqueta[]))),
      flujos: this.flujoService.listarFlujos().pipe(catchError(() => of([] as RespuestaFlujoTrabajo[]))),
      plantillasContrato: this.plantillaContratoService.listarActivas().pipe(catchError(() => of([] as RespuestaPlantillaContratoResumen[]))),
      cuestionarios: this.briefingService.listarMisPlantillas().pipe(catchError(() => of([] as RespuestaBriefing[])))
    }).subscribe({
      next: ({ categorias, subcategorias, etiquetas, flujos, plantillasContrato, cuestionarios }) => {
        this.categorias.set(categorias);
        this.subcategorias.set(subcategorias);
        this.etiquetas.set(etiquetas);
        this.flujos.set(flujos);
        this.plantillasContrato.set(plantillasContrato);
        this.cuestionarios.set(cuestionarios);

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
          tipoItem: servicio.tipoItem,
          estadoPublicacion: servicio.estadoPublicacion,
          urlMiniatura: servicio.urlMiniatura || '',
          cargoRevisionAdicional: servicio.cargoRevisionAdicional,
          limiteRevisionesBase: servicio.limiteRevisionesBase,
          idFlujo: servicio.idFlujo,
          idPlantillaContrato: servicio.idPlantillaContrato,
          idBriefingPlantilla: servicio.idBriefingPlantilla
        });
        this.previewMiniatura.set(servicio.urlMiniatura || '');
        this.subcategoriasElegidas.set((servicio.subcategorias || []).map(s => s.idSubcategoria));
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

  // ── Subcategorías del servicio ──

  agregarSubcategoria(): void {
    const id = this.subcategoriaParaAgregar;
    if (id === null) return;
    if (!this.subcategoriasElegidas().includes(id)) {
      this.subcategoriasElegidas.update(lista => [...lista, id]);
    }
    this.subcategoriaParaAgregar = null;
  }

  quitarSubcategoriaElegida(id: number): void {
    this.subcategoriasElegidas.update(lista => lista.filter(i => i !== id));
  }

  // ── Categoría nueva (pantalla flotante propia) ──

  abrirFormCategoria(): void {
    this.nuevaCategoriaNombre = '';
    this.mostrarFormCategoria.set(true);
  }

  cancelarFormCategoria(): void {
    this.mostrarFormCategoria.set(false);
  }

  guardarCategoria(): void {
    const nombre = this.nuevaCategoriaNombre.trim();
    if (!nombre) {
      this.toast.error('Indica el nombre de la categoría');
      return;
    }

    this.creandoCategoria.set(true);
    this.catalogoService.crearCategoria(nombre).subscribe({
      next: (categoria) => {
        this.categorias.update(lista => [...lista, categoria]);
        this.creandoCategoria.set(false);
        this.mostrarFormCategoria.set(false);
        this.toast.success(`Categoría «${categoria.nombreCategoria}» creada — un moderador la revisará pronto`);
      },
      error: (err) => {
        this.creandoCategoria.set(false);
        this.toast.error(mensajeError(err, 'No se pudo crear la categoría'));
      }
    });
  }

  // ── Subcategoría nueva (alta liviana, dentro del mismo formulario) ──

  abrirFormSubcategoria(): void {
    this.categoriaParaNuevaSubcategoria = this.categorias()[0]?.idCategoria ?? null;
    this.nuevaSubcategoriaNombre = '';
    this.mostrarFormSubcategoria.set(true);
  }

  cancelarFormSubcategoria(): void {
    this.mostrarFormSubcategoria.set(false);
  }

  guardarSubcategoria(): void {
    const idCategoria = this.categoriaParaNuevaSubcategoria;
    const nombre = this.nuevaSubcategoriaNombre.trim();
    if (!idCategoria || !nombre) {
      this.toast.error('Elige una categoría e indica el nombre de la subcategoría');
      return;
    }

    this.creandoSubcategoria.set(true);
    this.catalogoService.crearSubcategoria(idCategoria, nombre).subscribe({
      next: (subcategoria) => {
        this.subcategorias.update(lista => [...lista, subcategoria]);
        this.subcategoriasElegidas.update(lista => [...lista, subcategoria.idSubcategoria]);
        this.creandoSubcategoria.set(false);
        this.mostrarFormSubcategoria.set(false);
        this.toast.success(`Subcategoría «${subcategoria.nombreSubcategoria}» creada — un moderador la revisará pronto`);
      },
      error: (err) => {
        this.creandoSubcategoria.set(false);
        this.toast.error(mensajeError(err, 'No se pudo crear la subcategoría'));
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

  // ── Miniatura ──

  onMiniaturaSeleccionada(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!TIPOS_MINIATURA_PERMITIDOS.includes(file.type)) {
      this.toast.error(`Formato no soportado: ${file.type || 'desconocido'}. Se acepta JPG, PNG, WEBP o GIF.`);
      input.value = '';
      return;
    }
    if (file.size > MAX_BYTES_MINIATURA) {
      this.toast.error('La imagen supera el máximo de 5 MB.');
      input.value = '';
      return;
    }

    this.subiendoMiniatura.set(true);
    this.servicioService.subirMiniatura(file).subscribe({
      next: (resp) => {
        this.form.patchValue({ urlMiniatura: resp.url });
        this.previewMiniatura.set(resp.url);
        this.subiendoMiniatura.set(false);
      },
      error: (err) => {
        this.subiendoMiniatura.set(false);
        this.toast.error(mensajeError(err, 'No se pudo subir la miniatura'));
      }
    });
    input.value = '';
  }

  quitarMiniatura(): void {
    this.form.patchValue({ urlMiniatura: '' });
    this.previewMiniatura.set('');
  }

  // ── Flujo de trabajo ──

  abrirFormFlujo(): void {
    this.formFlujo = { nombreFlujo: '', descripcionFlujo: '', etapas: [] };
    this.nuevaEtapaFlujo = { nombreEtapa: '', numeroOrden: 1, esEtapaFinal: false, requiereEntregable: false };
    this.mostrarFormFlujo.set(true);
  }

  cancelarFormFlujo(): void {
    this.mostrarFormFlujo.set(false);
  }

  agregarEtapaFlujo(): void {
    const nombre = this.nuevaEtapaFlujo.nombreEtapa.trim();
    if (!nombre) return;

    const yaExiste = this.formFlujo.etapas.some(
      e => e.nombreEtapa.trim().toLowerCase() === nombre.toLowerCase());
    if (yaExiste) {
      this.toast.warning(`Ya agregaste una etapa llamada «${nombre}».`);
      return;
    }

    this.formFlujo.etapas.push({ ...this.nuevaEtapaFlujo, nombreEtapa: nombre });
    this.nuevaEtapaFlujo = { nombreEtapa: '', numeroOrden: this.formFlujo.etapas.length + 1, esEtapaFinal: false, requiereEntregable: false };
  }

  quitarEtapaFlujo(index: number): void {
    this.formFlujo.etapas.splice(index, 1);
    this.formFlujo.etapas.forEach((e, i) => e.numeroOrden = i + 1);
  }

  guardarFlujo(): void {
    if (!this.formFlujo.nombreFlujo.trim() || this.formFlujo.etapas.length === 0) {
      this.toast.error('El flujo necesita un nombre y al menos una etapa.');
      return;
    }

    this.creandoFlujo.set(true);
    this.flujoService.crearFlujo(this.formFlujo).subscribe({
      next: (flujo) => {
        this.flujos.update(lista => [...lista, flujo]);
        this.form.patchValue({ idFlujo: flujo.idFlujo });
        this.creandoFlujo.set(false);
        this.mostrarFormFlujo.set(false);
        // Mismo aviso que en guardarCuestionario(): queda elegido en el
        // <select>, pero la asignación se persiste recién al guardar este
        // formulario del servicio.
        this.toast.success(`Flujo «${flujo.nombreFlujo}» creado y seleccionado — no olvides guardar el servicio para aplicarlo`);
      },
      error: (err) => {
        this.creandoFlujo.set(false);
        this.toast.error(mensajeError(err, 'No se pudo crear el flujo'));
      }
    });
  }

  // ── Cuestionario (briefing): crear uno nuevo sin salir de este formulario ──

  abrirFormCuestionario(): void {
    this.formCuestionario = { nombrePlantilla: '', preguntas: [] };
    this.nuevaPreguntaCuestionario = '';
    this.mostrarFormCuestionario.set(true);
  }

  cancelarFormCuestionario(): void {
    this.mostrarFormCuestionario.set(false);
  }

  agregarPreguntaCuestionario(): void {
    const texto = this.nuevaPreguntaCuestionario.trim();
    if (!texto) return;

    if (this.formCuestionario.preguntas.length >= this.maxPreguntasCuestionario) {
      this.toast.warning(`Un cuestionario admite como máximo ${this.maxPreguntasCuestionario} preguntas.`);
      return;
    }

    this.formCuestionario.preguntas.push({
      textoPregunta: texto,
      numeroOrden: this.formCuestionario.preguntas.length + 1
    });
    this.nuevaPreguntaCuestionario = '';
  }

  quitarPreguntaCuestionario(index: number): void {
    this.formCuestionario.preguntas.splice(index, 1);
    this.formCuestionario.preguntas.forEach((p, i) => p.numeroOrden = i + 1);
  }

  guardarCuestionario(): void {
    if (!this.formCuestionario.nombrePlantilla.trim() || this.formCuestionario.preguntas.length === 0) {
      this.toast.error('El cuestionario necesita un nombre y al menos una pregunta.');
      return;
    }

    this.creandoCuestionario.set(true);
    this.briefingService.crearPlantilla(this.formCuestionario).subscribe({
      next: (plantilla) => {
        this.cuestionarios.update(lista => [...lista, plantilla]);
        this.form.patchValue({ idBriefingPlantilla: plantilla.idPlantilla });
        this.creandoCuestionario.set(false);
        this.mostrarFormCuestionario.set(false);
        // El cuestionario ya existe y quedó elegido en el <select>, pero la
        // asignación al servicio recién se persiste cuando se guarda ESTE
        // formulario — sin este aviso, un creador puede leer "creado" como
        // "ya quedó aplicado" y salir sin guardar (regresión real reportada:
        // el cuestionario se creó una hora después del último guardado del
        // servicio y nunca llegó a asociarse).
        this.toast.success(`Cuestionario «${plantilla.nombrePlantilla}» creado y seleccionado — no olvides guardar el servicio para aplicarlo`);
      },
      error: (err) => {
        this.creandoCuestionario.set(false);
        this.toast.error(mensajeError(err, 'No se pudo crear el cuestionario'));
      }
    });
  }

  // ── Navegación entre secciones ──

  private pasoValido(paso: number): boolean {
    const campos = this.camposPorPaso[paso];
    const camposOk = campos.every(c => this.form.get(c)?.valid ?? true);
    return paso === 1 ? camposOk && this.subcategoriasElegidas().length > 0 : camposOk;
  }

  private marcarPasoTocado(paso: number): void {
    this.camposPorPaso[paso].forEach(c => this.form.get(c)?.markAsTouched());
    if (paso === 1 && this.subcategoriasElegidas().length === 0) {
      this.toast.error('Elige al menos una subcategoría antes de continuar');
    }
  }

  irAPaso(paso: number): void {
    this.pasoActual.set(paso);
  }

  pasoAnterior(): void {
    this.pasoActual.update(p => Math.max(0, p - 1));
  }

  pasoSiguiente(): void {
    if (!this.pasoValido(this.pasoActual())) {
      this.marcarPasoTocado(this.pasoActual());
      return;
    }
    this.pasoActual.update(p => Math.min(this.pasos.length - 1, p + 1));
  }

  // ── Guardado del servicio ──

  invalido(campo: string): boolean {
    const control = this.form.get(campo);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  guardar(): void {
    const pasoInvalido = this.pasos.findIndex((_, i) => !this.pasoValido(i));
    if (pasoInvalido !== -1) {
      this.form.markAllAsTouched();
      this.pasoActual.set(pasoInvalido);
      this.marcarPasoTocado(pasoInvalido);
      return;
    }
    const perfil = this.contexto.perfil();
    if (!perfil) return;

    const val = this.form.getRawValue();
    const base: PeticionCrearServicio = {
      tituloServicio: val.tituloServicio!,
      descripcionDetallada: val.descripcionDetallada!,
      precioBase: Number(val.precioBase),
      idsSubcategoria: this.subcategoriasElegidas(),
      tipoItem: val.tipoItem as TipoItem,
      urlMiniatura: val.urlMiniatura || null,
      cargoRevisionAdicional: val.cargoRevisionAdicional !== null ? Number(val.cargoRevisionAdicional) : null,
      limiteRevisionesBase: val.limiteRevisionesBase !== null ? Number(val.limiteRevisionesBase) : null,
      idFlujo: val.idFlujo !== null ? Number(val.idFlujo) : null,
      idPlantillaContrato: val.idPlantillaContrato !== null ? Number(val.idPlantillaContrato) : null,
      idBriefingPlantilla: val.idBriefingPlantilla !== null ? Number(val.idBriefingPlantilla) : null,
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
