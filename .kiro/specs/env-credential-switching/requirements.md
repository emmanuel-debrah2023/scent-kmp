# Requirements Document

## Introduction

This feature configures the Android/KMP project (`composeApp`) to automatically supply the correct database credentials depending on the active build variant. Debug builds connect to a local PostgreSQL database; release builds connect to Supabase. All secrets are read from `local.properties` at Gradle configuration time so that no credentials are ever hardcoded or committed to version control. A `DatabaseClientFactory` at runtime selects the appropriate client implementation based on the generated `BuildConfig` flag.

## Glossary

- **Build_System**: The Gradle build system that processes `composeApp/build.gradle.kts` and generates `BuildConfig`.
- **BuildConfig**: The Android-generated class that exposes compile-time constants to application code.
- **Local_Properties**: The `local.properties` file at the project root, excluded from version control, that stores all environment-specific secrets.
- **Debug_Build**: The `debug` build type in Gradle, used during local development.
- **Release_Build**: The `release` build type in Gradle, used for production distribution.
- **DatabaseClient**: The common interface implemented by both `LocalDatabaseClient` and `SupabaseDatabaseClient`.
- **LocalDatabaseClient**: The `DatabaseClient` implementation that connects to the local PostgreSQL database.
- **SupabaseDatabaseClient**: The `DatabaseClient` implementation that connects to the Supabase-hosted database.
- **DatabaseClientFactory**: The Kotlin object responsible for selecting and returning the correct `DatabaseClient` at runtime.
- **Gitignore**: The `.gitignore` file at the project root that controls which files are excluded from version control.

---

## Requirements

### Requirement 1: Local Properties Key Structure

**User Story:** As a developer, I want a documented set of keys in `local.properties` for both local and Supabase credentials, so that I know exactly which values to populate before building.

#### Acceptance Criteria

1. THE `Local_Properties` file SHALL contain the key `LOCAL_DB_URL` for the local PostgreSQL JDBC connection string.
2. THE `Local_Properties` file SHALL contain the key `LOCAL_DB_USER` for the local PostgreSQL username.
3. THE `Local_Properties` file SHALL contain the key `LOCAL_DB_PASSWORD` for the local PostgreSQL password.
4. THE `Local_Properties` file SHALL contain the key `SUPABASE_DB_URL` for the Supabase PostgreSQL JDBC connection string.
5. THE `Local_Properties` file SHALL contain the key `SUPABASE_DB_USER` for the Supabase PostgreSQL username.
6. THE `Local_Properties` file SHALL contain the key `SUPABASE_DB_PASSWORD` for the Supabase PostgreSQL password.

---

### Requirement 2: Build System Reads Local Properties

**User Story:** As a developer, I want `build.gradle.kts` to load `local.properties` automatically, so that secret values are available to the Gradle configuration without being hardcoded.

#### Acceptance Criteria

1. WHEN the `Build_System` configures the `composeApp` module, THE `Build_System` SHALL load `local.properties` from the project root directory using `java.util.Properties`.
2. WHEN `local.properties` is absent, THE `Build_System` SHALL fail the configuration phase with a descriptive error message identifying the missing file.

---

### Requirement 3: Build System Validates Required Keys

**User Story:** As a developer, I want the build to fail immediately with a clear message if any required credential key is missing, so that I catch configuration errors before a broken binary is produced.

#### Acceptance Criteria

1. WHEN the `Build_System` configures the `debug` build type, THE `Build_System` SHALL assert that `LOCAL_DB_URL`, `LOCAL_DB_USER`, and `LOCAL_DB_PASSWORD` are each present and non-empty in `Local_Properties`.
2. WHEN the `Build_System` configures the `release` build type, THE `Build_System` SHALL assert that `SUPABASE_DB_URL`, `SUPABASE_DB_USER`, and `SUPABASE_DB_PASSWORD` are each present and non-empty in `Local_Properties`.
3. IF a required key is absent or empty, THEN THE `Build_System` SHALL throw a `GradleException` whose message names the missing key and the build type that requires it.

---

### Requirement 4: Debug BuildConfig Fields

