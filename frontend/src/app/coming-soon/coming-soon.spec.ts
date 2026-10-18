import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ComingSoon } from './coming-soon';

describe('ComingSoon', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ComingSoon],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({
              moduleTitle: 'Inventario',
              moduleDescription: 'Controla existencias y movimientos de almacén.',
            }),
          },
        },
      ],
    }).compileComponents();
  });

  it('should identify the module as unavailable without showing business data', async () => {
    const fixture = TestBed.createComponent(ComingSoon);
    await fixture.whenStable();
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('Inventario');
    expect(text).toContain('Módulo en preparación');
    expect(text).toContain('todavía no tiene funcionalidad ni datos empresariales asociados');
  });
});
