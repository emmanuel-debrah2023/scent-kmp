# ADR-0001: Adopt Flow SSOT for Feed, Marketplace, and Profile reads, keep suspend+Either elsewhere

- **Status:** Accepted
- **Date:** 2026-09-03
- **Deciders:** Emmanuel
- **Ticket:** Scent Tasks Tracker — "ADR: decide data-layer streaming strategy (suspend vs Flow SSOT)" (https://app.notion.com/p/3cd11d6b186b81e1881fe80b38d3d095)

## Context

Repositories in Scent currently expose one-shot `suspend fun ...(): Either<AppError, T>`
methods. Read methods like `FragranceRepositoryImpl.searchFragrances` do an ad-hoc
cache-then-network check: try the local cache, return early if it has data, otherwise
hit the API and write through. Nothing pushes updates to a screen that's already open —
if a like count changes on the Post Detail screen, the Feed screen behind it stays stale
until it's manually re-fetched.

This staleness is most visible on three screens where users watch data change in
real time while browsing: the short-form video Feed (like/comment/share counts,
new posts), the Marketplace (listing price changes, sold-out state, new listings),
and the Profile screen — its own review count, fragrance collection, seller
listings, and follower/following stats and lists all update in place as the user
takes actions (adding a review, adding a bottle, another user following them)
without leaving or manually refreshing any of the screen's tabs. All of these are
list- or count-shaped, multi-screen-visible or multi-action-visible within a single
session, and read far more often than they're written to from that same session.

## Decision

Scent adopts a hybrid data-layer strategy. Repository methods keep their current
`suspend fun ...(): Either<AppError, T>` shape for one-shot operations — auth
(`AuthRepository`), and any create/update/delete mutation (`PostRepository.createPost`,
`ListingRepository.purchaseListing`, etc.).

For the Feed, Marketplace, and Profile read paths specifically, repositories instead
expose `fun get...Flow(): Flow<Either<AppError, T>>`, backed by Room as the local
source of truth:

- `PostRepository.getFeedFlow(page: Int, limit: Int): Flow<Either<AppError, List<Post>>>`
- `ListingRepository.getListingsFlow(query: ListingQuery): Flow<Either<AppError, List<Listing>>>`
- `ListingRepository.getListingDetailFlow(id: String): Flow<Either<AppError, Listing>>`
- `UserRepository.getProfileFlow(userId: String): Flow<Either<AppError, User>>` —
  includes `followerCount`/`followingCount` as live fields on `User`
- `PostRepository.getUserPostsFlow(userId: String): Flow<Either<AppError, List<Post>>>`
  — the Profile screen's Posts tab
- `CollectionRepository.getUserCollectionFlow(userId: String): Flow<Either<AppError, List<UserFragrance>>>`
  — the Collection tab
- `ListingRepository.getUserListingsFlow(userId: String): Flow<Either<AppError, List<Listing>>>`
  — the My Listings tab (`ProfileRoute.MyListings`)
- `ReviewRepository.getUserReviewsFlow(userId: String): Flow<Either<AppError, List<Review>>>`
  — the Reviews tab
- `SocialRepository.getFollowersFlow(userId: String): Flow<Either<AppError, List<User>>>`
  and `SocialRepository.getFollowingFlow(userId: String): Flow<Either<AppError, List<User>>>`
  — the Followers/Following lists reachable from the profile stats

Flow-returning repository methods are named with a `Flow` suffix (`getFeedFlow`,
`getListingsFlow`), not an `observe`-prefix — this keeps the verb consistent with the
existing `get...` naming used by the suspend methods, with the suffix as the only
signal that the return type differs.

Network calls become **writers**, not readers, for these paths: a fetch upserts into
Room, and Room's Flow-returning DAO query re-emits to every collector automatically.
ViewModels for Feed, Marketplace, and Profile screens collect these Flows instead of
calling a use case and pushing a one-shot result into `UiState`. The outer wrapper
changes from `suspend` to `Flow`; the `Either<AppError, T>` error boundary is unchanged.

This decision applies only to the Feed, Marketplace, and Profile read paths in the
shared KMP module. It does not apply to Search or any mutation-style repository
method — adding a review or adding a bottle to a collection stays a `suspend fun
...(): Either<AppError, Unit>` write; only the *read* that reflects the result of
that write becomes a Flow.

## Consequences

**Good**
- Feed, Marketplace, and every Profile tab (Posts, Collection, My Listings, Reviews,
  Followers/Following) reflect changes — likes, price updates, new listings, a review
  or collection bottle just added, a new follower — as soon as the local DB is
  written to, without a manual refresh or re-navigation.
- The mandatory `Either<AppError, T>` boundary from `architecture-guidelines.md` is
  preserved everywhere — this only changes the outer wrapper on the converted methods,
  not the error-handling contract.
- No repository-wide rewrite. Auth and every mutation-shaped repository method are
  untouched, so most of the existing test suite doesn't change shape.

**Bad**
- Two repository shapes now coexist (`suspend`-`Either` and `Flow`-`Either`). Without
  the rule stated above being followed consistently, new repository methods will be
  added to whichever shape a given PR author reaches for first, and the boundary erodes.
- Feed, Marketplace, and Profile need a real Room entity + DAO layer to become
  authoritative caches — they currently only have stub DAOs (`CachedFragranceDao`,
  `UserFragranceDao`) referenced in Koin's `databaseModule`, not a full write-through
  schema for posts, listings, reviews, collection entries, and the follow graph. The
  Profile screen alone now needs seven converted methods across six repositories
  (`UserRepository`, `PostRepository`, `CollectionRepository`, `ListingRepository`,
  `ReviewRepository`, `SocialRepository`), which is the largest single chunk of this
  migration's Room surface.
- Test surface changes for the converted methods only: assertions move from a single
  suspend return to Flow emission sequences (Turbine), which is more test code per
  method — and now spans seven Profile-tab methods on top of Feed and Marketplace.
- Cache invalidation now matters in a way it didn't before. A stale suspend-based
  cache previously meant one stale read; a stale or incorrectly-evicted Room row now
  means every open collector keeps rendering wrong data until the next write. The
  network writer has to upsert and evict correctly, not just "return what we got."
- iOS Flow ergonomics: Swift has no native `Flow` collection. Feed, Marketplace, and
  every Profile-tab ViewModel on iOS need a Flow-to-callback (or
  `SkieKotlinFlow`-style) bridge that doesn't exist yet in `iosMain`.

**Neutral**
- Auth and Search screens are unaffected — they keep calling suspend use cases
  exactly as they do today. Writes across all of Profile (submitting a review,
  adding a bottle, following/unfollowing another user) also stay suspend-shaped;
  only the reads that reflect them become Flow.

## Alternatives considered

**Full Flow SSOT (every repository method converted)** — rejected because it forces
one-shot, mutation-style operations (login, create-post, checkout) into Flow shape
they don't need, purely for repository-shape consistency. That's a full rewrite of
every repository plus every consuming ViewModel, disproportionate to what the actual
pain point (stale Feed, Marketplace, and Profile screens) requires at this stage of
the project.

**Status quo (keep the ad-hoc suspend cache check everywhere)** — rejected because it
leaves the actual problem unsolved: a like tapped on one screen still won't be
reflected on the Feed screen behind it without a manual re-fetch, and the ticket that
raised this was explicitly asking for the ad-hoc pattern to be reconsidered, not kept.

## Revisit if

- Scent adds real offline-write support (queued mutations, conflict resolution). At
  that point Room-as-truth stops being optional for the read paths above and becomes
  the only sane way to reconcile local writes against server state — likely widening
  Flow SSOT to more repositories, which would supersede this ADR.
- The set of screens needing live updates grows past Feed/Marketplace/Profile to the
  point where maintaining two repository shapes costs more than standardizing on
  Flow everywhere.
