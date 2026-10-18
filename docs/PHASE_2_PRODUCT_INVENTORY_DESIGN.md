# Phase 2: Products, inventory and multi-company foundation

## Status

This document records the approved contracts and architectural decisions for Phase 2. The current
company and product foundations are implemented. Authentication HTTP endpoints, sessions, inventory
and public business endpoints are not part of the current identity persistence iteration.

## Architectural boundaries

Nexo ERP remains a modular monolith. Each business module separates API, application, domain and
infrastructure concerns. Persistence entities are not exposed through HTTP.

All company-owned operations will obtain their company identifier through `CompanyContext`. A
controller must never build that context from a request body, query parameter or client-controlled
header. Until authentication and company memberships exist, `NoAuthenticatedCompanyContext` fails
closed and no public product or inventory API will be created.

The `companies` table is additive. Migration V2 does not modify, delete or assign existing ETL
executions. Deciding how ETL history becomes company-owned is deferred to a later migration.

## Company contract

A company has a UUID, a validated name, an ISO 4217 default currency, a status, and creation and
modification timestamps. Supported statuses are `ACTIVE` and `SUSPENDED`. No seed company is
created, because an implicit tenant would conceal the absence of authentication.

## Product decisions

- Categories start as validated text rather than a separate aggregate.
- SKU comparison is company-scoped, trimmed, normalized to uppercase and unique even when a
  product is inactive.
- Prices use Java `BigDecimal` and PostgreSQL `NUMERIC(19,4)`. They are never represented by binary
  floating-point values.
- API monetary values will use decimal strings to avoid JavaScript precision loss.
- Currency comes from the company default currency.
- Quantities use `NUMERIC(19,4)` to support fractional units.
- Units begin with `UNIT`, `KG`, `G`, `L`, `ML`, `M`, `M2` and `HOUR`, and may be extended through
  reviewed application and database changes.
- Migration V3 persists the company currency with each product price. Commands cannot select this
  currency: the application copies it from the current active company. This preserves the historical
  meaning of existing prices if the company default changes later.
- Product lookup and uniqueness methods always require the company resolved by `CompanyContext`.
- Optimistic versioning rejects stale updates rather than silently overwriting newer data.
- The database unique constraint `(company_id, normalized_sku)` remains the final protection against
  concurrent duplicate creation, including when the existing product is inactive.

## Inventory decisions

Inventory will use one current balance per product plus an immutable movement ledger:

- An entry adds a positive quantity.
- An exit subtracts a positive quantity and cannot produce a negative balance.
- An adjustment sets an absolute counted balance and records the calculated difference and reason.
- Each operation locks the balance row with `PESSIMISTIC_WRITE`, validates the rule, updates the
  balance and inserts the movement in one transaction.
- Movement rows are never edited or deleted; corrections create compensating movements.

Idempotency is company-scoped. Repeating the same key and normalized command returns the result of
the original operation without inserting another movement. Reusing the key with different content
returns an HTTP 409 conflict. The persisted operation must therefore include an idempotency key and
a deterministic request fingerprint covered by a unique company-scoped constraint.

## Future warehouses

The first version has one balance per product. To introduce warehouses later without rewriting the
movement ledger:

1. Add a company-owned `warehouses` table.
2. Add `warehouse_id` to balances and movements.
3. Change the balance key from `product_id` to `(warehouse_id, product_id)`.
4. Migrate every existing balance and movement to one default warehouse per company.
5. Represent transfers as two linked immutable movements: an exit from the source and an entry into
   the destination, committed in one transaction with both balance rows locked in stable UUID order.

## Future CSV import

Product import will be a confirmable workflow: upload, column mapping, row validation, preview,
explicit confirmation and result reporting. Preview never mutates business data. Confirmed imports
are company-scoped and idempotent. Imported stock changes create inventory movements instead of
editing balances directly.

## Verification strategy

The company domain has unit tests, persistence is covered with a transactional repository test, and
the unauthenticated context has a fail-closed test. True cross-company isolation tests will accompany
the first company-owned aggregate, because the company root itself is not tenant-owned.

Inventory will add PostgreSQL Testcontainers tests for numeric behavior, row locking and concurrent
stock exits. H2 remains useful for fast application-context tests but is not considered sufficient to
prove PostgreSQL concurrency behavior.

## Identity and membership foundation

Migration V4 adds users and company memberships without changing V1-V3 or assigning legacy ETL data.
Email addresses are trimmed and lower-cased before lookup and have a database-enforced unique key.
Passwords are stored only as BCrypt hashes (cost 12); raw passwords are accepted internally only long
enough to hash them and are limited to 72 UTF-8 bytes so BCrypt never truncates them silently.

Registration is one transaction that creates an active user, an active company and an active OWNER
membership. Roles are `OWNER`, `ADMIN`, `INVENTORY_MANAGER` and `VIEWER`; membership status is
independent from user and company status. A future authenticated request must revalidate all three,
so disabling a user, suspending a membership or suspending a company invalidates effective access even
when a server-side session already exists.

The active company will be selected through a membership-validated server-side session. A company id
sent by a client is never authorization evidence. When company selection changes, the Angular client
must clear all company-scoped in-memory state before refetching it.

Spring Security, JDBC sessions, controllers and CSRF configuration are intentionally deferred. When
introduced, every endpoint will be denied by default except an explicit public allowlist. CSRF remains
enabled, including multipart CSV requests. Global ETL history must not be exposed to business users
until a later additive migration assigns new executions to companies while preserving legacy rows.

Email verification, password recovery and production-grade abuse protection are release requirements,
not optional post-launch work.
