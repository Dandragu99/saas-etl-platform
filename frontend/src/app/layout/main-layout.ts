import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthSessionService } from '../auth/auth-session.service';
import { CompanyAccess } from '../auth/auth.models';

interface NavigationItem {
  readonly label: string;
  readonly route: string;
  readonly icon: string;
  readonly available: boolean;
}

@Component({
  selector: 'app-main-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
})
export class MainLayout implements OnInit {
  private readonly sessions = inject(AuthSessionService);
  private readonly router: Router;
  private readonly currentUrl = signal('/dashboard');

  protected readonly sidebarOpen = signal(false);
  protected readonly session = this.sessions.session;
  protected readonly companies = signal<readonly CompanyAccess[]>([]);
  protected readonly companyLoading = signal(false);
  protected readonly sessionActionPending = signal(false);
  protected readonly navigation: readonly NavigationItem[] = [
    { label: 'Dashboard', route: '/dashboard', icon: 'dashboard', available: true },
    { label: 'Documentos', route: '/documentos', icon: 'document', available: false },
    {
      label: 'Transformaciones',
      route: '/transformaciones',
      icon: 'transform',
      available: true,
    },
    { label: 'Productos', route: '/productos', icon: 'product', available: false },
    { label: 'Inventario', route: '/inventario', icon: 'inventory', available: false },
    { label: 'Clientes', route: '/clientes', icon: 'clients', available: false },
    { label: 'Ventas', route: '/ventas', icon: 'sales', available: false },
    { label: 'Gastos', route: '/gastos', icon: 'expenses', available: false },
    { label: 'Asistente IA', route: '/asistente-ia', icon: 'assistant', available: false },
  ];
  protected readonly pageTitle = computed(
    () =>
      this.navigation.find((item) => this.currentUrl().startsWith(item.route))?.label ??
      'Dashboard',
  );

  constructor(router: Router) {
    this.router = router;
    this.currentUrl.set(router.url === '/' ? '/dashboard' : router.url);
    router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe((event) => {
        this.currentUrl.set(event.urlAfterRedirects);
        this.sidebarOpen.set(false);
      });
  }

  ngOnInit(): void {
    this.companyLoading.set(true);
    this.sessions.getCompanies().subscribe({
      next: (companies) => {
        this.companies.set(companies);
        this.companyLoading.set(false);
      },
      error: () => this.companyLoading.set(false),
    });
  }

  protected toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  protected closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  protected changeCompany(event: Event): void {
    const companyId = (event.target as HTMLSelectElement).value;
    if (companyId.length === 0 || companyId === this.session()?.activeCompany?.id) {
      return;
    }
    this.sessionActionPending.set(true);
    this.sessions.selectCompany(companyId).subscribe({
      next: () => window.location.assign('/dashboard'),
      error: () => this.sessionActionPending.set(false),
    });
  }

  protected logout(): void {
    if (this.sessionActionPending()) {
      return;
    }
    this.sessionActionPending.set(true);
    this.sessions.logout().subscribe({
      next: () => void this.router.navigateByUrl('/login'),
      error: () => void this.router.navigateByUrl('/login'),
    });
  }
}
