# Requirements Document

## Introduction

This feature upgrades all out-of-date dependencies in the Scent project to their latest stable versions while preserving full compatibility across all three modules: `composeApp`, `shared`, and `server`. The upgrade is split into two tiers. Tier 1 covers safe, non-breaking version bumps that require only editing `libs.versions.toml`. Tier 2 covers three major-version upgrades — Ktor 2.x → 3.x, JetBrains Exposed 0.x → 1.x, and Android Gradle Plugin 8.x → 9.x — each of which introduces breaking API changes and requires targeted code migration. The scope is strictly limited to version numbers and the code changes mandated by breaking API changes; no architectural changes or new features are introduced.

## Glossary

- **Dependency_Upgrader**: The developer or automated process performing the upgrade steps described in this document.
- **libs.versions.toml**: The Gradle version catalog file at `Scent/gradle/libs.versions.toml` that centralises all dependency and plugin versions for the project.
- **Tier_1_Dependencies**: Dependencies whose latest stable version introduces no breaking API changes relative to the currently used version; upgrading requires only a version number change in `libs.versions.toml`.
- **Tier_2_Dependencies**: Dependencies whose latest stable version introduces breaking API changes relative to the currently used version; upgrading requires both a version number change and targeted source-code migration.
- **composeApp**: The Kotlin Multiplatform Android/iOS application module located at `Scent/composeApp/`.
- **shared**: The Kotlin Multiplatform shared library module located at `Scent/shared/`.
- **server**: The Ktor-based JVM server module located at `Scent/server/`.
- **AGP**: Android Gradle Plugin — the Gradle plugin that builds Android targets; currently `8.9.1`, target `9.2.0`.
- **Ktor**: The asynchronous Kotlin framework used for both the HTTP client (`composeApp`, `shared`) and the HTTP server (`server`); currently `2.3.12`, target `3.4.2`.
- **Exposed**: The JetBrains Kotlin SQL framework used for database access in `server`; currently `0.56.0`, target `1.1.x`.
- **Build_Verification**: A full Gradle build of all modules (`./gradlew build`) that exits with code 0 and produces no compilation errors.
- **Test_Suite**: All existing automated tests reachable via `./gradlew test` across all modules.

---

## Requirements

### Requirement 1: Tier 1 — Safe Version Bumps

**User Story:** As a developer, I want all non-breaking dependencies updated to their latest stable versions, so that the project benefits from bug fixes, performance improvements, and security patches without any risk of compilation failures.

#### Acceptance Criteria

1. THE `Dependency_Upgrader` SHALL update the following version entries in `libs.versions.toml` to the specified values:
   - `kotlin` → `2.3.21`
   - `kotlinx-serialization` → `1.8.1`
   - `kotlinx-datetime` → `0.6.2`
   - `kotlinx-coroutines` → `1.10.2`
   - `coil` → `3.4.0`
   - `postgresql` → `42.7.5`
   - `dotenv` → `6.5.1`
   - `java-jwt` → `4.5.0`
   - `google-auth` → `1.35.0`
   - `google-api-client` → `2.7.2`
2. THE `Dependency_Upgrader` SHALL leave unchanged all version entries not listed in Acceptance Criterion 1 of this requirement and not covered by Requirements 3, 4, or 5.
3. WHEN the Tier 1 version bumps are applied, THE `Build_Verification` SHALL succeed for all three modules (`composeApp`, `shared`, `server`) with no compilation errors.

---

### Requirement 2: Tier 1 Build Verification

**User Story:** As a developer, I want a clean build after Tier 1 changes, so that I can confirm the safe upgrades did not introduce any unexpected incompatibilities before proceeding to the riskier Tier 2 migrations.

#### Acceptance Criteria

1. WHEN all Tier 1 version entries have been updated in `libs.versions.toml`, THE `Build_Verification` SHALL complete successfully before any Tier 2 migration step is started.
2. IF `Build_Verification` fails after Tier 1 changes, THEN THE `Dependency_Upgrader` SHALL resolve all compilation errors before proceeding to Tier 2.

---

### Requirement 3: Tier 2 — Ktor 2.x → 3.x Migration

**User Story:** As a developer, I want Ktor upgraded from `2.3.12` to `3.4.2`, so that the project uses a supported, actively maintained version with improved coroutine and IO APIs.

