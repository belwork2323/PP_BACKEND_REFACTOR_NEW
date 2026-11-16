# Repository Guidelines

## Project Structure & Module Organization

This is the `acempp` Spring Boot backend for propellant-processing workflows. The application entry point is `src/main/java/in/co/bel/acempp/AcemApplication.java`. REST controllers are grouped by audience and workflow under `controller/` (admin, approver, system manager, manufacturing, dispatch, sourcing, and quality control). Business logic lives in matching `service/` packages; MongoDB documents and Spring Data repositories are under `entity/` and `repository/`. Request/response contracts are organized under `dto/` by workflow. Cross-cutting concerns are in `security/` (stateless JWT), `validation/`, `exception/`, `audit/`, `config/`, and `util/`.

The main workflow spans raw-material preparation and sourcing, subscale processing, mixing, casting/curing, case preparation, trimming, post-cure, dispatch, and QC/STF/NDT/RMC operations. Batch progression is coordinated by `BatchStageOrchestrationService`, with status/progress logic shared by batch services and dashboards. File upload/download and attachment binding are centralized in `FileSystemService` and `FileBindingService`.

## Build, Test, and Development Commands

The project targets Java 21 and uses the checked-in Maven wrapper:

- `./mvnw clean package` — compile and package the Spring Boot application.
- `./mvnw test` — run the test suite.
- `./mvnw -Dtest=AcemApplicationTests test` — run the current context-load test alone.
- `./mvnw spring-boot:run` — start the application locally.

MongoDB, JWT, server, and file-upload settings are read from `src/main/resources/application.properties`; use environment/local overrides for machine-specific values and never copy credentials into new documentation or commits.

## Coding Style & Naming Conventions

Use the existing package root `in.co.bel.acempp` and lowercase package names. Follow the surrounding Spring conventions: controllers expose DTO-based endpoints, services contain workflow rules, repositories contain persistence queries, and entities map MongoDB collections with `@Document`. Lombok is enabled through Maven annotation processing. No repository formatter, linter, or Checkstyle configuration was found, so preserve the formatting and naming style of the files being changed.

Security is stateless JWT. Public authentication routes are explicitly listed in `SecurityConfig`/`JwtFilter`; other routes require authentication, and authorities are currently matched without an automatic `ROLE_` prefix.

## Testing Guidelines

Tests use JUnit 5 and Spring Boot’s test starter. The repository currently contains a single `@SpringBootTest` context-load test; add focused tests alongside the package under test and run the narrow Maven test command while iterating.

## Commit & Pull Request Guidelines

Recent commits use short, descriptive summaries focused on a workflow or fix (for example, batch statistics, QC/file handling, and approval-status changes). Keep commits similarly focused and call out API, status, or persistence changes explicitly. No PR template or additional review configuration was found.
