import { Component, computed, effect, inject, OnDestroy, signal, untracked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Subscription } from 'rxjs';

import { AuthSessionService } from '../auth/auth-session.service';
import { hasCompanyPermission } from '../auth/company-permissions';
import { InventoryApiService } from './inventory-api.service';
import {
  InventoryMovement,
  InventoryMovementType,
  InventoryRequestError,
  StockLevel,
} from './inventory.models';

type InventoryStatus = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-inventory',
  imports: [DatePipe],
  templateUrl: './inventory.html',
  styleUrl: './inventory.css',
})
export class Inventory implements OnDestroy {
  private readonly api = inject(InventoryApiService);
  private readonly sessions = inject(AuthSessionService);
  private loadSubscription: Subscription | null = null;
  private saveSubscription: Subscription | null = null;

  protected readonly status = signal<InventoryStatus>('loading');
  protected readonly stocks = signal<readonly StockLevel[]>([]);
  protected readonly movements = signal<readonly InventoryMovement[]>([]);
  protected readonly selectedProductId = signal('');
  protected readonly movementType = signal<InventoryMovementType>('ENTRY');
  protected readonly quantity = signal('');
  protected readonly pending = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly canWrite = computed(() =>
    hasCompanyPermission(this.sessions.session()?.activeCompany?.role, 'INVENTORY_WRITE'),
  );

  constructor() {
    effect(() => {
      const companyId = this.sessions.session()?.activeCompany?.id ?? null;
      untracked(() => {
        this.loadSubscription?.unsubscribe();
        this.saveSubscription?.unsubscribe();
        this.stocks.set([]);
        this.movements.set([]);
        this.selectedProductId.set('');
        this.errorMessage.set(null);
        this.successMessage.set(null);
        if (companyId === null) {
          this.status.set('success');
          return;
        }
        this.load(companyId);
      });
    });
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
    this.saveSubscription?.unsubscribe();
  }

  protected updateProduct(event: Event): void {
    this.selectedProductId.set((event.target as HTMLSelectElement).value);
  }

  protected updateType(event: Event): void {
    this.movementType.set((event.target as HTMLSelectElement).value as InventoryMovementType);
  }

  protected updateQuantity(event: Event): void {
    this.quantity.set((event.target as HTMLInputElement).value);
  }

  protected register(event: Event): void {
    event.preventDefault();
    const companyId = this.sessions.session()?.activeCompany?.id;
    if (!companyId || !this.canWrite() || this.pending()) return;
    if (!this.selectedProductId() || !this.validQuantity(this.quantity())) {
      this.errorMessage.set('Selecciona un producto e indica una cantidad positiva válida.');
      return;
    }
    this.pending.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.saveSubscription = this.api
      .registerMovement({
        productId: this.selectedProductId(),
        type: this.movementType(),
        quantity: this.quantity(),
      })
      .subscribe({
        next: () => {
          if (this.sessions.session()?.activeCompany?.id !== companyId) return;
          this.pending.set(false);
          this.quantity.set('');
          this.successMessage.set('Movimiento registrado correctamente.');
          this.load(companyId);
        },
        error: (error: unknown) => {
          if (this.sessions.session()?.activeCompany?.id !== companyId) return;
          this.pending.set(false);
          this.errorMessage.set(this.safeMessage(error));
        },
      });
  }

  protected retry(): void {
    const companyId = this.sessions.session()?.activeCompany?.id;
    if (companyId) this.load(companyId);
  }

  protected typeLabel(type: InventoryMovementType): string {
    return type === 'ENTRY' ? 'Entrada' : 'Salida';
  }

  private load(companyId: string): void {
    this.loadSubscription?.unsubscribe();
    this.status.set('loading');
    this.loadSubscription = this.api.getInventory().subscribe({
      next: ({ stocks, movements }) => {
        if (this.sessions.session()?.activeCompany?.id !== companyId) return;
        this.stocks.set(stocks);
        this.movements.set(movements);
        this.status.set('success');
      },
      error: (error: unknown) => {
        if (this.sessions.session()?.activeCompany?.id !== companyId) return;
        this.errorMessage.set(this.safeMessage(error));
        this.status.set('error');
      },
    });
  }

  private validQuantity(value: string): boolean {
    return /^\d{1,15}(\.\d{1,4})?$/.test(value) && Number(value) > 0;
  }

  private safeMessage(error: unknown): string {
    return error instanceof InventoryRequestError
      ? error.message
      : 'No se pudo completar la operación de inventario.';
  }
}
