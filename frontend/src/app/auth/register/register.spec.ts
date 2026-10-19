import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthSessionService } from '../auth-session.service';
import { Register } from './register';

describe('Register', () => {
  const sessions = { register: vi.fn(() => of({ userId: 'u1', companyId: 'c1' })) };

  beforeEach(async () => {
    sessions.register.mockClear();
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [provideRouter([]), { provide: AuthSessionService, useValue: sessions }],
    }).compileComponents();
  });

  it('should validate fields before creating a company owner', () => {
    const fixture = TestBed.createComponent(Register);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(sessions.register).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelectorAll('.field-error').length).toBeGreaterThan(0);
  });
});
