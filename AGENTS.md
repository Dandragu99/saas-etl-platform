# AGENTS.md

## Project

This repository contains a SaaS platform for running ETL transformations on user-uploaded data files.

Initial stack:

- Backend: Java with Spring Boot
- Frontend: Angular with TypeScript
- Database: PostgreSQL
- Infrastructure: Docker Compose
- Version control: Git

The initial supported file format will be CSV.

## Working method

Before modifying code:

1. Inspect the relevant files.
2. Explain the current state.
3. Propose a short implementation plan.
4. Identify the files that would be created or modified.
5. Wait for explicit approval when the task involves an architectural decision or a large change.

Do not implement unrelated improvements.

Keep each task small, focused and reviewable.

## User learning objective

The repository owner is a junior developer learning Java, Spring Boot, Angular and software architecture.

After every implementation:

- Explain what changed.
- Explain why it was implemented that way.
- Mention the main classes, methods and files involved.
- Point out concepts the repository owner should understand.
- Avoid unnecessarily complex abstractions.
- Prefer readable and explicit code over clever code.

Do not generate large amounts of code without explaining them.

## Architecture

Use a modular monolith for the initial MVP.

Do not introduce microservices, Kubernetes, Kafka, Redis or additional infrastructure unless explicitly requested and justified by a real requirement.

Backend code should maintain clear separation between:

- API or controller layer
- Application or service layer
- Domain logic
- Persistence or repository layer
- Infrastructure concerns

Do not expose persistence entities directly through the API.

Use DTOs at application boundaries.

## Backend standards

- Use a supported Java LTS version.
- Use Spring Boot.
- Use Maven unless the repository explicitly changes this decision.
- Prefer constructor injection.
- Avoid field injection.
- Use meaningful names.
- Keep methods focused.
- Validate external input.
- Handle errors explicitly.
- Do not silently catch exceptions.
- Add tests for important business logic.
- Do not add Lombok initially unless explicitly approved.

## Frontend standards

- Use Angular with TypeScript.
- Use standalone components.
- Use strict TypeScript settings.
- Keep API access outside presentation components.
- Avoid using `any`.
- Keep components small.
- Separate presentation, application state and API communication.
- Add tests for important logic when appropriate.

## Database standards

- Use PostgreSQL.
- Database schema changes must use migrations.
- Do not rely on automatic destructive schema updates.
- Use clear table and column names.
- Do not store uploaded files directly in PostgreSQL unless explicitly decided.

## Security

- Never commit secrets, passwords, API keys or tokens.
- Use environment variables for configuration.
- Validate uploaded filenames, MIME types and file sizes.
- Treat all uploaded data as untrusted input.
- Do not implement custom cryptography.

## Git

- Do not commit or push unless explicitly requested.
- Do not modify `.gitignore` without checking whether existing entries should be preserved.
- Do not combine unrelated changes.
- Before proposing a commit, summarize the changed files and verification performed.

## Verification

After modifying code, run the most relevant available checks.

Backend checks may include:

```bash
mvn test
mvn verify
```

Frontend checks may include:

```bash
npm test
npm run build
```

If a command fails:

- Report the failure.
- Explain the likely cause.
- Do not claim the task is complete.
- Fix it only when it falls within the approved scope.

## Response format

At the end of every coding task, provide:

1. Summary
2. Files changed
3. Important implementation decisions
4. Verification performed
5. Remaining limitations
6. What the repository owner should review and understand