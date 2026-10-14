# Campus Hub Events Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a Vietnamese Campus Hub event portal with reliable event registration, PostgreSQL persistence, a public GitHub repository, and a verified VPS deployment.

**Architecture:** A single Spring Boot application exposes a REST API and serves a React/Vite build from the same origin. PostgreSQL is the system of record; Docker Compose runs the app and a private database on the VPS. GitHub Actions tests the project, publishes an image to GHCR, and deploys over a verified SSH key.

**Tech Stack:** Java/Spring Boot, Gradle, React/TypeScript/Vite, PostgreSQL, Flyway, Docker Compose, GitHub Actions, GHCR.

**Spec:** `docs/superpowers/specs/2026-10-14-campus-hub-events-design.md`

## Global Constraints

- UI is Vietnamese and responsive; list/search/filter, event detail, and registration are the first-release flows.
- PostgreSQL is authoritative; registration email is normalized and never returned by public API.
- Unique `(event_id, email_normalized)` and a transaction with event-row locking prevent duplicates and over-capacity registration.
- Do not publish the PostgreSQL port; preserve its named volume during app deployment.
- Do not commit credentials. Use SSH keys and GitHub Secrets; verify the VPS host key before deployment.
- Do not collect real attendee email over plain HTTP; use test data unless HTTPS is configured.
- VPS target supplied: `4.230.8.209`; inspect its OS, disk, existing services, ports and firewall before changing it.
- The Campus Hub Base source is absent from the workspace; report this honestly and do not claim a code delta against unavailable source.
- Public URL, successful deployment and screenshots are recorded only after live verification.

## Review Focus

- Invalid/oversized names, malformed email, blank values, and unknown event IDs return safe `400`/`404` responses without persistence.
- Email case/whitespace variants of the same address are duplicates; concurrent duplicate requests create only one registration.
- Simultaneous last-seat requests never exceed capacity; full and closed events return `409`.
- Event timestamps around timezone boundaries display consistently in UI and API.
- Database unavailable or constraint failures return safe errors without leaking SQL, stack traces, or email values.

---

### Task 1: Create the application skeleton and executable build

