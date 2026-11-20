import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { BusinessApiService } from './business-api.service';

describe('BusinessApiService', () => {
  let api: BusinessApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(BusinessApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the company-scoped workspace endpoints', () => {
    api.loadWorkspace().subscribe((value) => expect(value.customers).toEqual([]));

    http.expectOne('/api/business/dashboard').flush({ customers: 0, activeWorks: 0, quotes: 0, invoices: 0, pendingRevenue: 0 });
    http.expectOne('/api/business/customers').flush([]);
    http.expectOne('/api/business/quotes').flush([]);
    http.expectOne('/api/business/invoices').flush([]);
  });

  it('obtains CSRF before creating a customer', () => {
    const request = { name: 'Cliente', taxId: '', email: '', phone: '', address: '', notes: '' };
    api.createCustomer(request).subscribe((customer) => expect(customer.name).toBe('Cliente'));

    http.expectOne('/api/auth/csrf').flush({});
    const create = http.expectOne('/api/business/customers');
    expect(create.request.method).toBe('POST');
    create.flush({ id: '1', ...request });
  });
});
