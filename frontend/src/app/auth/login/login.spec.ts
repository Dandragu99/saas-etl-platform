import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthSessionService } from '../auth-session.service';
import { Login } from './login';

describe('Login', () => {
  const sessions = {
    login: vi.fn(() => of({ userId: 'u1', email: 'owner@example.com', activeCompany: null })),
  };

  beforeEach(async () => {
    sessions.login.mockClear();
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: AuthSessionService, useValue: sessions }],
    }).compileComponents();
  });

  it('should not submit invalid credentials', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(sessions.login).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelectorAll('.field-error').length).toBe(2);
  });
});
