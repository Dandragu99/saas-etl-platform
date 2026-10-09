import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { of } from 'rxjs';
import { EtlExecutionApiService } from './execution-history/etl-execution-api.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        { provide: EtlExecutionApiService, useValue: { getExecutions: () => of([]) } },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the CSV preview feature', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-csv-preview')).not.toBeNull();
    expect(compiled.querySelector('app-etl-execution-history')).not.toBeNull();
    expect(compiled.querySelector('h1')?.textContent).toContain('SaaS ETL Platform');
  });
});
