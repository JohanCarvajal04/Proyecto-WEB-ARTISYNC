import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';

import { AuditoriaComponent } from './auditoria.component';
import { AuthService } from '../../../seguridad/services/auth.service';
import { environment } from '../../../../../environments/environment';

const API = `${environment.apiUrl}/v1/admin/auditoria`;

/** Doble de AuthService con solo lo que *appHasPermission consulta. */
function authFalso() {
  return {
    hasAnyPermission: () => true,
    userRoles: () => ['ROLE_ADMIN']
  };
}

describe('AuditoriaComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditoriaComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authFalso() }
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('carga el catálogo de acciones y la primera página al iniciar', async () => {
    const fixture = TestBed.createComponent(AuditoriaComponent);
    fixture.detectChanges();

    httpMock.expectOne(`${API}/acciones`).flush(['PAIS_CREAR']);
    const reqListado = httpMock.expectOne(r => r.url === API);
    expect(reqListado.request.params.get('page')).toBe('0');
    reqListado.flush({
      content: [{ idEventoAuditoria: 1, correoActor: 'admin@artisync.dev', accionAuditoria: 'PAIS_CREAR', resultadoEvento: 'EXITO' }],
      number: 0, size: 20, totalElements: 1, totalPages: 1, last: true
    });
    await fixture.whenStable();

    expect(fixture.componentInstance.pagina().contenido).toHaveLength(1);
    expect(fixture.componentInstance.isLoading()).toBe(false);
  });

  it('aplicarFiltros() vuelve a pedir la página 0 con el filtro activo', async () => {
    const fixture = TestBed.createComponent(AuditoriaComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${API}/acciones`).flush([]);
    httpMock.expectOne(r => r.url === API).flush({ content: [], number: 0, size: 20, totalElements: 0, totalPages: 0, last: true });
    await fixture.whenStable();

    fixture.componentInstance.actualizarFiltro('modulo', 'FINANZAS');
    fixture.componentInstance.aplicarFiltros();

    const req = httpMock.expectOne(r => r.url === API);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('modulo')).toBe('FINANZAS');
    req.flush({ content: [], number: 0, size: 20, totalElements: 0, totalPages: 0, last: true });
  });

  it('verDetalle() pide el detalle completo del evento al servidor', async () => {
    const fixture = TestBed.createComponent(AuditoriaComponent);
    fixture.detectChanges();
    httpMock.expectOne(`${API}/acciones`).flush([]);
    httpMock.expectOne(r => r.url === API).flush({
      content: [{ idEventoAuditoria: 7, correoActor: 'a@a.dev', accionAuditoria: 'X', resultadoEvento: 'EXITO' }],
      number: 0, size: 20, totalElements: 1, totalPages: 1, last: true
    });
    await fixture.whenStable();

    fixture.componentInstance.verDetalle(fixture.componentInstance.pagina().contenido[0]);

    const req = httpMock.expectOne(`${API}/7`);
    req.flush({ idEventoAuditoria: 7, detalleCambio: { nombrePais: 'Ecuador' } });
    await fixture.whenStable();

    expect(fixture.componentInstance.detalle()?.detalleCambio).toEqual({ nombrePais: 'Ecuador' });
  });
});
