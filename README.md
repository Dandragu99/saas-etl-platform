# SaaS ETL Platform

A full-stack platform for previewing and transforming CSV files, built with **Spring Boot** and **Angular**.

The project currently allows users to upload a CSV file, inspect its structure, remove a selected column from the preview, and generate a complete transformed CSV file from the backend.

## Current features

* Upload a CSV file of up to 5 MiB.
* Validate file extension, size, encoding, headers and row structure.
* Preview the first 20 rows.
* Preserve the original column order.
* Support UTF-8 and UTF-8 BOM input.
* Handle quoted values, commas, empty fields and multiline fields.
* Select and remove a column from the preview.
* Keep the original and transformed previews visible separately.
* Generate and download the complete transformed CSV through the backend.
* Persist complete-download executions and their status in PostgreSQL, isolated by active company.
* Filter and paginate the execution history.
* Display an ETL dashboard with summary cards, responsive history table and refresh controls.
* Return consistent and safe API errors.
* Process the complete CSV sequentially without storing every row in memory.
* Register an owner together with the first company.
* Authenticate with a server-side, PostgreSQL-backed session and an HttpOnly cookie.
* Select an authorized active company without trusting a client-provided company context.
* Protect Angular routes and clear company-scoped client state when the active company changes.

## Technology stack

### Backend

* Java 21
* Spring Boot 4.1
* Spring Web MVC
* Spring Data JPA
* PostgreSQL
* Flyway
* Apache Commons CSV
* Maven
* JUnit
* Mockito
* MockMvc

### Frontend

* Angular 22
* TypeScript 6
* Standalone components
* Signals
* Angular HttpClient
* CSS
* Vitest

## Project structure

```text
saas-etl-platform/
├── backend/
│   ├── src/main/java/
│   ├── src/test/java/
│   ├── pom.xml
│   └── mvnw.cmd
├── frontend/
│   ├── src/
│   ├── angular.json
│   ├── package.json
│   └── proxy.conf.json
├── docs/
│   ├── PROJECT_SPEC.md
│   └── API_CONTRACT.md
├── docker-compose.yml
├── AGENTS.md
└── README.md
```

## Architecture

The backend is divided into four main layers:

```text
API
  ↓
Application
  ↓
Domain
  ↓
Infrastructure
```

### API

Receives HTTP requests and converts application results into JSON or downloadable CSV responses.

### Application

Coordinates use cases, file validation, stream ownership and error translation.

### Domain

Contains pure transformation logic, independent of Spring, HTTP and Apache Commons CSV.

### Infrastructure

Handles CSV parsing, sequential reading, CSV writing and PostgreSQL persistence through a JPA adapter. The in-memory repository remains available only for focused unit tests.

The Angular frontend separates:

* Visual components.
* HTTP services.
* Typed API models.
* Reusable tabular preview components.
* Independent preview and transformation states.

## API endpoints

### Health check

```http
GET /api/health
```

Example response:

```json
{
  "status": "UP",
  "application": "saas-etl-backend"
}
```

### Preview CSV

```http
POST /api/csv/preview
Content-Type: multipart/form-data
```

Multipart field:

```text
file
```

Example response:

```json
{
  "fileName": "clientes.csv",
  "columns": ["id", "nombre", "email"],
  "rows": [
    {
      "id": "1",
      "nombre": "Ana",
      "email": "ana@example.com"
    }
  ],
  "previewRowCount": 1,
  "truncated": false
}
```

### Preview CSV without a column

```http
POST /api/csv/transform/remove-column
Content-Type: multipart/form-data
```

Multipart fields:

```text
file
column
```

Example response:

```json
{
  "fileName": "clientes.csv",
  "columns": ["id", "nombre"],
  "rows": [
    {
      "id": "1",
      "nombre": "Ana"
    }
  ],
  "previewRowCount": 1,
  "truncated": false,
  "transformation": {
    "type": "REMOVE_COLUMN",
    "removedColumn": "email"
  }
}
```

### Download complete transformed CSV

```http
POST /api/csv/transform/remove-column/download
Content-Type: multipart/form-data
```

Multipart fields:

```text
file
column
```

Successful responses return:

```text
Content-Type: text/csv;charset=UTF-8
Content-Disposition: attachment
```

Example result:

```csv
id,nombre
1,Ana
2,Carlos
```

The backend validates the complete input before returning the downloadable file, preventing partial CSV responses when an error is found.

### List ETL execution history

```http
GET /api/etl/executions
```

Example response:

```json
[
  {
    "id": "ca5b9df0-28ae-4cd1-859a-075a28297d97",
    "type": "REMOVE_COLUMN",
    "status": "SUCCESS",
    "startedAt": "2026-10-09T10:00:00Z",
    "finishedAt": "2026-10-09T10:00:01Z",
    "processedRecordCount": 25,
    "errorMessage": null
  }
]
```

The response body remains an array for backwards compatibility. Optional query parameters are `status`, `startedFrom`, `startedTo`, `page` and `size` (maximum 100). Pagination metadata is returned in `X-Total-Count`, `X-Page`, `X-Page-Size` and `X-Total-Pages` headers.

Summary cards use:

```http
GET /api/etl/executions/summary
```

The history is persisted in PostgreSQL. It stores operational metadata only. CSV content, file names, selected column names, exception details and stack traces are never stored.

An execution can move through `PENDING`, `RUNNING`, `SUCCESS` and `FAILED`. Validation, parsing and generation failures raised while the application service is running can be registered as `FAILED`. Multipart failures that occur before the controller opens the uploaded stream are not registered. Once the fully generated byte array has been returned to Spring, network disconnections or servlet response-writing failures cannot be detected reliably by this service, so the history may remain `SUCCESS` even if the client does not receive the complete response.

