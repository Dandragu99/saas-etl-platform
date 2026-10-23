import { Component, computed, effect, inject, OnDestroy, signal, untracked } from '@angular/core';
import { Subscription } from 'rxjs';

import { AuthSessionService } from '../auth/auth-session.service';
import { hasCompanyPermission } from '../auth/company-permissions';
import { ProductApiService } from './product-api.service';
import { ProductForm } from './product-form/product-form';
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
  imports: [ProductForm],
  templateUrl: './products.html',
  styleUrl: './products.css',
})
export class Products implements OnDestroy {
  private static readonly PAGE_SIZE = 20;
  private readonly api = inject(ProductApiService);
  private readonly sessions = inject(AuthSessionService);
  private loadSubscription: Subscription | null = null;
  private operationSubscription: Subscription | null = null;

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
  protected readonly editedProduct = signal<Product | null | undefined>(undefined);
  protected readonly statusProduct = signal<Product | null>(null);
  protected readonly operationPending = signal(false);
  protected readonly operationMessage = signal<string | null>(null);
  protected readonly operationError = signal<string | null>(null);
  protected readonly activeCompanyId = computed(
    () => this.sessions.session()?.activeCompany?.id ?? null,
  );
  protected readonly canWrite = computed(() =>
    hasCompanyPermission(this.sessions.session()?.activeCompany?.role, 'PRODUCT_WRITE'),
  );

  constructor() {
    effect(() => {
      const companyId = this.sessions.session()?.activeCompany?.id ?? null;
      untracked(() => {
        this.closeEditor();
        this.operationSubscription?.unsubscribe();
        this.operationPending.set(false);
        this.operationMessage.set(null);
        this.operationError.set(null);
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
    this.operationSubscription?.unsubscribe();
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

  protected createProduct(): void {
    if (this.canWrite()) this.editedProduct.set(null);
  }

  protected editProduct(product: Product): void {
    if (this.canWrite()) this.editedProduct.set(product);
  }

  protected closeEditor(): void {
    this.editedProduct.set(undefined);
    this.statusProduct.set(null);
  }

  protected productSaved(product: Product): void {
    const wasCreation = this.editedProduct() === null;
    this.closeEditor();
    this.operationMessage.set(
      wasCreation ? 'Producto creado correctamente.' : 'Producto actualizado correctamente.',
    );
    this.operationError.set(null);
    this.reload();
  }

  protected requestStatusChange(product: Product): void {
    if (this.canWrite()) this.statusProduct.set(product);
  }

  protected confirmStatusChange(): void {
    const product = this.statusProduct();
    const companyId = this.activeCompanyId();
    if (product === null || companyId === null || this.operationPending()) return;
    this.operationPending.set(true);
    this.operationError.set(null);
    this.operationMessage.set(null);
    this.operationSubscription = this.api
      .changeProductStatus(product.id, {
        active: !product.active,
        expectedVersion: product.version,
      })
      .subscribe({
        next: () => {
          if (this.activeCompanyId() !== companyId) return;
          this.statusProduct.set(null);
          this.operationPending.set(false);
          this.operationMessage.set(
            product.active
              ? 'Producto desactivado correctamente.'
              : 'Producto reactivado correctamente.',
          );
          this.reload();
        },
        error: (error: unknown) => {
          if (this.activeCompanyId() !== companyId) return;
          this.statusProduct.set(null);
          this.operationPending.set(false);
          this.operationError.set(this.operationErrorMessage(error));
        },
      });
  }

  protected reloadAfterConflict(): void {
    this.statusProduct.set(null);
    this.operationError.set(null);
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

  private operationErrorMessage(error: unknown): string {
    if (error instanceof ProductRequestError && error.code === 'PRODUCT_VERSION_CONFLICT') {
      return 'El producto ha cambiado. Recarga el catálogo antes de volver a intentarlo.';
    }
    if (error instanceof ProductRequestError) return error.message;
    return 'No se pudo cambiar el estado del producto. Inténtalo de nuevo.';
  }
}
