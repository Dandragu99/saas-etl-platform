import { Component, effect, inject, OnDestroy, signal, untracked } from '@angular/core';
import { Subscription } from 'rxjs';

import { AuthSessionService } from '../auth/auth-session.service';
import { ProductApiService } from './product-api.service';
import {
  Product,
  ProductQuery,
  ProductRequestError,
  ProductSort,
  SortDirection,
  UnitOfMeasure,
} from './product.models';

type ProductsStatus = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-products',
  templateUrl: './products.html',
  styleUrl: './products.css',
})
export class Products implements OnDestroy {
  private static readonly PAGE_SIZE = 20;
  private readonly api = inject(ProductApiService);
  private readonly sessions = inject(AuthSessionService);
  private loadSubscription: Subscription | null = null;

  protected readonly status = signal<ProductsStatus>('loading');
  protected readonly products = signal<readonly Product[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly currentPage = signal(0);
  protected readonly nameFilter = signal('');
  protected readonly skuFilter = signal('');
  protected readonly categoryFilter = signal('');
  protected readonly activeFilter = signal<boolean | undefined>(undefined);
  protected readonly sort = signal<ProductSort>('name');
  protected readonly direction = signal<SortDirection>('asc');

  constructor() {
    effect(() => {
      const companyId = this.sessions.session()?.activeCompany?.id ?? null;
      untracked(() => {
        this.clearCompanyData();
        if (companyId === null) {
          this.status.set('success');
          return;
        }
        this.loadProducts(companyId);
      });
    });
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
  }

  protected applyFilters(event: Event): void {
    event.preventDefault();
    this.currentPage.set(0);
    this.reload();
  }

  protected updateName(event: Event): void {
    this.nameFilter.set((event.target as HTMLInputElement).value);
  }

  protected updateSku(event: Event): void {
    this.skuFilter.set((event.target as HTMLInputElement).value);
  }

  protected updateCategory(event: Event): void {
    this.categoryFilter.set((event.target as HTMLInputElement).value);
  }

  protected updateActive(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.activeFilter.set(value === '' ? undefined : value === 'true');
    this.currentPage.set(0);
    this.reload();
  }

  protected updateSort(event: Event): void {
    this.sort.set((event.target as HTMLSelectElement).value as ProductSort);
    this.currentPage.set(0);
    this.reload();
  }

  protected updateDirection(event: Event): void {
    this.direction.set((event.target as HTMLSelectElement).value as SortDirection);
    this.currentPage.set(0);
    this.reload();
  }

  protected clearFilters(): void {
    this.nameFilter.set('');
    this.skuFilter.set('');
    this.categoryFilter.set('');
    this.activeFilter.set(undefined);
    this.currentPage.set(0);
    this.reload();
  }

  protected retry(): void {
    this.reload();
  }

  protected previousPage(): void {
    if (this.currentPage() === 0) return;
    this.currentPage.update((page) => page - 1);
    this.reload();
  }

  protected nextPage(): void {
    if (this.currentPage() + 1 >= this.totalPages()) return;
    this.currentPage.update((page) => page + 1);
    this.reload();
  }

  protected unitLabel(unit: UnitOfMeasure): string {
    switch (unit) {
      case 'UNIT':
        return 'Unidad';
      case 'KG':
        return 'Kilogramo';
      case 'G':
        return 'Gramo';
      case 'L':
        return 'Litro';
      case 'ML':
        return 'Mililitro';
      case 'M':
        return 'Metro';
      case 'M2':
        return 'Metro cuadrado';
      case 'HOUR':
        return 'Hora';
    }
  }

  private reload(): void {
    const companyId = this.sessions.session()?.activeCompany?.id;
    if (companyId !== undefined) {
      this.loadProducts(companyId);
    }
  }

  private clearCompanyData(): void {
    this.loadSubscription?.unsubscribe();
    this.loadSubscription = null;
    this.products.set([]);
    this.totalElements.set(0);
    this.totalPages.set(0);
    this.currentPage.set(0);
    this.errorMessage.set(null);
  }

  private loadProducts(companyId: string): void {
    this.loadSubscription?.unsubscribe();
    this.status.set('loading');
    this.errorMessage.set(null);

    const query: ProductQuery = {
      name: this.nameFilter(),
      sku: this.skuFilter(),
      category: this.categoryFilter(),
      active: this.activeFilter(),
      page: this.currentPage(),
      size: Products.PAGE_SIZE,
      sort: this.sort(),
      direction: this.direction(),
    };

    this.loadSubscription = this.api.getProducts(query).subscribe({
      next: (page) => {
        if (this.sessions.session()?.activeCompany?.id !== companyId) return;
        this.products.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.currentPage.set(page.page);
        this.status.set('success');
      },
      error: (error: unknown) => {
        if (this.sessions.session()?.activeCompany?.id !== companyId) return;
        this.products.set([]);
        this.totalElements.set(0);
        this.totalPages.set(0);
        this.errorMessage.set(this.safeMessage(error));
        this.status.set('error');
      },
    });
  }

  private safeMessage(error: unknown): string {
    return error instanceof ProductRequestError
      ? error.message
      : 'No se pudieron cargar los productos. Inténtalo de nuevo.';
  }
}
