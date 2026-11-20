import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { MiniDatabaseService } from './mini-database.service';
import { DEFAULT_SETTINGS, MiniClient, MiniQuote, MiniWork } from './mini.models';
import { MiniStoreService } from './mini-store.service';

describe('MiniStoreService', () => {
  const client: MiniClient = {
    id: 'c1',
    name: 'Cliente local',
    taxId: '',
    phone: '600',
    email: '',
    address: '',
    createdAt: '2026-11-20',
  };
  const quote: MiniQuote = {
    id: 'q1',
    number: 'PRE-001',
    clientId: 'c1',
    clientName: 'Cliente local',
    date: '2026-11-20',
    status: 'DRAFT',
    taxRate: 21,
    notes: '',
    lines: [],
    subtotal: 0,
    tax: 0,
    total: 0,
  };
  const work: MiniWork = {
    id: 'w1',
    title: 'Trabajo',
    clientName: 'Cliente local',
    date: '2026-11-21',
    status: 'PENDING',
  };
  const database = {
    all: vi.fn((store: string) =>
      Promise.resolve(store === 'clients' ? [client] : store === 'quotes' ? [quote] : [work]),
    ),
    get: vi.fn(() => Promise.resolve(DEFAULT_SETTINGS)),
    put: vi.fn(() => Promise.resolve()),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [{ provide: MiniDatabaseService, useValue: database }],
    });
  });

  it('loads persisted business data instead of relying on in-memory fixtures', async () => {
    const store = TestBed.inject(MiniStoreService);
    await store.load();
    expect(store.clients()).toEqual([client]);
    expect(store.quotes()).toEqual([quote]);
    expect(store.pendingWorks()).toEqual([work]);
    expect(database.all).toHaveBeenCalledWith('clients');
  });

  it('writes a client to IndexedDB and refreshes the signals', async () => {
    const store = TestBed.inject(MiniStoreService);
    await store.saveClient(client);
    expect(database.put).toHaveBeenCalledWith('clients', client);
    expect(store.clients()).toEqual([client]);
  });
});
