import { Component, computed, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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
export class MainLayout {
  private readonly currentUrl = signal('/dashboard');

  protected readonly sidebarOpen = signal(false);
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

  protected toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  protected closeSidebar(): void {
    this.sidebarOpen.set(false);
  }
}
