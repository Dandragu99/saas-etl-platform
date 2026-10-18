import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { MainLayout } from './main-layout';

describe('MainLayout', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [provideRouter([{ path: 'transformaciones', children: [] }])],
    }).compileComponents();
  });

  it('should render every business navigation destination', () => {
    const fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
    const links = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLAnchorElement>(
        '.main-navigation a',
      ),
    );

    expect(links.map((link) => link.textContent?.trim())).toEqual([
      'Dashboard',
      'DocumentosPronto',
      'Transformaciones',
      'ProductosPronto',
      'InventarioPronto',
      'ClientesPronto',
      'VentasPronto',
      'GastosPronto',
      'Asistente IAPronto',
    ]);
  });

  it('should open and close the mobile navigation', () => {
    const fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const menuButton = element.querySelector<HTMLButtonElement>('.menu-button');

    menuButton?.click();
    fixture.detectChanges();
    expect(element.querySelector('.sidebar')?.classList.contains('sidebar-open')).toBe(true);
    expect(menuButton?.getAttribute('aria-expanded')).toBe('true');

    element.querySelector<HTMLButtonElement>('.backdrop')?.click();
    fixture.detectChanges();
    expect(element.querySelector('.sidebar')?.classList.contains('sidebar-open')).toBe(false);
  });

  it('should update the visible title after navigation', async () => {
    const router = TestBed.inject(Router);
    const fixture = TestBed.createComponent(MainLayout);

    await router.navigateByUrl('/transformaciones');
    fixture.detectChanges();

    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.topbar-title strong')?.textContent,
    ).toContain('Transformaciones');
  });
});
