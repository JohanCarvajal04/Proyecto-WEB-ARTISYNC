import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { describe, expect, it } from 'vitest';
import { AccesoRequeridoComponent } from './acceso-requerido.component';

function crear(queryParams: Record<string, string>) {
  const rutaFalsa = {
    snapshot: { queryParamMap: { get: (clave: string) => queryParams[clave] ?? null } }
  };

  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      { provide: ActivatedRoute, useValue: rutaFalsa }
    ]
  });

  const fixture = TestBed.createComponent(AccesoRequeridoComponent);
  fixture.detectChanges();
  return fixture;
}

describe('AccesoRequeridoComponent', () => {
  it('propaga returnUrl a los enlaces de login y registro', () => {
    const fixture = crear({ returnUrl: '/pedido/crear?idServicio=7', motivo: 'contratar' });

    const anchors: HTMLAnchorElement[] = Array.from(fixture.nativeElement.querySelectorAll('a'));
    const login = anchors.find(a => (a.getAttribute('href') ?? '').includes('/auth/login'))!;
    const registro = anchors.find(a => (a.getAttribute('href') ?? '').includes('/auth/register'))!;

    expect(login.getAttribute('href')).toContain('/auth/login');
    expect(login.getAttribute('href')).toContain('returnUrl');
    expect(registro.getAttribute('href')).toContain('/auth/register');
    expect(registro.getAttribute('href')).toContain('returnUrl');
  });

  it('adapta el mensaje según el motivo', () => {
    const fixture = crear({ returnUrl: '/explorar/creador/3', motivo: 'seguir' });

    expect(fixture.nativeElement.textContent).toContain('seguir a este creador');
  });

  it('usa un mensaje genérico si no llega un motivo reconocido', () => {
    const fixture = crear({});

    expect(fixture.nativeElement.textContent).toContain('Necesitas iniciar sesión para continuar');
  });

  it('sin returnUrl, cae a /explorar', () => {
    const fixture = crear({});

    const anchors: HTMLAnchorElement[] = Array.from(fixture.nativeElement.querySelectorAll('a'));
    const login = anchors.find(a => (a.getAttribute('href') ?? '').includes('/auth/login'))!;
    expect(decodeURIComponent(login.getAttribute('href') ?? '')).toContain('returnUrl=/explorar');
  });
});
