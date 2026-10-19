export type MembershipRole = 'OWNER' | 'ADMIN' | 'INVENTORY_MANAGER' | 'VIEWER';

export interface CompanyAccess {
  readonly id: string;
  readonly name: string;
  readonly defaultCurrency: string;
  readonly role: MembershipRole;
}

export interface AuthSession {
  readonly userId: string;
  readonly email: string;
  readonly activeCompany: CompanyAccess | null;
}

export interface LoginRequest {
  readonly email: string;
  readonly password: string;
}

export interface RegistrationRequest extends LoginRequest {
  readonly companyName: string;
  readonly defaultCurrency: string;
}

export interface RegistrationResponse {
  readonly userId: string;
  readonly companyId: string;
}

export interface ApiErrorResponse {
  readonly code: string;
  readonly message: string;
  readonly status: number;
  readonly path: string;
  readonly timestamp: string;
}

export class AuthRequestError extends Error {
  constructor(
    readonly code: string,
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = 'AuthRequestError';
  }
}
