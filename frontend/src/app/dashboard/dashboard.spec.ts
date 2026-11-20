import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('should show the mobile commercial story with clearly labelled demo data', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    await fixture.whenStable();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Buenos días');
    expect(element.textContent).toContain('Facturación mensual');
    expect(element.textContent).toContain('datos ficticios realistas');
    expect(element.querySelector('a[href="/obras"]')).not.toBeNull();
  });
});