**User Story:** As a developer, I want the debug build to expose local database credentials through `BuildConfig`, so that debug code can connect to the local PostgreSQL instance without any runtime configuration.

#### Acceptance Criteria

1. WHEN the `Debug_Build` is assembled, THE `Build_System` SHALL inject `LOCAL_DB_URL` from `Local_Properties` as a `String` field named `DB_URL` in `BuildConfig`.
2. WHEN the `Debug_Build` is assembled, THE `Build_System` SHALL inject `LOCAL_DB_USER` from `Local_Properties` as a `String` field named `DB_USER` in `BuildConfig`.
3. WHEN the `Debug_Build` is assembled, THE `Build_System` SHALL inject `LOCAL_DB_PASSWORD` from `Local_Properties` as a `String` field named `DB_PASSWORD` in `BuildConfig`.
4. WHEN the `Debug_Build` is assembled, THE `Build_System` SHALL inject the boolean value `false` as a field named `IS_SUPABASE` in `BuildConfig`.

---

### Requirement 5: Release BuildConfig Fields

**User Story:** As a developer, I want the release build to expose Supabase JDBC credentials through `BuildConfig`, so that production code connects to Supabase without any runtime configuration.

#### Acceptance Criteria

1. WHEN the `Release_Build` is assembled, THE `Build_System` SHALL inject `SUPABASE_DB_URL` from `Local_Properties` as a `String` field named `DB_URL` in `BuildConfig`.
2. WHEN the `Release_Build` is assembled, THE `Build_System` SHALL inject `SUPABASE_DB_USER` from `Local_Properties` as a `String` field named `DB_USER` in `BuildConfig`.
3. WHEN the `Release_Build` is assembled, THE `Build_System` SHALL inject `SUPABASE_DB_PASSWORD` from `Local_Properties` as a `String` field named `DB_PASSWORD` in `BuildConfig`.
4. WHEN the `Release_Build` is assembled, THE `Build_System` SHALL inject the boolean value `true` as a field named `IS_SUPABASE` in `BuildConfig`.

---

### Requirement 6: DatabaseClient Interface

**User Story:** As a developer, I want a common `DatabaseClient` interface, so that the rest of the application can interact with the database without knowing which backend is active.

#### Acceptance Criteria

1. THE `DatabaseClient` interface SHALL be defined in the `androidMain` source set of the `composeApp` module.
2. THE `DatabaseClient` interface SHALL declare at minimum a `connect()` function and a `disconnect()` function.
3. THE `LocalDatabaseClient` class SHALL implement the `DatabaseClient` interface and use `BuildConfig.DB_URL`, `BuildConfig.DB_USER`, and `BuildConfig.DB_PASSWORD` to establish its connection.
4. THE `SupabaseDatabaseClient` class SHALL implement the `DatabaseClient` interface and use `BuildConfig.DB_URL`, `BuildConfig.DB_USER`, and `BuildConfig.DB_PASSWORD` to establish its connection.

---

### Requirement 7: DatabaseClientFactory Runtime Selection

**User Story:** As a developer, I want a factory that automatically returns the correct `DatabaseClient` at runtime, so that no conditional logic is scattered across the codebase.

#### Acceptance Criteria

1. THE `DatabaseClientFactory` SHALL expose a single function `create()` that returns a `DatabaseClient` instance.
2. WHEN `BuildConfig.IS_SUPABASE` is `false`, THE `DatabaseClientFactory` SHALL return a `LocalDatabaseClient` instance from `create()`.
3. WHEN `BuildConfig.IS_SUPABASE` is `true`, THE `DatabaseClientFactory` SHALL return a `SupabaseDatabaseClient` instance from `create()`.
4. THE `DatabaseClientFactory` SHALL be defined in the `androidMain` source set of the `composeApp` module.

---

### Requirement 8: Gitignore Protection

**User Story:** As a developer, I want `local.properties` to be excluded from version control, so that credentials are never accidentally committed.

#### Acceptance Criteria

1. THE `Gitignore` file SHALL contain an entry that excludes `local.properties` from version control.
2. THE `Gitignore` file SHALL NOT add an entry for `.env` files as part of this feature.
