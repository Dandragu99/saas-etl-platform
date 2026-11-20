import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { DEFAULT_SETTINGS } from './mini.models';
import { MiniShell } from './mini-shell';
import { MiniStoreService } from './mini-store.service';

describe('MiniShell', () => {
  it('renders an independent four-destination mobile navigation', async () => {
    await TestBed.configureTestingModule({
      imports: [MiniShell],
      providers: [
        provideRouter([]),
        {
          provide: MiniStoreService,
          useValue: {
            ready: signal(true),
            settings: signal(DEFAULT_SETTINGS),
            load: vi.fn(() => Promise.resolve()),
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(MiniShell);
    fixture.detectChanges();
    const labels = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.mini-nav a'),
    ).map((item) => item.textContent?.trim());
    expect(labels).toEqual(['⌂Inicio', '♙Clientes', '▤Presupuestos', '⚡Trabajos']);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Voltia Mini');
  });
});
