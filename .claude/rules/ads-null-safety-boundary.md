---
paths:
  - "shared/src/commonMain/kotlin/**/data/dto/**"
  - "shared/src/commonMain/kotlin/**/data/remote/dto/**"
  - "shared/src/commonMain/kotlin/**/data/mapper/**"
  - "shared/src/commonMain/kotlin/**/domain/model/**"
  - "**/*Dto.kt"
  - "**/*Mapper.kt"
---

# ADS-STE100 — Null-safety boundary

The DTO/domain boundary is where unreliable server data becomes trustworthy app
data. It only works if both sides hold up their end.

**DTOs (`data/dto/`, `data/remote/dto/`)**
- Every field is nullable with an explicit `= null` default. A non-null DTO field
  crashes deserialization the day the server omits it.
- `@SerialName` for every snake_case wire name.
- Enums are `String?` on the wire, never a Kotlin enum — unknown values must not
  throw.

**Domain models (`domain/model/`)**
- Non-null with sensible defaults: `emptyList()`, `""`, `0`, `false`.
- Nullable *only* where absence is semantically meaningful (`releaseYear`,
  `discontinuedDate`). "The server might not send it" is not semantic meaning —
  that is what the default is for.
- Never expose a DTO type to the UI layer.

**Mappers (`data/mapper/`)**
- Mapping lives here and nowhere else. Not in repositories, not in ViewModels,
  not inline at the call site.
- `fun XDto.toDomain(): Result<X>` — returns `Either<AppError, X>`, never a bare
  model and never a throw.
- Validate required fields first and return
  `AppError.NetworkError.ParseError(message =, fieldName =)` as `.asLeft()`.
  Name the field in the error; a bare "parse failed" is not debuggable.
- Nullable lists: `list?.filterNotNull() ?: emptyList()`.
- List mapping (`toDomainList()`) drops invalid entries via `mapNotNull { it.toDomain().getOrNull() }`
  rather than failing the whole page.
- Unknown enum strings go through the companion `fromString()` with a fallback
  branch — never `valueOf()`.

**Never** use `!!` anywhere in this boundary. If you reach for it, the mapper
validation above is missing.
