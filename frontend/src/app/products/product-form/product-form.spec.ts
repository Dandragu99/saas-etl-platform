import { signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';

import { AuthSessionService } from '../../auth/auth-session.service';
import { AuthSession } from '../../auth/auth.models';
import { ProductApiService } from '../product-api.service';
import {
  CreateProductRequest,
  Product,
  ProductRequestError,
  UpdateProductRequest,
} from '../product.models';
import { ProductForm } from './product-form';

const PRODUCT: Product = {
  id: 'product-1',
  sku: 'CHAIR-1',
  name: 'Office chair',
  description: 'Ergonomic',
  category: 'Furniture',
  price: '12.3400',
  currency: 'EUR',
  unitOfMeasure: 'UNIT',
  active: true,
  version: 7,
  createdAt: '2026-10-23T10:00:00Z',
  updatedAt: '2026-10-23T10:00:00Z',
};

class ProductApiStub {
  createProduct(request: CreateProductRequest): Observable<Product> {
    return of({ ...PRODUCT, ...request });
  }
  updateProduct(_id: string, request: UpdateProductRequest): Observable<Product> {
    return of({ ...PRODUCT, ...request, version: request.expectedVersion + 1 });
  }
  getProduct(): Observable<Product> {
    return of(PRODUCT);
  }
}

describe('ProductForm', () => {
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
      imports: [ProductForm],
      providers: [
        { provide: ProductApiService, useValue: api },
        { provide: AuthSessionService, useValue: { session: sessionState.asReadonly() } },
      ],
    }).compileComponents();
  });

  it('should validate required fields and decimal price', () => {
    const create = vi.spyOn(api, 'createProduct');
    const fixture = createForm();
    submit(fixture);
    expect(create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Introduce un SKU');
    expect(fixture.nativeElement.textContent).toContain('Usa un importe no negativo');

    setInput(fixture, 'sku', 'SKU-1');
    setInput(fixture, 'name', 'Product');
    setInput(fixture, 'category', 'Category');
    setInput(fixture, 'price', '-1.00');
    submit(fixture);
    expect(fixture.nativeElement.textContent).toContain('Usa un importe no negativo');

    setInput(fixture, 'sku', '   ');
    setInput(fixture, 'price', '1.0000');
    submit(fixture);
    expect(create).not.toHaveBeenCalled();
  });

  it('should create a product once and keep price as a string', async () => {
    const pending = new Subject<Product>();
    const create = vi.spyOn(api, 'createProduct').mockReturnValue(pending);
    const fixture = createForm();
    fillValidForm(fixture);
    submit(fixture);
    submit(fixture);

    expect(create).toHaveBeenCalledTimes(1);
    expect(create).toHaveBeenCalledWith(
      expect.objectContaining({ price: '10.2500', description: null }),
    );
    expect(findSubmit(fixture).disabled).toBe(true);
    pending.next(PRODUCT);
    pending.complete();
  });

  it('should edit using the product expected version', async () => {
    const update = vi.spyOn(api, 'updateProduct');
    const fixture = createForm(PRODUCT);
    await fixture.whenStable();
    setInput(fixture, 'name', 'Updated chair');
    submit(fixture);

    expect(update).toHaveBeenCalledWith(
      PRODUCT.id,
      expect.objectContaining({ name: 'Updated chair', expectedVersion: 7 }),
    );
  });

  it('should explain duplicate SKU and stale version conflicts', async () => {
    vi.spyOn(api, 'createProduct').mockReturnValueOnce(
      throwError(() => new ProductRequestError('Conflict', 'PRODUCT_SKU_ALREADY_EXISTS', 409)),
    );
    const createFixture = createForm();
    fillValidForm(createFixture);
    submit(createFixture);
    expect(createFixture.nativeElement.textContent).toContain('Ya existe un producto con ese SKU');
    createFixture.destroy();

    vi.spyOn(api, 'updateProduct').mockReturnValueOnce(
      throwError(() => new ProductRequestError('Conflict', 'PRODUCT_VERSION_CONFLICT', 409)),
    );
    const editFixture = createForm(PRODUCT);
    await editFixture.whenStable();
    submit(editFixture);
    expect(editFixture.nativeElement.textContent).toContain('El producto ha cambiado');
    expect(editFixture.nativeElement.textContent).toContain('Recargar datos');
  });

  it('should refuse saving after the active company changes', async () => {
    const create = vi.spyOn(api, 'createProduct');
    const fixture = createForm();
    fillValidForm(fixture);
    sessionState.set({
      ...sessionState(),
      activeCompany: { ...sessionState().activeCompany!, id: 'company-2' },
    });
    submit(fixture);

    expect(create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('La empresa activa ha cambiado');
  });
});

function createForm(product: Product | null = null): ComponentFixture<ProductForm> {
  const fixture = TestBed.createComponent(ProductForm);
  fixture.componentRef.setInput('companyId', 'company-1');
  fixture.componentRef.setInput('product', product);
  fixture.detectChanges();
  return fixture;
}

function setInput(fixture: ComponentFixture<ProductForm>, name: string, value: string): void {
  const input = fixture.nativeElement.querySelector(
    `[formcontrolname="${name}"]`,
  ) as HTMLInputElement;
  input.value = value;
  input.dispatchEvent(new Event('input'));
  fixture.detectChanges();
}

function fillValidForm(fixture: ComponentFixture<ProductForm>): void {
  setInput(fixture, 'sku', ' SKU-1 ');
  setInput(fixture, 'name', ' Product ');
  setInput(fixture, 'category', ' Category ');
  setInput(fixture, 'price', '10.2500');
}

function submit(fixture: ComponentFixture<ProductForm>): void {
  fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
  fixture.detectChanges();
}

function findSubmit(fixture: ComponentFixture<ProductForm>): HTMLButtonElement {
  return fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
}
