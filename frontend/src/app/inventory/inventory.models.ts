import { UnitOfMeasure } from '../products/product.models';

export type InventoryMovementType = 'ENTRY' | 'EXIT';

export interface StockLevel {
  readonly productId: string;
  readonly productSku: string;
  readonly productName: string;
  readonly unitOfMeasure: UnitOfMeasure;
  readonly quantity: string;
  readonly updatedAt: string;
}

export interface InventoryMovement {
  readonly id: string;
  readonly productId: string;
  readonly productSku: string;
  readonly productName: string;
  readonly type: InventoryMovementType;
  readonly quantity: string;
  readonly responsibleUserId: string;
  readonly responsibleUserEmail: string;
  readonly occurredAt: string;
}

export interface RegisterInventoryMovementRequest {
  readonly productId: string;
  readonly type: InventoryMovementType;
  readonly quantity: string;
}

export class InventoryRequestError extends Error {
  constructor(
    message: string,
    readonly code = 'INVENTORY_REQUEST_FAILED',
    readonly status = 0,
  ) {
    super(message);
    this.name = 'InventoryRequestError';
  }
}
