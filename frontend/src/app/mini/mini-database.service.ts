import { Injectable } from '@angular/core';

import { DEFAULT_SETTINGS, MiniClient, MiniQuote, MiniSettings, MiniWork } from './mini.models';

type StoreName = 'clients' | 'quotes' | 'works' | 'settings';

@Injectable({ providedIn: 'root' })
export class MiniDatabaseService {
  private readonly database = this.open();

  async all<T>(storeName: StoreName): Promise<readonly T[]> {
    const database = await this.database;
    return new Promise((resolve, reject) => {
      const request = database.transaction(storeName, 'readonly').objectStore(storeName).getAll();
      request.onsuccess = () => resolve(request.result as readonly T[]);
      request.onerror = () => reject(request.error);
    });
  }

  async get<T>(storeName: StoreName, id: string): Promise<T | undefined> {
    const database = await this.database;
    return new Promise((resolve, reject) => {
      const request = database.transaction(storeName, 'readonly').objectStore(storeName).get(id);
      request.onsuccess = () => resolve(request.result as T | undefined);
      request.onerror = () => reject(request.error);
    });
  }

  async put<T>(storeName: StoreName, value: T): Promise<void> {
    const database = await this.database;
    return new Promise((resolve, reject) => {
      const request = database
        .transaction(storeName, 'readwrite')
        .objectStore(storeName)
        .put(value);
      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
  }

  private open(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open('voltia-mini', 1);
      request.onupgradeneeded = () => {
        const database = request.result;
        for (const name of ['clients', 'quotes', 'works', 'settings'] as const) {
          if (!database.objectStoreNames.contains(name))
            database.createObjectStore(name, { keyPath: 'id' });
        }
      };
      request.onsuccess = async () => {
        resolve(request.result);
        await this.seed(request.result);
      };
      request.onerror = () => reject(request.error);
    });
  }

  private async seed(database: IDBDatabase): Promise<void> {
    const count = await new Promise<number>((resolve, reject) => {
      const request = database.transaction('clients', 'readonly').objectStore('clients').count();
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
    if (count > 0) return;

    const clients: readonly MiniClient[] = [
      {
        id: 'demo-client-1',
        name: 'Reformas Martínez',
        taxId: 'B12345678',
        phone: '600 123 456',
        email: 'obra@martinez.es',
        address: 'C/ Alcalá 214, Madrid',
        createdAt: '2026-11-18',
      },
      {
        id: 'demo-client-2',
        name: 'Clínica Dental Nova',
        taxId: 'B87654321',
        phone: '611 908 221',
        email: 'hola@clinicanova.es',
        address: 'Av. Europa 18, Getafe',
        createdAt: '2026-11-12',
      },
    ];
    const works: readonly MiniWork[] = [
      {
        id: 'demo-work-1',
        title: 'Instalación vivienda',
        clientName: 'Reformas Martínez',
        date: '2026-11-21',
        status: 'ACTIVE',
      },
      {
        id: 'demo-work-2',
        title: 'Iluminación clínica',
        clientName: 'Clínica Dental Nova',
        date: '2026-11-23',
        status: 'PENDING',
      },
    ];
    const quote: MiniQuote = {
      id: 'demo-quote-1',
      number: 'PRE-2026-001',
      clientId: clients[0].id,
      clientName: clients[0].name,
      date: '2026-11-20',
      status: 'SENT',
      taxRate: 21,
      notes: 'Ejemplo no válido para emisión fiscal.',
      lines: [
        {
          description: 'Renovación de cuadro eléctrico',
          kind: 'LABOR',
          quantity: 1,
          unitPrice: 680,
        },
      ],
      subtotal: 680,
      tax: 142.8,
      total: 822.8,
    };
    const transaction = database.transaction(
      ['clients', 'works', 'quotes', 'settings'],
      'readwrite',
    );
    clients.forEach((client) => transaction.objectStore('clients').put(client));
    works.forEach((work) => transaction.objectStore('works').put(work));
    transaction.objectStore('quotes').put(quote);
    transaction.objectStore('settings').put(DEFAULT_SETTINGS);
  }
}