**Files:**
- Create: `settings.gradle`, `build.gradle`, `gradle/wrapper/*`, `.gitignore`
- Create: `src/main/java/vn/campushub/CampusHubApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `frontend/package.json`, `frontend/package-lock.json`, `frontend/vite.config.ts`, `frontend/tsconfig.json`
- Create: `frontend/src/main.tsx`, `frontend/src/App.tsx`, `frontend/index.html`
- Test: `src/test/java/vn/campushub/CampusHubApplicationTest.java`

**Interfaces:**
- Produces: Gradle `test` and `bootJar` tasks; frontend `build` task; one Spring Boot executable JAR.
- The frontend build output is copied into Spring's static-resource path by a Gradle task; the browser calls relative `/api/...` URLs.

- [ ] **Step 1: Write the failing application-context smoke test**
  - Add `applicationStarts()` using `@SpringBootTest`; assert the context loads with test profile and no external secrets.
- [ ] **Step 2: Run the test and confirm the missing application/build fails**
  - Run: `./gradlew test --tests vn.campushub.CampusHubApplicationTest`
  - Expected: FAIL because the wrapper/application has not been created.
- [ ] **Step 3: Create the Gradle/Spring and Vite skeleton**
  - Use Java 21, Gradle wrapper, Spring Web, Validation, Data JPA, PostgreSQL driver and Flyway; use current mutually compatible stable versions after checking official release documentation.
  - Add frontend build/copy task and production static-resource fallback for React routes.
- [ ] **Step 4: Run the smoke test and build**
  - Run: `./gradlew test bootJar`
  - Expected: PASS and exactly one runnable application JAR.
- [ ] **Step 5: Commit**
  - Commit message: `build: scaffold Campus Hub application`

### Task 2: Implement event discovery and safe API contracts

**Files:**
- Create: `src/main/java/vn/campushub/event/Event.java`
- Create: `src/main/java/vn/campushub/event/EventRepository.java`
- Create: `src/main/java/vn/campushub/event/EventService.java`
- Create: `src/main/java/vn/campushub/event/EventController.java`
- Create: `src/main/java/vn/campushub/event/dto/EventSummaryResponse.java`
- Create: `src/main/java/vn/campushub/event/dto/EventDetailResponse.java`
- Create: `src/main/java/vn/campushub/common/ApiExceptionHandler.java`
- Create: `src/main/resources/db/migration/V1__create_events.sql`
- Create: `src/main/resources/db/migration/V2__seed_events.sql`
- Test: `src/test/java/vn/campushub/event/EventControllerIntegrationTest.java`

**Interfaces:**
- Produces: `GET /api/events?query=&category=` and `GET /api/events/{id}`.
- Response fields: `id`, `title`, `category`, `description`, `location`, `startsAt`, `endsAt`, `capacity`, `registeredCount`, `remainingSeats`, `registrationOpen`; no registration PII.
- Produces stable JSON errors with `status`, `code`, and user-safe `message`.

- [ ] **Step 1: Add API tests for event list, query/category filters, details, and unknown IDs**
  - Assert seeded upcoming events are returned; filters combine; unknown UUID returns `404`; response JSON contains no email property.
- [ ] **Step 2: Run the tests and confirm they fail**
  - Run: `./gradlew test --tests vn.campushub.event.EventControllerIntegrationTest`
  - Expected: FAIL because endpoints and schema do not exist.
- [ ] **Step 3: Implement Event persistence, Flyway migrations, DTO mapping, and endpoints**
  - Keep repository access behind `EventService`; constrain list to upcoming events and support case-insensitive title/description search and category filtering.
- [ ] **Step 4: Run API tests**
  - Run: `./gradlew test --tests vn.campushub.event.EventControllerIntegrationTest`
  - Expected: PASS, including timezone-safe JSON and no PII.
- [ ] **Step 5: Commit**
  - Commit message: `feat: add event discovery API`

### Task 3: Implement validated, transactional registration

**Files:**
- Create: `src/main/java/vn/campushub/registration/Registration.java`
- Create: `src/main/java/vn/campushub/registration/RegistrationRepository.java`
- Create: `src/main/java/vn/campushub/registration/RegistrationService.java`
- Create: `src/main/java/vn/campushub/registration/RegistrationController.java`
- Create: `src/main/java/vn/campushub/registration/dto/CreateRegistrationRequest.java`
- Create: `src/main/java/vn/campushub/registration/dto/RegistrationConfirmationResponse.java`
- Create: `src/main/resources/db/migration/V3__create_registrations.sql`
- Test: `src/test/java/vn/campushub/registration/RegistrationServiceTest.java`
- Test: `src/test/java/vn/campushub/registration/RegistrationControllerIntegrationTest.java`
- Test: `src/test/java/vn/campushub/registration/RegistrationConcurrencyTest.java`
- Test: `src/test/java/vn/campushub/common/ApiExceptionHandlerTest.java`

**Interfaces:**
- Produces: `POST /api/events/{id}/registrations`, body `{ "fullName": "...", "email": "..." }`, status `201`, response `{ "registrationId": "...", "status": "CONFIRMED" }`.
- `RegistrationService.register(UUID eventId, String fullName, String email): RegistrationConfirmationResponse`.
- Maps unknown event to `404`; invalid input to `400`; duplicate/full/closed to `409`; infrastructure failures to safe `503`.

- [ ] **Step 1: Write failing service/API tests for valid registration, invalid data, missing event, duplicate, full and closed event**
  - Assert trimming/lowercasing, 201 response omits email, duplicate/full/closed map to `409`, invalid values map to `400`, and persistence errors map to safe `503` without SQL/stack details.
- [ ] **Step 2: Run targeted tests and confirm they fail**
  - Run: `./gradlew test --tests vn.campushub.registration.RegistrationServiceTest --tests vn.campushub.registration.RegistrationControllerIntegrationTest`
  - Expected: FAIL because registration types and endpoint are absent.
- [ ] **Step 3: Implement migration, repository constraint, DTO validation and transaction**
  - Add unique `(event_id, email_normalized)` constraint. Lock the event row while checking current count and inserting; translate unique violation to the duplicate domain error. Do not return or log raw email.
- [ ] **Step 4: Add and run PostgreSQL-backed concurrency test**
  - Use Testcontainers PostgreSQL; submit simultaneous registrations for the last seat and simultaneous normalized duplicate requests. Assert no over-capacity state and at most one duplicate row.
  - Run: `./gradlew test`
  - Expected: all unit, API, and PostgreSQL integration tests pass.
- [ ] **Step 5: Commit**
  - Commit message: `feat: add safe event registration`

### Task 4: Build the responsive Vietnamese user interface

**Files:**
- Create: `frontend/src/api/events.ts`
- Create: `frontend/src/components/EventCard.tsx`
- Create: `frontend/src/components/EventFilters.tsx`
- Create: `frontend/src/components/RegistrationForm.tsx`
- Create: `frontend/src/pages/EventListPage.tsx`
- Create: `frontend/src/pages/EventDetailPage.tsx`
- Create: `frontend/src/styles.css`
- Test: `frontend/src/App.test.tsx`

**Interfaces:**
- Consumes the event list/detail and registration HTTP contracts from Tasks 2–3.
- Produces accessible list/detail/register flows, loading/empty/error states, and success/validation feedback in Vietnamese.

- [ ] **Step 1: Write UI tests for list/filter, detail navigation, registration success, and API error states**
  - Mock the HTTP boundary; assert labels, form errors, duplicate/full messages, and success confirmation without showing email in the confirmation.
- [ ] **Step 2: Run frontend tests and confirm they fail**
  - Run: `npm --prefix frontend test -- --run`
  - Expected: FAIL because views/components are missing.
- [ ] **Step 3: Implement semantic, responsive pages and API client**
  - Ensure keyboard-accessible forms, clear focus states, responsive layout, and locale-formatted dates.
- [ ] **Step 4: Run frontend tests and production build**
  - Run: `npm --prefix frontend test -- --run && npm --prefix frontend run build`
  - Expected: PASS and static build output is generated.
- [ ] **Step 5: Commit**
  - Commit message: `feat: add Vietnamese event portal UI`

### Task 5: Package and run the full stack locally

**Files:**
- Create: `Dockerfile`
- Create: `compose.yaml`
- Create: `.dockerignore`
- Modify: `build.gradle`
- Modify: `src/main/resources/application.yml`
- Test: `src/test/java/vn/campushub/DeploymentSmokeTest.java`

**Interfaces:**
- `docker compose up --build` starts the application and PostgreSQL; only the app port is published; database credentials are environment-injected; PostgreSQL uses a named volume.
- Spring readiness endpoint: `/actuator/health/readiness` or a minimal equivalent health endpoint.

- [ ] **Step 1: Add deployment smoke test for static UI, API, migration and readiness**
  - Assert `/` serves the built UI, event API returns seed data, and readiness is healthy after DB migration.
- [ ] **Step 2: Run the test and confirm it fails for missing packaging/configuration**
  - Run: `./gradlew test --tests vn.campushub.DeploymentSmokeTest`
  - Expected: FAIL until static build and runtime configuration exist.
- [ ] **Step 3: Implement multi-stage image and Compose services**
  - Build frontend assets in Node stage and app in Java build stage; runtime image contains only JRE and application artifact. Keep database on the private Compose network.
- [ ] **Step 4: Run clean Compose verification**
  - Run: `docker compose up --build -d`; then `curl -fsS http://localhost:8080/actuator/health/readiness` and `curl -fsS http://localhost:8080/api/events`.
  - Expected: healthy app, seeded event response, no published DB port; verify data survives app container recreation.