Migration reference: https://ktor.io/docs/migrating-3.html

#### Acceptance Criteria

1. THE `Dependency_Upgrader` SHALL update the `ktor` version entry in `libs.versions.toml` from `2.3.12` to `3.4.2`.
2. WHEN the Ktor version is updated to `3.4.2`, THE `Dependency_Upgrader` SHALL update all call sites in `composeApp`, `shared`, and `server` that use APIs removed or renamed in Ktor 3.x, including but not limited to:
   - `ApplicationEngine` and `ApplicationEnvironment` usages that changed in the server module.
   - `ByteReadChannel` and `ByteWriteChannel` IO API usages that changed between Ktor 2.x and 3.x.
   - Any `Application` configuration DSL usages that changed between Ktor 2.x and 3.x.
3. THE `Dependency_Upgrader` SHALL NOT change Ktor server artifact names in `libs.versions.toml` (e.g., `ktor-server-core-jvm`, `ktor-server-netty-jvm`) unless the Ktor 3.x migration guide explicitly requires renaming them.
4. WHEN the Ktor 3.x migration is complete, THE `Build_Verification` SHALL succeed for all three modules with no compilation errors before the Exposed migration is started.

---

### Requirement 4: Tier 2 — JetBrains Exposed 0.x → 1.x Migration

**User Story:** As a developer, I want Exposed upgraded from `0.56.0` to `1.1.x`, so that the server module uses the stable, long-term-supported Exposed API with guaranteed compatibility.

Migration reference: https://www.jetbrains.com/help/exposed/migration-guide-1-0-0.html

#### Acceptance Criteria

1. THE `Dependency_Upgrader` SHALL update both the `exposed` and `exposedKotlinDatetime` version entries in `libs.versions.toml` to the latest `1.1.x` stable release.
2. WHEN the Exposed version is updated to `1.1.x`, THE `Dependency_Upgrader` SHALL update all call sites in the `server` module that use APIs removed or renamed in Exposed 1.x, including but not limited to:
   - Any `select()` DSL usages that changed between Exposed 0.x and 1.x (the `where {}` API is the preferred replacement).
   - Any other DSL or DAO APIs identified as breaking changes in the Exposed 1.0.0 migration guide.
3. WHEN the Exposed 1.x migration is complete, THE `Build_Verification` SHALL succeed for all three modules with no compilation errors before the AGP migration is started.

---

### Requirement 5: Tier 2 — Android Gradle Plugin 8.x → 9.x Migration

**User Story:** As a developer, I want AGP upgraded from `8.9.1` to `9.2.0`, so that the Android build toolchain is current and benefits from the latest build performance and API improvements.

#### Acceptance Criteria

1. THE `Dependency_Upgrader` SHALL update the `agp` version entry in `libs.versions.toml` from `8.9.1` to `9.2.0`.
2. WHEN the AGP version is updated to `9.2.0`, THE `Dependency_Upgrader` SHALL update all `build.gradle.kts` files in `composeApp` and `shared` to replace any `buildFeatures` DSL usages or other AGP APIs that were removed or changed in AGP 9.x.
3. WHEN the AGP 9.x migration is complete, THE `Build_Verification` SHALL succeed for all three modules with no compilation errors.

---

### Requirement 6: Full Post-Upgrade Verification

**User Story:** As a developer, I want the complete project to compile cleanly and all existing tests to pass after all upgrades are applied, so that I can be confident the upgrade has not introduced any regressions.

#### Acceptance Criteria

1. WHEN all Tier 1 and Tier 2 upgrades are complete, THE `Build_Verification` SHALL succeed for all three modules (`composeApp`, `shared`, `server`) with exit code 0.
2. WHEN all Tier 1 and Tier 2 upgrades are complete, THE `Test_Suite` SHALL pass with no test failures or errors across all modules.
3. IF any test in the `Test_Suite` fails after the upgrade, THEN THE `Dependency_Upgrader` SHALL fix the failure before considering the upgrade complete.
4. THE `Dependency_Upgrader` SHALL NOT introduce any new dependencies, remove any existing dependencies, or change the project architecture as part of this upgrade.
5. THE `Dependency_Upgrader` SHALL NOT modify any dependency version entries that are already at their latest stable version as listed in the introduction of this document.
