# Scent

**Early preview** — active development, not yet production-ready.

A fragrance social-commerce app built with Kotlin Multiplatform. Discover, review, and trade fragrances — all in one place.

## Screenshots

_Coming soon_

## Features

- **Auth** — email/password registration and login with JWT sessions; Google Sign-In support
- **Community feed** — text and video posts, hashtags, likes
- **Fragrance catalogue** — browse and search fragrances with notes, concentration, and condition details
- **Marketplace** — list and discover fragrance listings
- **Media** — short-form video upload and playback via Cloudflare Stream
- **Profile** — user profile with avatar, bio, and logout

## Tech stack

| Layer | Technology |
|-------|-----------|
| Mobile (Android & iOS) | Kotlin Multiplatform · Compose Multiplatform |
| Backend | Ktor · Exposed · PostgreSQL |
| Dependency injection | Koin |
| Networking | Ktor client · kotlinx.serialization |
| Video | Cloudflare Stream · ExoPlayer (Android) |
| Auth | JWT · Google OAuth |

## Project structure

```
├── composeApp/   # Shared Compose UI (Android + iOS targets)
├── shared/       # Domain models, repositories, use cases, DI
├── server/       # Ktor REST API
└── iosApp/       # iOS entry point (Xcode)
```

## Getting started

### Prerequisites

- Android Studio Meerkat or later
- Xcode 16+ (iOS builds)
- JDK 17+
- PostgreSQL instance

### Android

```bash
./gradlew :composeApp:assembleDebug
```

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.

### Server

Copy `.env.example` to `.env` and fill in your database and Cloudflare credentials, then:

```bash
./gradlew :server:run
```

To seed the local feed with sample posts (requires `STREAM_PROVIDER=fake`):

```bash
curl -X POST "http://localhost:8080/api/v1/dev/seed-feed?count=10"
```

## Architecture

Scent follows a clean, layered architecture documented in `docs/architecture-guidelines.md`.

- **Data layer** — DTOs, mappers, remote/local data sources, repositories returning `Either<AppError, T>`
- **Domain layer** — non-null models, use cases, typed error hierarchy
- **UI layer** — Compose screens driven by `StateFlow<UiState<T>>` ViewModels; state-based navigation

## Contributing

Issues and pull requests are welcome. Please open an issue first to discuss larger changes.

## Licence

[MIT](LICENSE)