- [ ] **Step 5: Commit**
  - Commit message: `build: package app and postgres with compose`

### Task 6: Add CI, public repository and safe image publishing

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `.github/workflows/publish-image.yml`
- Create: `README.md`
- Create: `LICENSE` only if the owner selects an open-source license; otherwise document source-availability terms without inventing a license.

**Interfaces:**
- CI runs Gradle tests, frontend tests/build, and image build on pull requests and main-branch pushes.
- Main-branch publishing uses GitHub Actions `GITHUB_TOKEN` to push the image to GHCR; no user token is stored in workflow files.

- [ ] **Step 1: Create workflow validation checklist and verify the exact tests/build commands locally**
  - Run: `./gradlew test bootJar` and `npm --prefix frontend test -- --run && npm --prefix frontend run build`.
  - Expected: PASS before adding workflow triggers.
- [ ] **Step 2: Write least-privilege CI and GHCR publishing workflows**
  - Pin actions to reviewed release tags/SHAs after checking official sources; grant only required package permissions; never run deployment from untrusted pull-request code.
- [ ] **Step 3: Validate workflow YAML and image naming**
  - Confirm build artifact path, GHCR lowercase image name, branch filters, token permissions and action pinning.
- [ ] **Step 4: Create the public GitHub repository and push the reviewed source**
  - Use authenticated GitHub tooling; create under the user's account as `campus-hub-events` if available. If the name is unavailable or account ownership differs, stop and ask before choosing a different public destination.
  - Confirm the first push contains no secrets, passwords, private keys, or real attendee data.
