export type WorkStatus = 'PENDING' | 'ACTIVE' | 'DONE';
export type QuoteStatus = 'DRAFT' | 'SENT' | 'ACCEPTED';

export interface MiniClient {
  id: string;
  name: string;
  taxId: string;
  phone: string;
  email: string;
  address: string;
  createdAt: string;
}

export interface MiniQuoteLine {
  description: string;
  kind: 'MATERIAL' | 'LABOR';
  quantity: number;
  unitPrice: number;
}

export interface MiniQuote {
  id: string;
  number: string;
  clientId: string;
  clientName: string;
  date: string;
  status: QuoteStatus;
  taxRate: number;
  notes: string;
  lines: readonly MiniQuoteLine[];
  subtotal: number;
  tax: number;
  total: number;
}

export interface MiniWork {
  id: string;
  title: string;
  clientName: string;
  date: string;
  status: WorkStatus;
}

export interface MiniSettings {
  id: 'business';
  tradeName: string;
  taxId: string;
  address: string;
  phone: string;
  email: string;
  logoDataUrl: string;
}

export const DEFAULT_SETTINGS: MiniSettings = {
  id: 'business',
  tradeName: 'Voltia Instalaciones',
  taxId: '12345678A',
  address: 'Calle Mayor 24, Madrid',
  phone: '600 123 456',
  email: 'hola@voltia.es',
  logoDataUrl: '',
};

export const miniMoney = (value: number): string =>
  new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(value);
