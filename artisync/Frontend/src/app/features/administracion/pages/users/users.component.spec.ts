import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';

import { UsersComponent } from './users.component';
import { AuthService } from '../../../seguridad/services/auth.service';
import { environment } from '../../../../../environments/environment';

const API_USUARIOS = `${environment.apiUrl}/v1/admin/usuarios`;
const API_ROLES = `${environment.apiUrl}/v1/admin/role-permissions/roles`;

/** Doble de AuthService con solo lo que *appHasPermission consulta. */
function authFalso() {
  return {
    hasAnyPermission: () => true,
    userRoles: () => ['ROLE_ADMIN']
  };
}

function pagina(content: unknown[] = [], extra: Record<string, unknown> = {}) {
  return { content, number: 0, size: 10, totalElements: content.length, totalPages: 3, last: false, ...extra };
}

const UN_USUARIO = [
  { idUsuario: 1, nombres: 'Ana', apellidos: 'García', correo: 'ana@test.dev', estadoCuenta: true, roles: ['ROLE_ADMIN'] }
];

describe('UsersComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsersComponent],
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

  /** Arranca el componente y responde a las dos peticiones de inicio. */
  function iniciar() {
    const fixture = TestBed.createComponent(UsersComponent);
    fixture.detectChanges();

    httpMock.expectOne(API_ROLES)
      .flush([{ idRol: 1, nombreRol: 'ROLE_ADMIN', descripcionRol: '', permisos: [] }]);
    httpMock.expectOne(r => r.url === API_USUARIOS).flush(pagina(UN_USUARIO));

    return fixture;
  }

  it('carga el listado de usuarios y el catálogo de roles al iniciar', () => {
    // Regresión: takeUntilDestroyed() dentro de ngOnInit lanzaba NG0203 y
    // abortaba el hook antes de loadUsers()/loadRolesFiltro() — la tabla
    // quedaba vacía ("Prueba ajustando los filtros") y el <select> de roles
    // sin opciones.
    const fixture = iniciar();

    expect(fixture.componentInstance.users()).toHaveLength(1);
    expect(fixture.componentInstance.rolesFiltro()).toHaveLength(1);
  });

  it('cambiar los filtros NO dispara ninguna petición hasta pulsar Filtrar', () => {
    const fixture = iniciar();
    const componente = fixture.componentInstance;

    componente.searchTerm = 'ana';
    componente.selectedRoleFilter = 'ADMIN';
    componente.selectedStatusFilter = 'ACTIVO';
    fixture.detectChanges();

    httpMock.expectNone(r => r.url === API_USUARIOS);
  });

  it('aplicarFiltros() manda los filtros al backend y vuelve a la primera página', () => {
    const fixture = iniciar();
    const componente = fixture.componentInstance;

    componente.changePage(1);
    httpMock.expectOne(r => r.url === API_USUARIOS).flush(pagina(UN_USUARIO, { number: 1 }));
    expect(componente.currentPage()).toBe(1);

    componente.searchTerm = 'ana';
    componente.selectedRoleFilter = 'ADMIN';
    componente.selectedStatusFilter = 'SUSPENDIDO';
    componente.aplicarFiltros();

    const req = httpMock.expectOne(r => r.url === API_USUARIOS);
    expect(req.request.params.get('busqueda')).toBe('ana');
    expect(req.request.params.get('rol')).toBe('ADMIN');
    expect(req.request.params.get('estadoCuenta')).toBe('false');
    expect(req.request.params.get('page')).toBe('0');
    req.flush(pagina(UN_USUARIO));

    expect(componente.currentPage()).toBe(0);
  });

  it('paginar conserva los filtros aplicados y NO arrastra los que aún no se aplicaron', () => {
    const fixture = iniciar();
    const componente = fixture.componentInstance;

    componente.searchTerm = 'ana';
    componente.aplicarFiltros();
    httpMock.expectOne(r => r.url === API_USUARIOS).flush(pagina(UN_USUARIO));

    // El usuario escribe otro término pero NO pulsa Filtrar, y pagina.
    componente.searchTerm = 'beto';
    componente.changePage(1);

    const req = httpMock.expectOne(r => r.url === API_USUARIOS);
    expect(req.request.params.get('busqueda')).toBe('ana');
    expect(req.request.params.get('page')).toBe('1');
    req.flush(pagina(UN_USUARIO, { number: 1 }));
  });

  it('limpiarFiltros() borra los filtros y recarga sin parámetros', () => {
    const fixture = iniciar();
    const componente = fixture.componentInstance;

    componente.searchTerm = 'ana';
    componente.selectedRoleFilter = 'ADMIN';
    componente.aplicarFiltros();
    httpMock.expectOne(r => r.url === API_USUARIOS).flush(pagina(UN_USUARIO));

    componente.limpiarFiltros();

    const req = httpMock.expectOne(r => r.url === API_USUARIOS);
    expect(req.request.params.has('busqueda')).toBe(false);
    expect(req.request.params.has('rol')).toBe(false);
    expect(req.request.params.has('estadoCuenta')).toBe(false);
    req.flush(pagina(UN_USUARIO));

    expect(componente.searchTerm).toBe('');
    expect(componente.selectedRoleFilter).toBe('ALL');
    expect(componente.selectedStatusFilter).toBe('ALL');
  });

  it('exportar() usa los filtros aplicados, no el borrador sin aplicar', () => {
    const fixture = iniciar();
    const componente = fixture.componentInstance;

    componente.searchTerm = 'ana';
    componente.aplicarFiltros();
    httpMock.expectOne(r => r.url === API_USUARIOS).flush(pagina(UN_USUARIO));

    componente.searchTerm = 'beto'; // escrito pero no aplicado
    componente.exportar('CSV');

    const req = httpMock.expectOne(r => r.url === `${API_USUARIOS}/exportar`);
    expect(req.request.params.get('busqueda')).toBe('ana');
    expect(req.request.params.get('formato')).toBe('CSV');
    req.flush(new Blob(['x']));
  });
});
