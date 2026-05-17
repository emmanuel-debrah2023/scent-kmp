# Implementation Plan: env-credential-switching

## Overview

Wire environment-specific database credentials into the Android build pipeline via three incremental steps: (1) update `composeApp/build.gradle.kts` to load, validate, and inject credentials from `local.properties`; (2) create the four Kotlin DI files under `androidMain/kotlin/di/`; (3) extract `requireProperty` into a testable form and write the property-based test.

## Tasks

- [x] 1. Update `composeApp/build.gradle.kts` to load and validate `local.properties`
  - Add `import java.util.Properties` at the top of the file (before the `plugins {}` block)
  - After the `plugins {}` block, add a `val localProperties = Properties().apply { ... }` block that opens `rootProject.file("local.properties")`; if the file does not exist, throw a `GradleException` with the absolute path in the message
  - Add a top-level `fun requireProperty(key: String, buildType: String): String` helper that reads from `localProperties`, throwing a `GradleException` naming the missing key and build type if the value is null or blank
  - Inside the `android {}` block, add `buildFeatures { buildConfig = true }`
  - Inside `buildTypes`, add `buildConfigField` calls to the existing `getByName("debug")` block (create it if absent): `DB_URL` ← `DATABASE_URL`, `DB_USER` ← `DATABASE_USER`, `DB_PASSWORD` ← `DATABASE_PASSWORD`, `IS_SUPABASE` ← `false`
  - Inside the existing `getByName("release")` block, add `buildConfigField` calls: `DB_URL` ← `DB_URL`, `DB_USER` ← `DB_USER`, `DB_PASSWORD` ← `DB_PASSWORD`, `IS_SUPABASE` ← `true`
  - _Requirements: 2.1, 2.2, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 5.4_

- [x] 2. Extract `requireProperty` into a standalone testable function
  - Create `composeApp/src/commonTest/kotlin/RequirePropertyTest.kt` (or a shared utility file) that re-declares `requireProperty` as a pure function accepting `(props: java.util.Properties, key: String, buildType: String): String` — identical logic to the Gradle helper but decoupled from the `localProperties` closure so it can be called from tests
  - This file is the test target; the Gradle script's `requireProperty` can delegate to it or duplicate the logic — either approach is acceptable as long as the pure function is independently callable
  - _Requirements: 3.1, 3.2, 3.3_

  - [ ]* 2.1 Write property-based test for `requireProperty` (Property 1)
    - Add `kotest-property` to `commonTest` dependencies in `composeApp/build.gradle.kts` if not already present
    - In `composeApp/src/commonTest/kotlin/RequirePropertyTest.kt`, write two `checkAll` blocks (minimum 100 iterations each) using `Arb.string()`:
      - **Block A** — key present: if `value.isBlank()` → `shouldThrow<GradleException> { requireProperty(props, key, buildType) }.message shouldContainAll listOf(key, buildType)`; else → `requireProperty(props, key, buildType) shouldBe value`
      - **Block B** — key absent: `Properties()` with no entry for `key` → `shouldThrow<GradleException> { ... }.message shouldContainAll listOf(key, buildType)`
    - **Property 1: requireProperty validation correctness**
    - **Validates: Requirements 3.1, 3.2, 3.3**

- [x] 3. Checkpoint — verify Gradle configuration
  - Run `./gradlew :composeApp:assembleDebug` and confirm the build succeeds and `BuildConfig.kt` contains `DB_URL`, `DB_USER`, `DB_PASSWORD`, and `IS_SUPABASE = false`
  - Run `./gradlew :composeApp:assembleRelease` and confirm `IS_SUPABASE = true` and Supabase values are present
  - Ensure all tests pass; ask the user if questions arise.

- [x] 4. Create the `DatabaseClient` interface
  - Create `composeApp/src/androidMain/kotlin/di/DatabaseClient.kt` with `package di`
  - Declare `interface DatabaseClient` with `fun connect()` and `fun disconnect()`
  - _Requirements: 6.1, 6.2_

- [x] 5. Create `LocalDatabaseClient`
  - Create `composeApp/src/androidMain/kotlin/di/LocalDatabaseClient.kt` with `package di`
  - Declare `class LocalDatabaseClient : DatabaseClient`
  - Implement `connect()` with a TODO comment referencing `BuildConfig.DB_URL`, `BuildConfig.DB_USER`, `BuildConfig.DB_PASSWORD`
  - Implement `disconnect()` with a TODO comment for closing the JDBC connection
  - Add `import org.scent.project.BuildConfig`
  - _Requirements: 6.3_

- [x] 6. Create `SupabaseDatabaseClient`
  - Create `composeApp/src/androidMain/kotlin/di/SupabaseDatabaseClient.kt` with `package di`
  - Declare `class SupabaseDatabaseClient : DatabaseClient`
  - Implement `connect()` with a TODO comment referencing `BuildConfig.DB_URL`, `BuildConfig.DB_USER`, `BuildConfig.DB_PASSWORD`
  - Implement `disconnect()` with a TODO comment for closing the JDBC connection
  - Add `import org.scent.project.BuildConfig`
  - _Requirements: 6.4_

- [x] 7. Create `DatabaseClientFactory`
  - Create `composeApp/src/androidMain/kotlin/di/DatabaseClientFactory.kt` with `package di`
  - Declare `object DatabaseClientFactory` with a single `fun create(): DatabaseClient`
  - Implement `create()` as: `if (BuildConfig.IS_SUPABASE) SupabaseDatabaseClient() else LocalDatabaseClient()`
  - Add `import org.scent.project.BuildConfig`
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 7.1 Write unit tests for `DatabaseClientFactory`
    - In `composeApp/src/commonTest/kotlin/`, create `DatabaseClientFactoryTest.kt`
    - Test that when `IS_SUPABASE` is `false`, `create()` returns a `LocalDatabaseClient` instance
    - Test that when `IS_SUPABASE` is `true`, `create()` returns a `SupabaseDatabaseClient` instance
    - _Requirements: 7.2, 7.3_

- [x] 8. Final checkpoint — ensure all tests pass
  - Run `./gradlew :composeApp:testDebugUnitTest` (or equivalent) and confirm all unit and property tests pass
  - Confirm the four DI files compile without errors in both debug and release variants
  - Ensure all tests pass; ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- `local.properties` and `.gitignore` require no changes — both are already correct
- The `di/` directory already exists at `composeApp/src/androidMain/kotlin/di/`; no directory creation needed
- Key mapping: debug reads `DATABASE_URL` / `DATABASE_USER` / `DATABASE_PASSWORD`; release reads `DB_URL` / `DB_USER` / `DB_PASSWORD`
- `requireProperty` in the Gradle script closes over `localProperties`; the extracted pure form for testing takes `Properties` as an explicit parameter
- Property tests validate universal correctness of the validation helper; unit tests validate specific factory branching behavior
