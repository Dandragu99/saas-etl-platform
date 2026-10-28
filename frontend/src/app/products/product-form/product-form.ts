import { Component, effect, inject, input, OnDestroy, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';

import { AuthSessionService } from '../../auth/auth-session.service';
import { ProductApiService } from '../product-api.service';
import { Product, ProductRequestError, ProductValues, UnitOfMeasure } from '../product.models';

@Component({
  selector: 'app-product-form',
  imports: [ReactiveFormsModule],
  templateUrl: './product-form.html',
  styleUrl: './product-form.css',
})
export class ProductForm implements OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly api = inject(ProductApiService);
  private readonly sessions = inject(AuthSessionService);
  private requestSubscription: Subscription | null = null;

  readonly product = input<Product | null>(null);
  readonly companyId = input.required<string>();
  readonly saved = output<Product>();
  readonly cancelled = output<void>();

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly staleVersion = signal(false);
  protected readonly units: readonly { value: UnitOfMeasure; label: string }[] = [
    { value: 'UNIT', label: 'Unidad' },
    { value: 'KG', label: 'Kilogramo' },
    { value: 'G', label: 'Gramo' },
    { value: 'L', label: 'Litro' },
    { value: 'ML', label: 'Mililitro' },
    { value: 'M', label: 'Metro' },
    { value: 'M2', label: 'Metro cuadrado' },
    { value: 'HOUR', label: 'Hora' },
  ];
  protected readonly form = this.formBuilder.nonNullable.group({
    sku: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(80)]],
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(160)]],
    description: ['', [Validators.maxLength(2000)]],
    category: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)]],
    price: ['', [Validators.required, Validators.pattern(/^[0-9]{1,15}(\.[0-9]{1,4})?$/)]],
    unitOfMeasure: ['UNIT' as UnitOfMeasure, [Validators.required]],
  });

  constructor() {
    effect(() => {
      const product = this.product();
      this.form.reset({
        sku: product?.sku ?? '',
        name: product?.name ?? '',
        description: product?.description ?? '',
        category: product?.category ?? '',
        price: product?.price ?? '',
        unitOfMeasure: product?.unitOfMeasure ?? 'UNIT',
      });
      this.errorMessage.set(null);
      this.staleVersion.set(false);
    });
  }

  ngOnDestroy(): void {
    this.requestSubscription?.unsubscribe();
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.sameCompany()) {
      this.errorMessage.set(
        'La empresa activa ha cambiado. Cierra el formulario y vuelve a abrirlo.',
      );
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.staleVersion.set(false);
    const product = this.product();
    const values = this.values();
    const request =
      product === null
        ? this.api.createProduct(values)
        : this.api.updateProduct(product.id, { ...values, expectedVersion: product.version });

    this.requestSubscription = request.subscribe({
      next: (savedProduct) => {
        if (this.sameCompany()) this.saved.emit(savedProduct);
        this.submitting.set(false);
      },
      error: (error: unknown) => {
        this.handleError(error);
        this.submitting.set(false);
      },
    });
  }

  protected cancel(): void {
    if (!this.submitting()) this.cancelled.emit();
  }

  protected validationMessage(
    field: 'sku' | 'name' | 'description' | 'category' | 'price',
  ): string {
    const errors = this.form.controls[field].errors;
    if (errors === null) return '';
    if (errors['duplicate']) return 'Ya existe un producto con ese SKU en la empresa activa.';
    if (errors['required'] || (errors['pattern'] && field !== 'price')) {
      return this.requiredMessage(field);
    }
    if (errors['maxlength']) {
      return this.maximumLengthMessage(field);
    }
    if (field === 'price' && errors['pattern']) {
      return 'Debe ser un importe no negativo, con hasta 15 enteros y 4 decimales.';
    }
    return 'El valor introducido no es válido.';
  }

  protected reloadProduct(): void {
    const product = this.product();
    if (product === null || this.submitting() || !this.sameCompany()) return;
    this.submitting.set(true);
    this.requestSubscription = this.api.getProduct(product.id).subscribe({
      next: (freshProduct) => {
        if (this.sameCompany()) {
          this.form.reset({
            sku: freshProduct.sku,
            name: freshProduct.name,
            description: freshProduct.description ?? '',
            category: freshProduct.category,
            price: freshProduct.price,
            unitOfMeasure: freshProduct.unitOfMeasure,
          });
          this.saved.emit(freshProduct);
        }
        this.submitting.set(false);
      },
      error: () => {
        this.errorMessage.set(
          'No se pudo recargar el producto. Cierra el formulario e inténtalo de nuevo.',
        );
        this.submitting.set(false);
      },
    });
  }

  private values(): ProductValues {
    const value = this.form.getRawValue();
    return {
      sku: value.sku.trim(),
      name: value.name.trim(),
      description: value.description.trim() || null,
      category: value.category.trim(),
      price: value.price,
      unitOfMeasure: value.unitOfMeasure,
    };
  }

  private sameCompany(): boolean {
    return this.sessions.session()?.activeCompany?.id === this.companyId();
  }

  private handleError(error: unknown): void {
    if (error instanceof ProductRequestError) {
      if (error.code === 'PRODUCT_SKU_ALREADY_EXISTS') {
        this.form.controls.sku.setErrors({
          ...this.form.controls.sku.errors,
          duplicate: true,
        });
        this.form.controls.sku.markAsTouched();
        return;
      }
      if (error.code === 'PRODUCT_VERSION_CONFLICT') {
        this.errorMessage.set(
          'El producto ha cambiado desde que lo abriste. Recarga sus datos antes de guardar.',
        );
        this.staleVersion.set(true);
        return;
      }
      this.errorMessage.set(error.message);
      return;
    }
    this.errorMessage.set('No se pudo guardar el producto. Inténtalo de nuevo.');
  }

  private requiredMessage(field: 'sku' | 'name' | 'description' | 'category' | 'price'): string {
    switch (field) {
      case 'sku':
        return 'El SKU es obligatorio.';
      case 'name':
        return 'El nombre es obligatorio.';
      case 'category':
        return 'La categoría es obligatoria.';
      case 'price':
        return 'El precio es obligatorio.';
      case 'description':
        return '';
    }
  }

  private maximumLengthMessage(
    field: 'sku' | 'name' | 'description' | 'category' | 'price',
  ): string {
    switch (field) {
      case 'sku':
        return 'El SKU no puede superar 80 caracteres.';
      case 'name':
        return 'El nombre no puede superar 160 caracteres.';
      case 'description':
        return 'La descripción no puede superar 2000 caracteres.';
      case 'category':
        return 'La categoría no puede superar 100 caracteres.';
      case 'price':
        return '';
    }
  }
}
