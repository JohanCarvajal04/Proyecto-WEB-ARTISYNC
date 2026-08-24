import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';

import { AuditoriaService } from './auditoria.service';
import { environment } from '../../../../environments/environment';

const API = `${environment.apiUrl}/v1/admin/auditoria`;

describe('AuditoriaService', () => {
  let service: AuditoriaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuditoriaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listar() no envía en la URL los filtros vacíos o indefinidos', () => {
    service.listar({ correoActor: '', accion: undefined, modulo: 'SEGURIDAD' }, 0, 20).subscribe();

    const req = httpMock.expectOne(r => r.url === API);
    expect(req.request.params.has('correoActor')).toBe(false);
    expect(req.request.params.has('accion')).toBe(false);
    expect(req.request.params.get('modulo')).toBe('SEGURIDAD');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');

    req.flush({ content: [], number: 0, size: 20, totalElements: 0, totalPages: 0, last: true });
  });

  it('listar() normaliza la forma PagedResponse (number/pageSize) del backend', () => {
    let recibido: unknown;
    service.listar({}, 1, 10).subscribe(p => (recibido = p));

    const req = httpMock.expectOne(r => r.url === API);
    req.flush({
      content: [{ idEventoAuditoria: 1, accionAuditoria: 'PAIS_CREAR' }],
      number: 1, pageSize: 10, totalElements: 11, totalPages: 2, last: false
    });

    expect(recibido).toEqual({
      contenido: [{ idEventoAuditoria: 1, accionAuditoria: 'PAIS_CREAR' }],
      numero: 1, tamano: 10, totalElementos: 11, totalPaginas: 2, ultima: false
    });
  });

  it('obtener() pide el detalle de un evento por id', () => {
    service.obtener(42).subscribe();

    const req = httpMock.expectOne(`${API}/42`);
    expect(req.request.method).toBe('GET');
    req.flush({ idEventoAuditoria: 42 });
  });

  it('listarAcciones() pide el catálogo de acciones distintas', () => {
    service.listarAcciones().subscribe();

    const req = httpMock.expectOne(`${API}/acciones`);
    expect(req.request.method).toBe('GET');
    req.flush(['PAIS_CREAR', 'USUARIO_CREAR']);
  });

  it('exportarCsv() pide un blob y no un JSON', () => {
    service.exportarCsv({ modulo: 'FINANZAS' }).subscribe();

    const req = httpMock.expectOne(r => r.url === `${API}/csv`);
    expect(req.request.responseType).toBe('blob');
    expect(req.request.params.get('modulo')).toBe('FINANZAS');

    req.flush(new Blob(['x']));
  });
});
