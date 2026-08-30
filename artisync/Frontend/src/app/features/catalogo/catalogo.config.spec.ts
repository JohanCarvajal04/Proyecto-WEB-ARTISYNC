import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { ExplorarComponent } from './pages/explorar/explorar.component';
import { CatalogoPublicoService } from './services/catalogo-publico.service';
import { CATALOGO_BASE_PATH } from './catalogo.config';
import { paginaVacia } from '../../shared/models/pagina.model';

/** Doble mínimo: lo único que ExplorarComponent consulta en ngOnInit. */
function catalogoFalso() {
  return {
    listarCategorias: vi.fn(() => of([])),
    listarEtiquetas: vi.fn(() => of([])),
    buscarCatalogo: vi.fn(() => of(paginaVacia()))
  };
}

/** No se llama detectChanges() en estos casos, pero ActivatedRoute igual debe
 *  resolver en la construcción del componente. */
const rutaFalsa = { snapshot: { queryParamMap: { get: () => null } } };

describe('CATALOGO_BASE_PATH', () => {
  it('por defecto apunta al catálogo público (/explorar)', () => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        { provide: CatalogoPublicoService, useValue: catalogoFalso() },
        { provide: HttpClient, useValue: {} },
        { provide: ActivatedRoute, useValue: rutaFalsa }
      ]
    });

    expect(TestBed.inject(CATALOGO_BASE_PATH)).toBe('/explorar');
  });

  it('el montaje autenticado provee su propio prefijo (/dashboard/explorar)', () => {
    // Reproduce lo que dashboard.routes.ts hace con `providers` a nivel de
    // ruta: el mismo componente debe generar enlaces internos distintos según
    // quién lo monte, sin que el componente sepa cuál de los dos es.
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        { provide: CatalogoPublicoService, useValue: catalogoFalso() },
        { provide: HttpClient, useValue: {} },
        { provide: ActivatedRoute, useValue: rutaFalsa },
        { provide: CATALOGO_BASE_PATH, useValue: '/dashboard/explorar' }
      ]
    });

    const fixture = TestBed.createComponent(ExplorarComponent);
    expect(fixture.componentInstance.base).toBe('/dashboard/explorar');
  });
});