- [ ] **Step 5: Commit**
  - Commit message: `ci: test and publish Campus Hub image`

### Task 7: Deploy to the supplied VPS and verify public behavior

**Files:**
- Create: `deploy/compose.production.yaml`
- Create: `.github/workflows/deploy-vps.yml`
- Create: `docs/deployment/runbook.md`
- Runtime-only VPS changes: deploy directory, Compose environment file, app container, PostgreSQL container/volume, firewall rule only if confirmed necessary.

**Interfaces:**
- Deploy workflow consumes GitHub Secrets `SSH_HOST`, `SSH_USER`, `SSH_PRIVATE_KEY`, and verified `SSH_KNOWN_HOSTS`; image reference is a workflow variable from GHCR.
- VPS runs the app and DB on a private Compose network; app serves HTTP on port 80 for IP-only demo unless a domain and HTTPS are configured.

- [ ] **Step 1: Before remote writes, inspect VPS OS, disk, Docker status, existing listeners/services, firewall and public reachability**
  - Use read-only SSH commands first; confirm no existing user data or app will be overwritten. Stop and request direction if port or directory conflicts exist.
- [ ] **Step 2: Prepare an SSH deploy key and trusted host key**
  - Use the supplied initial account access only to install a restricted deploy public key; do not place its password in shell arguments, repository, logs, or GitHub. Verify the host fingerprint and configure repository secrets through the GitHub UI/API. Never reuse a private key from another project.
- [ ] **Step 3: Add production Compose configuration and deployment workflow**
  - Deploy immutable commit SHA image; persist database volume; set restart policy, health check, resource limits where supported, and never expose PostgreSQL publicly.
- [ ] **Step 4: Deploy, then verify**
  - Confirm workflow success; run remote health checks and public `GET /`, `GET /api/events`, valid registration, duplicate registration and full/invalid behavior with test data. Confirm app restart preserves event/registration rows.
  - Expected: public endpoint responds successfully; if HTTP-only, show only dummy data and state this limitation in README/report. Do not claim HTTPS unless certificate and domain are verified.
- [ ] **Step 5: Commit**
  - Commit message: `deploy: add VPS deployment workflow`

### Task 8: Produce and verify the SRS report and evidence

**Files:**
- Create: `docs/report/Campus-Hub-Mini-Project.docx`
- Create: `docs/report/evidence/` (only actual captured output/screenshots)
- Modify: `README.md`

**Interfaces:**
- Report includes the ten supplied members, verified repository and public URLs, problem/scope, flow/UI, functions/API/data, software/deployment architecture, honest base-project delta, testing/deployment evidence, and limitations.

- [ ] **Step 1: Gather actual test, CI, repository, and deployment results**
  - Do not use fabricated screenshots, fabricated command output, or placeholder URLs as proof.
- [ ] **Step 2: Draft the report using the documents skill and render every page**
  - Include actual UI screenshots and sanitized evidence only; ensure no secrets or real personal data appear.
- [ ] **Step 3: Inspect rendered pages and revise layout/content defects**
  - Confirm all pages are readable, member names are correct, architecture matches deployed system, and claims are supported by evidence.
- [ ] **Step 4: Re-run final verification and link deliverables**
  - Run backend, frontend, Docker and CI-equivalent checks; open the public URL and verify core flow.
- [ ] **Step 5: Commit**
  - Commit message: `docs: add SRS project report and verified evidence`