## Requirements

Install the following tools:

* Java 21
* Node.js 24
* npm 11
* Git
* Docker Desktop (recommended for local PostgreSQL)

The Maven Wrapper is included, so a global Maven installation is not required.

## Running the backend

Start PostgreSQL from the project root:

```powershell
$env:DATABASE_PASSWORD = "choose-a-local-password"
docker compose up -d postgres
docker compose ps
```

Flyway applies pending migrations automatically when the backend starts. Then run:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The backend will be available at:

```text
http://localhost:8080
```

The health endpoint is public. Business endpoints require an authenticated session:

```powershell
curl.exe http://localhost:8080/api/health
```

The ETL history endpoints return only records owned by the active, authorized company. Records
created before the multi-company migration remain preserved but hidden until manually reviewed.

The local database and username default to `saas_etl`. The password is deliberately required through an environment variable and is not stored in Git. You can also override every database setting without editing tracked files:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/saas_etl"
$env:DATABASE_USERNAME = "saas_etl"
$env:DATABASE_PASSWORD = "choose-a-local-password"
```

To stop PostgreSQL while preserving data:

```powershell
docker compose stop postgres
```

To remove the container and its local database volume deliberately:

```powershell
docker compose down --volumes
```

## Running the frontend

Open another terminal:

```powershell
cd frontend
npm.cmd install
npm.cmd start
```

The frontend will be available at:

```text
http://localhost:4200
```

During development, Angular forwards `/api` requests to the backend through `proxy.conf.json`.

Open `http://localhost:4200/registro`, create the first account and company, and then sign in at
`http://localhost:4200/login`. The Angular client obtains a CSRF cookie before every state-changing
authentication request. The session identifier stays in the `NEXO_SESSION` HttpOnly cookie and is
never stored in browser storage.

After login, the top bar shows the active company. Changing it asks the backend to authorize the
membership and then reloads the application at `/dashboard`, preventing state from the previous
company from remaining in Angular memory.

## Running tests

### Backend

Run the complete Maven lifecycle. Docker must be running for the PostgreSQL Testcontainers tests:

```powershell
cd backend
.\mvnw.cmd verify
```

### Frontend

Run the frontend unit suite and production build:

```powershell
cd frontend
npm.cmd test -- --watch=false
npm.cmd run build
npm.cmd audit
```

## Example using curl

Preview a CSV:

```powershell
curl.exe -X POST `
  -F "file=@C:\path\to\clientes.csv" `
  http://localhost:8080/api/csv/preview
```

Preview the CSV without the `email` column:

```powershell
curl.exe -X POST `
  -F "file=@C:\path\to\clientes.csv" `
  -F "column=email" `
  http://localhost:8080/api/csv/transform/remove-column
```

Download the complete transformed CSV:

```powershell
curl.exe -X POST `
  -F "file=@C:\path\to\clientes.csv" `
  -F "column=email" `
  http://localhost:8080/api/csv/transform/remove-column/download `
  --output clientes-sin-email.csv
```

Inspect the execution history:

```powershell
curl.exe http://localhost:8080/api/etl/executions
```

The same history is displayed at the bottom of the Angular page. Successful, failed, running and pending executions have different visual states. The duration and processed-record count are shown when the backend provides enough information to calculate them.

## CSV validation

The backend currently validates:

* Required file.
* Non-empty file.
* Maximum size of 5 MiB.
* `.csv` extension, case-insensitive.
* Strict UTF-8 encoding.
* Optional UTF-8 BOM.
* Required headers.
* Non-empty and unique headers.
* Consistent number of fields in every row.
* Valid CSV quoting and escaping.
* Exact and case-sensitive column selection.

## Error format

API errors use a common structure:

```json
{
  "code": "CSV_COLUMN_NOT_FOUND",
  "message": "La columna indicada no existe en el archivo CSV.",
  "status": 422,
  "path": "/api/csv/transform/remove-column",
  "timestamp": "2026-07-22T17:31:58Z"
}
```

Technical exceptions and stack traces are not exposed to API clients.

## Current limitations

* Only the remove-column transformation is available.
* Transformations cannot yet be chained.
* Files and results are not persisted.
* HTTP transmission failures after CSV generation cannot be reflected reliably in the execution status.
* Email verification and password recovery are not implemented yet and are required before public launch.
* Registration/login abuse protection and production rate limiting are not implemented yet.
* ETL executions created before V6 remain hidden while `company_id` is null; recovery is a reviewed administrative process.
* The authenticated ETL transformation endpoints are not yet company-owned; they remain compatible but do not persist uploaded business data.
* PostgreSQL credentials in `docker-compose.yml` are development-only defaults and must be overridden outside local development.

## Roadmap

Planned improvements include:

* Add more CSV transformations.
* Support chained transformations.
* Improve frontend validation and user feedback.
* Add end-to-end tests.
* Deploy the backend and frontend.
* Add email verification, password recovery and abuse protection.
* Review legacy ETL records only when reliable ownership evidence is available.

## Documentation

More detailed specifications are available in:

* `docs/PROJECT_SPEC.md`
* `docs/API_CONTRACT.md`
* `docs/ETL_HISTORY_RECOVERY.md`

## Repository

Created as a portfolio project to practise:

* Clean backend architecture.
* REST API design.
* Multipart file handling.
* Sequential CSV processing.
* Defensive programming.
* Angular standalone components.
* Reactive UI state with signals.
* Automated testing.
* Git and incremental delivery.

## Author

**Danut Dragu**

GitHub: [Dandragu99](https://github.com/Dandragu99)
