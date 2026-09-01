import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection, signal } from '@angular/core';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, expect, it, vi } from 'vitest';
import { throwError } from 'rxjs';
import { errorInterceptor } from './error.interceptor';
import { AuthService } from '../../features/seguridad/services/auth.service';
import { ToastService } from '../services/toast.service';

/** Doble mínimo: solo lo que el interceptor consulta. */
function authFalso(isLoggedIn: boolean) {
  return {
    isLoggedIn: signal(isLoggedIn),
    refreshToken: vi.fn()
  };
}

function toastFalso() {
  return { error: vi.fn(), warning: vi.fn(), success: vi.fn(), info: vi.fn() };
}

function crear(isLoggedIn: boolean) {
  const auth = authFalso(isLoggedIn);
  const toast = toastFalso();

  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptors([errorInterceptor])),
      provideHttpClientTesting(),
      { provide: AuthService, useValue: auth },
      { provide: ToastService, useValue: toast }
    ]
  });

  return {
    http: TestBed.inject(HttpClient),
    httpMock: TestBed.inject(HttpTestingController),
    auth,
    toast
  };
}

describe('errorInterceptor', () => {
  it('un 401 sin sesión no intenta refrescar ni avisa "sesión expirada" (visitante anónimo del catálogo)', () => {
    const { http, httpMock, auth, toast } = crear(false);

    http.get('/api/v1/creadores/1/siguiendo/novedades').subscribe({ error: () => {} });
    httpMock.expectOne('/api/v1/creadores/1/siguiendo/novedades').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.refreshToken).not.toHaveBeenCalled();
    expect(toast.error).not.toHaveBeenCalled();
  });

  it('un 401 con sesión activa sí intenta refrescar (expiración real)', () => {
    const { http, httpMock, auth, toast } = crear(true);
    auth.refreshToken.mockReturnValue(throwError(() => new Error('refresh también falló')));

    http.get('/api/v1/pedidos/1').subscribe({ error: () => {} });
    httpMock.expectOne('/api/v1/pedidos/1').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.refreshToken).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('Tu sesión ha expirado. Inicia sesión nuevamente.');
  });
});
