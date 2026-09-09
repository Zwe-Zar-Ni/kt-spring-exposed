# AGENTS.md

Spring Boot 4 (Kotlin) + JetBrains Exposed REST API with JWT auth backed by MySQL.

## Build / run
- Gradle wrapper, Kotlin DSL, Java 17 toolchain. Commands:
  - `./gradlew build` / `./gradlew test`
  - `./gradlew bootRun` to run locally
- Tests use JUnit 5 (`useJUnitPlatform()`). The only test is a `@SpringBootTest` context load that boots the full app and needs a live MySQL, so `build`/`test` fail without one.

## Runtime prerequisites
- `src/main/resources/application.properties` hardcodes a local MySQL connection (DB `exposed`, committed root credentials) plus the JWT secret and expiration. No profiles or env overrides.
- `spring.exposed.generate-ddl=true`: Exposed auto-creates tables on startup, so there are NO migrations — a new `XTable` object takes effect on restart against the local DB.
- `spring.exposed.show-sql=true` logs every query.

## Gotchas (easy to get wrong)
- Spring Boot 4 renamed starters: use `spring-boot-starter-webmvc` and `spring-boot-starter-webmvc-test`, NOT `-web`. Jackson is Jackson 3 under `tools.jackson.*`, not `com.fasterxml.jackson`. Reference the same Boot 4 names when adding deps.
- This project uses the NEW Exposed v1 API: imports live under `org.jetbrains.exposed.v1.*` (`v1.core`, `v1.jdbc`, `v1.core.dao.id`). Do not import the classic `org.jetbrains.exposed.sql.*` shown in most older Exposed docs.
- `SpringexposedApplication.kt` deliberately `exclude`s `DataSourceTransactionManagerAutoConfiguration` in favor of `ExposedAutoConfiguration`. Repositories rely on Spring `@Transactional` (`@Transactional(readOnly = true)` for reads). Do not "fix" this.

## Layout & conventions
- Package root is `com.vaddshah2626.springexposed`.
- Feature slices under `features/<name>/`: Controller -> Service -> Repository -> table object (`object X : LongIdTable("table")`), with request/response DTOs in a `dtos/` subpackage. Shared infra (security, exceptions, `PageResponse`) lives in `common/`.
- Repositories use the Exposed DSL (`Table.selectAll()`, `andWhere { ... }`, `insertAndGetId { ... }`, `deleteWhere`) and return feature DTOs.
- Query params bind to a data class with defaults — see `ProductFilter` (search/stock/page/size); use the same pattern for list endpoints.
- DTO hygiene: internal `UserDto` carries the BCrypt `password`; API-response DTOs (`UserResponse`, etc.) must never expose it.

## Security
- Stateless JWT + BCrypt. `security.md` is an accurate end-to-end map of the auth flow — keep it in sync when touching that code.
- `SecurityConfig`: `/api/auth/**` and GET `/api/products/**` are `permitAll`; every other route requires authentication. When adding endpoints, add a rule here.
- Errors flow through `GlobalExceptionHandler` (`@RestControllerAdvice`): `BadCredentialsException`/`UsernameNotFoundException` -> 401 "Invalid email or password" (same message to avoid user enumeration), `IllegalStateException` -> 409, `AccessDeniedException` -> 403, validation -> 400 with field errors.