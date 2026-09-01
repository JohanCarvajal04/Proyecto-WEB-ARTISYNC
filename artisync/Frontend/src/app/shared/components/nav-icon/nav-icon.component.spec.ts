import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { describe, expect, it } from 'vitest';
import { NavIconComponent } from './nav-icon.component';
import { NAV_ICON_PATHS, NAV_ICON_FALLBACK } from './nav-icon.paths';

describe('NavIconComponent', () => {
  it('renderiza el path correcto para un icono conocido', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
    const fixture = TestBed.createComponent(NavIconComponent);
    fixture.componentRef.setInput('name', 'dashboard');
    await fixture.whenStable();

    const d = fixture.nativeElement.querySelector('path').getAttribute('d');
    expect(d).toBe(NAV_ICON_PATHS['dashboard']);
  });

  it('cae en el icono de respaldo sin lanzar cuando el nombre no existe', async () => {
    TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
    const fixture = TestBed.createComponent(NavIconComponent);
    fixture.componentRef.setInput('name', 'icono-que-no-existe');
    await fixture.whenStable();

    const d = fixture.nativeElement.querySelector('path').getAttribute('d');
    expect(d).toBe(NAV_ICON_FALLBACK);
  });
});
