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
* Return consistent and safe API errors.
* Process the complete CSV sequentially without storing every row in memory.

## Technology stack

### Backend

* Java 21
* Spring Boot 4.1
* Spring Web MVC
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

Handles CSV parsing, sequential reading and CSV writing.

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

## Requirements

Install the following tools:

* Java 21
* Node.js 24
* npm 11
* Git

The Maven Wrapper is included, so a global Maven installation is not required.

## Running the backend

From the project root:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The backend will be available at:

```text
http://localhost:8080
```

Verify it with:

```powershell
curl.exe http://localhost:8080/api/health
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

## Running tests

### Backend

```powershell
cd backend
.\mvnw.cmd test
```

Current backend test suite:

```text
140 tests
0 failures
0 errors
```

Run the complete Maven verification lifecycle:

```powershell
.\mvnw.cmd verify
```

### Frontend

```powershell
cd frontend
npm.cmd test -- --watch=false
```

Current frontend test suite:

```text
23 tests
0 failures
```

Create a production build:

```powershell
npm.cmd run build
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

* The frontend does not yet expose the complete CSV download endpoint.
* Only the remove-column transformation is available.
* Transformations cannot yet be chained.
* Files and results are not persisted.
* There is no authentication or user account system.
* There is no deployment configuration yet.

## Roadmap

Planned improvements include:

* Add the download button to the Angular frontend.
* Add more CSV transformations.
* Support chained transformations.
* Improve frontend validation and user feedback.
* Add end-to-end tests.
* Add Docker configuration.
* Deploy the backend and frontend.
* Add authentication and saved transformation jobs.

## Documentation

More detailed specifications are available in:

* `docs/PROJECT_SPEC.md`
* `docs/API_CONTRACT.md`

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
