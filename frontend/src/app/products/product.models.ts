export type UnitOfMeasure = 'UNIT' | 'KG' | 'G' | 'L' | 'ML' | 'M' | 'M2' | 'HOUR';

export type ProductSort = 'name' | 'sku' | 'category' | 'price' | 'created_at' | 'updated_at';
export type SortDirection = 'asc' | 'desc';

export interface Product {
  readonly id: string;
  readonly sku: string;
  readonly name: string;
  readonly description: string | null;
  readonly category: string;
  readonly price: string;
  readonly currency: string;
  readonly unitOfMeasure: UnitOfMeasure;
  readonly active: boolean;
  readonly version: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ProductPage {
  readonly content: readonly Product[];
  readonly totalElements: number;
  readonly totalPages: number;
  readonly page: number;
  readonly size: number;
}

export interface ProductQuery {
  readonly search?: string;
  readonly name?: string;
  readonly sku?: string;
  readonly category?: string;
  readonly active?: boolean;
  readonly page: number;
  readonly size: number;
  readonly sort: ProductSort;
  readonly direction: SortDirection;
}

export interface ProductValues {
  readonly sku: string;
  readonly name: string;
  readonly description: string | null;
  readonly category: string;
  readonly price: string;
  readonly unitOfMeasure: UnitOfMeasure;
}

export type CreateProductRequest = ProductValues;

export interface UpdateProductRequest extends ProductValues {
  readonly expectedVersion: number;
}

export interface ChangeProductStatusRequest {
  readonly active: boolean;
  readonly expectedVersion: number;
}

export class ProductRequestError extends Error {
  constructor(
    message: string,
    readonly code = 'PRODUCT_REQUEST_FAILED',
    readonly status = 0,
  ) {
    super(message);
    this.name = 'ProductRequestError';
  }
}
