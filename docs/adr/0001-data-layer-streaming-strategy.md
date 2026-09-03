# ADR 0001: Data Layer Streaming Strategy — Suspend vs Flow SSOT

**Status:** Accepted  
**Date:** 2026-09-03  
**Deciders:** Emmanuel Debrah

## Problem

Scent's repository layer needs to handle two fundamentally different read patterns:

1. **One-shot reads** — auth checks, search queries, mutations (create/update/delete) where the caller triggers a single fetch and the data isn't expected to change while the screen is visible.
2. **Live-sensitive reads** — Feed, Marketplace, Profile tabs where a user watches the same data change: listings update in real-time, new posts appear, follow counts increment, collection items change.

Using the same method shape for both scenarios creates architectural friction:
- Returning live data from a suspend function requires manual re-polling; the UI has no automatic way to learn the data changed without explicit user action or a ViewModel managing a refresh timer.
- Wrapping every live read in a Flow at the ViewModel layer (instead of the Repository) spreads the streaming logic and defers cache management to the UI state layer, which owns neither the persistence layer nor the network client.

## Decision

Scent's repository layer adopts **two valid method shapes**, each tied to a use case:

| Shape | Signature | Use for |
|-------|-----------|---------|
| **Suspend (one-shot)** | `suspend fun ...(): Either<AppError, T>` | Auth, search, mutations (create, update, delete, follow/unfollow, purchase). Clear start and end, triggered once. |
| **Flow SSOT** | `fun get<X>Flow(...): Flow<Either<AppError, T>>` | Feed, Marketplace (listings + detail), all Profile tabs (Posts, Collection, My Listings, Reviews, Followers/Following). Reads where user watches the same data change on-screen, or another screen's write should reflect without manual re-fetch. |

### Naming Convention
- Flow-returning methods end in a **Flow suffix** — `getFeedFlow`, `getListingsFlow`, `getUserReviewsFlow` — not an observe-prefix.
- This keeps the verb consistent with existing `get...` convention; the suffix signals the return type differs.

### Mechanics

For Flow SSOT methods:
1. **Room is the single source of truth** — the only thing the UI observes.
2. **Network calls are writers, not readers** — a fetch upserts into Room; Room's Flow-returning DAO query re-emits to every collector automatically.
3. **Never return data from a network call directly** — write it to Room and let the existing Flow carry it.

### Example: Feed SSOT

```kotlin
// shared/src/commonMain/kotlin/domain/repository/PostRepository.kt
interface PostRepository {
    // Mutation — stays suspend, one-shot
    suspend fun createPost(request: CreatePostRequest): Result<Post>

    // Live-sensitive read — Flow SSOT, Room-backed
    fun getFeedFlow(page: Int, limit: Int): Flow<Result<List<Post>>>
}

// Implementation sketch
class PostRepositoryImpl(
    private val apiClient: ApiClient,
    private val postDao: PostDao
) : PostRepository {

    override suspend fun createPost(request: CreatePostRequest): Result<Post> {
        // Unchanged: suspend + Either (see Architecture Guidelines §2)
        val request = apiClient.createPost(request)
        return if (request.isSuccessful && request.data != null) {
            request.data.toDomain().asRight()
        } else {
            AppError.Network(request.error).asLeft()
        }
    }

    override fun getFeedFlow(page: Int, limit: Int): Flow<Result<List<Post>>> =
        postDao.observeFeed(page, limit) // Room Flow query — local DB is the only reader
            .map { entities -> entities.map { it.toDomain() }.asRight() }
            .onStart { refreshFeedFromNetwork(page, limit) } // network only ever writes
            .catch { e -> emit(AppError.Unknown(cause = e).asLeft()) }

    private suspend fun refreshFeedFromNetwork(page: Int, limit: Int) {
        val response = apiClient.getFeed(page, limit)
        if (response.isSuccessful && response.data != null) {
            postDao.upsertAll(response.data.map { it.toEntity() }) // triggers re-emission
        }
        // Network failure here is silent to the Flow by design — the last good
        // Room state keeps rendering; surface the failure via a separate side channel
        // (e.g. a SharedFlow<AppError>) if the screen needs to show a toast/snackbar.
    }
}
```

## Consequences

### Benefits
- **Automatic cache invalidation** — Room's Flow automatically re-emits when the underlying data changes; no stale-data bugs or manual refresh logic at the ViewModel layer.
- **Single source of truth** — all reads go through Room; writes go through Room; no drifting between a network response and local state.
- **Scaling** — additional screens/collectors don't create additional network calls; they tap into the same Flow.
- **Offline-first by design** — the last-good Room state always renders; network failures are surfaced via a side channel, not a blocking error.

### Costs
- **Two shapes to maintain** — Repository classes must support both suspend and Flow methods; this requires discipline in design.
- **Room cache-invalidation correctness** — upsert queries must be correct; incorrect upserts lead to inconsistent state or missed updates. See the Room section of Architecture Guidelines (§5) for invalidation rules.
- **iOS Flow-bridging** — Kotlin Flow is coroutine-native; iOS doesn't have coroutines. ViewModels on iOS must bridge Flow to `StateFlow<>` or similar. See `composeApp/iosMain/` for the bridge implementation.

## Notes

**Don't convert a method to Flow SSOT just for consistency** — the two-shape split is intentional. If a method is genuinely one-shot (a mutation, or a read no other screen needs to see update live), it stays suspend. Mixing shapes intentionally is correct; mixing them accidentally is an antipattern.

## Related

- **Architecture Guidelines §2** (Error Handling) — both shapes use `Either<AppError, T>`.
- **Architecture Guidelines §5** (Room & Data Layer) — cache-invalidation rules for SSOT correctness.
- **ViewModel Pattern** — ViewModels on all platforms must observe Flow SSOT methods; suspend methods are observed on a one-shot basis.
