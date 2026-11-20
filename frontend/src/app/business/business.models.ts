export interface BusinessDashboard {
  customers: number;
  activeWorks: number;
  quotes: number;
  invoices: number;
  pendingRevenue: number;
}

export interface Customer {
  id: string;
  name: string;
  taxId?: string;
  email?: string;
  phone?: string;
  address?: string;
  notes?: string;
}

export interface DocumentLine {
  position: number;
  description: string;
  quantity: number;
  unitPrice: number;
  taxRate: number;
  subtotal: number;
  tax: number;
  total: number;
}

export interface Quote {
  id: string;
  customerId: string;
  customerName: string;
  number: string;
  status: string;
  issueDate: string;
  validUntil: string;
  notes?: string;
  subtotal: number;
  taxTotal: number;
  total: number;
  lines: readonly DocumentLine[];
}

export interface Invoice {
  id: string;
  customerId: string;
  customerName: string;
  quoteId?: string;
  number: string;
  status: string;
  issueDate: string;
  dueDate: string;
  total: number;
  lines: readonly DocumentLine[];
}

export interface CustomerInput {
  name: string;
  taxId: string;
  email: string;
  phone: string;
  address: string;
  notes: string;
}

export interface QuoteInput {
  customerId: string;
  issueDate: string;
  validUntil: string;
  notes: string;
  lines: readonly { description: string; quantity: number; unitPrice: number; taxRate: number }[];
}
