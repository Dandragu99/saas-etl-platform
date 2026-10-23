import { signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';

import { AuthSessionService } from '../auth/auth-session.service';
import { AuthSession } from '../auth/auth.models';
import { ProductApiService } from './product-api.service';
import { Product, ProductPage, ProductQuery, ProductRequestError } from './product.models';
import { Products } from './products';

const PRODUCT: Product = {
  id: 'product-1',
  sku: 'CHAIR-1',
  name: 'Office chair',
  description: null,
  category: 'Furniture',
  price: '12.3400',
  currency: 'EUR',
  unitOfMeasure: 'UNIT',
  active: true,
  version: 0,
  createdAt: '2026-10-23T10:00:00Z',
  updatedAt: '2026-10-23T10:00:00Z',
};

const page = (
  content: readonly Product[] = [PRODUCT],
  currentPage = 0,
  totalPages = 1,
): ProductPage => ({
  content,
  totalElements: content.length === 0 ? 0 : 21,
  totalPages,
  page: currentPage,
  size: 20,
});

class ProductApiStub {
  getProducts(_query: ProductQuery): Observable<ProductPage> {
    return of(page());
  }

  changeProductStatus(
    _id: string,
    _request: { active: boolean; expectedVersion: number },
  ): Observable<Product> {
    return of({ ...PRODUCT, active: false, version: PRODUCT.version + 1 });
  }
}

describe('Products', () => {
  let api: ProductApiStub;
  let sessionState: WritableSignal<AuthSession>;

  beforeEach(async () => {
    api = new ProductApiStub();
    sessionState = signal({
      userId: 'user-1',
      email: 'owner@example.com',
      activeCompany: { id: 'company-1', name: 'Nexo', defaultCurrency: 'EUR', role: 'OWNER' },
    });
    await TestBed.configureTestingModule({
      imports: [Products],
      providers: [
        { provide: ProductApiService, useValue: api },
        { provide: AuthSessionService, useValue: { session: sessionState.asReadonly() } },
      ],
    }).compileComponents();
  });

  it('should show loading and then render the product table', async () => {
    const pending = new Subject<ProductPage>();
    vi.spyOn(api, 'getProducts').mockReturnValue(pending);
    const fixture = createComponent();

    expect(fixture.nativeElement.textContent).toContain('Cargando productos');
    pending.next(page());
    pending.complete();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(element.textContent).toContain('CHAIR-1');
    expect(element.textContent).toContain('Office chair');
    expect(element.textContent).toContain('12.3400');
    expect(element.textContent).toContain('EUR');
    expect(element.querySelector('[data-active="true"]')?.textContent).toContain('Activo');
  });

  it('should apply name, sku and category filters using the backend', async () => {
    const getProducts = vi.spyOn(api, 'getProducts');
    const fixture = createComponent();
    await fixture.whenStable();
    const inputs = fixture.nativeElement.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    inputs[0].value = 'chair';
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = 'CH';
    inputs[1].dispatchEvent(new Event('input'));
    inputs[2].value = 'Furniture';
    inputs[2].dispatchEvent(new Event('input'));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(getProducts).toHaveBeenLastCalledWith(
      expect.objectContaining({ name: 'chair', sku: 'CH', category: 'Furniture', page: 0 }),
    );
  });

  it('should filter by status and update sorting', async () => {
    const getProducts = vi.spyOn(api, 'getProducts');
    const fixture = createComponent();
    await fixture.whenStable();
    const selects = fixture.nativeElement.querySelectorAll(
      'select',
    ) as NodeListOf<HTMLSelectElement>;
    selects[0].value = 'false';
    selects[0].dispatchEvent(new Event('change'));
    selects[1].value = 'price';
    selects[1].dispatchEvent(new Event('change'));
    selects[2].value = 'desc';
    selects[2].dispatchEvent(new Event('change'));
    await fixture.whenStable();

    expect(getProducts).toHaveBeenLastCalledWith(
      expect.objectContaining({ active: false, sort: 'price', direction: 'desc', page: 0 }),
    );
  });

  it('should paginate using backend pages', async () => {
    vi.spyOn(api, 'getProducts').mockReturnValue(of(page([PRODUCT], 0, 2)));
    const getProducts = vi.spyOn(api, 'getProducts');
    const fixture = createComponent();
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    const buttons = Array.from(element.querySelectorAll<HTMLButtonElement>('.pagination button'));
    buttons[1].click();
    await fixture.whenStable();

    expect(getProducts).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1, size: 20 }));
  });

  it('should show empty and safe error states', async () => {
    vi.spyOn(api, 'getProducts').mockReturnValue(of(page([])));
    const emptyFixture = createComponent();
    await emptyFixture.whenStable();
    expect(emptyFixture.nativeElement.textContent).toContain('No hay productos para mostrar.');
    emptyFixture.destroy();

    vi.spyOn(api, 'getProducts').mockReturnValue(
      throwError(() => new ProductRequestError('No se pudieron cargar los productos.')),
    );
    const errorFixture = createComponent();
    await errorFixture.whenStable();
    expect(errorFixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'No se pudieron cargar los productos.',
    );
  });

  it('should clear old products and reload when the active company changes', async () => {
    const secondCompany = new Subject<ProductPage>();
    const getProducts = vi
      .spyOn(api, 'getProducts')
      .mockReturnValueOnce(of(page()))
      .mockReturnValueOnce(secondCompany);
    const fixture = createComponent();
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Office chair');

    sessionState.set({
      ...sessionState(),
      activeCompany: { ...sessionState().activeCompany!, id: 'company-2', name: 'Other' },
    });
    fixture.detectChanges();

    expect(getProducts).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).not.toContain('Office chair');
    expect(fixture.nativeElement.textContent).toContain('Cargando productos');
    secondCompany.next(page([]));
    secondCompany.complete();
  });

  it('should show write actions only to owner and admin', async () => {
    const ownerFixture = createComponent();
    await ownerFixture.whenStable();
    expect(ownerFixture.nativeElement.textContent).toContain('Nuevo producto');
    expect(ownerFixture.nativeElement.textContent).toContain('Editar');
    ownerFixture.destroy();

    sessionState.set({
      ...sessionState(),
      activeCompany: { ...sessionState().activeCompany!, role: 'VIEWER' },
    });
    const viewerFixture = createComponent();
    await viewerFixture.whenStable();
    expect(viewerFixture.nativeElement.textContent).not.toContain('Nuevo producto');
    expect(viewerFixture.nativeElement.textContent).not.toContain('Editar');
  });

  it('should change status with expectedVersion and preserve the current query', async () => {
    const changeStatus = vi.spyOn(api, 'changeProductStatus');
    const getProducts = vi.spyOn(api, 'getProducts');
    const fixture = createComponent();
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('.status-action')?.click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Desactivar producto');
    element.querySelector<HTMLButtonElement>('.confirm-dialog button:last-child')?.click();
    await fixture.whenStable();

    expect(changeStatus).toHaveBeenCalledWith(PRODUCT.id, {
      active: false,
      expectedVersion: PRODUCT.version,
    });
    expect(getProducts).toHaveBeenCalledTimes(2);
    expect(element.textContent).toContain('Producto desactivado correctamente.');
  });

  it('should offer a catalog reload after a status version conflict', async () => {
    vi.spyOn(api, 'changeProductStatus').mockReturnValue(
      throwError(() => new ProductRequestError('Conflict', 'PRODUCT_VERSION_CONFLICT', 409)),
    );
    const fixture = createComponent();
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('.status-action')?.click();
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('.confirm-dialog button:last-child')?.click();
    await fixture.whenStable();

    expect(element.textContent).toContain('El producto ha cambiado');
    expect(element.textContent).toContain('Recargar catálogo');
  });

  it('should close an open editor when the active company changes', async () => {
    const fixture = createComponent();
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('.row-actions .link-button')?.click();
    fixture.detectChanges();
    expect(element.querySelector('app-product-form')).not.toBeNull();

    sessionState.set({
      ...sessionState(),
      activeCompany: { ...sessionState().activeCompany!, id: 'company-2' },
    });
    fixture.detectChanges();
    expect(element.querySelector('app-product-form')).toBeNull();
  });
});

function createComponent(): ComponentFixture<Products> {
  const fixture = TestBed.createComponent(Products);
  fixture.detectChanges();
  return fixture;
}
