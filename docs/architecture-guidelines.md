# Agent Instructions for Fragrance Social Commerce Android App

## Project Context
You are working on a Kotlin-based Android application for a fragrance social commerce platform. The app uses Jetpack Compose, MVVM architecture, and Kotlin Multiplatform for shared business logic.

## Core Architecture Principles

### 1. Null Safety and Serialization Strategy

**CRITICAL**: Balance between null safety and real-world API reliability. Use nullable properties defensively for API responses, but enforce non-null contracts in domain models.

#### API Data Transfer Objects (DTOs) - Nullable by Default
```kotlin
// shared/src/commonMain/kotlin/data/dto/ApiDtos.kt

/**
 * API DTOs should use nullable properties to handle:
 * - Unreliable server responses
 * - Missing fields in API responses
 * - Backward compatibility with API changes
 * - Malformed JSON without crashing
 */

@Serializable
data class FragranceDto(
    val id: String? = null,                    // Nullable - server might not return it
    val name: String? = null,                  // Nullable - handle missing data
    val brand: String? = null,
    val description: String? = null,
    @SerialName("image_urls")
    val imageUrls: List<String>? = null,       // Nullable list
    @SerialName("release_year")
    val releaseYear: Int? = null,
    val gender: String? = null,
    val concentration: String? = null,
    @SerialName("top_notes")
    val topNotes: List<String>? = null,
    @SerialName("middle_notes")
    val middleNotes: List<String>? = null,
    @SerialName("base_notes")
    val baseNotes: List<String>? = null,
    @SerialName("main_accords")
    val mainAccords: List<String>? = null,
    @SerialName("average_rating")
    val averageRating: Double? = null,
    @SerialName("review_count")
    val reviewCount: Int? = null,
    @SerialName("fragella_id")
    val fragellaId: String? = null
)

@Serializable
data class UserDto(
    val id: String? = null,
    val username: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val email: String? = null,
    @SerialName("profile_image_url")
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val location: String? = null,
    @SerialName("follower_count")
    val followerCount: Int? = null,
    @SerialName("following_count")
    val followingCount: Int? = null,
    @SerialName("created_at")
    val createdAt: Long? = null,
    @SerialName("is_verified_seller")
    val isVerifiedSeller: Boolean? = null
)

@Serializable
data class PostDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("content_format")
    val contentFormat: String? = null,          // String, not enum - handle unknown values
    @SerialName("text_content")
    val textContent: String? = null,
    @SerialName("media_urls")
    val mediaUrls: List<String>? = null,
    @SerialName("fragrance_ids")
    val fragranceIds: List<String>? = null,
    val hashtags: List<String>? = null,
    @SerialName("like_count")
    val likeCount: Int? = null,
    @SerialName("comment_count")
    val commentCount: Int? = null,
    @SerialName("share_count")
    val shareCount: Int? = null,
    @SerialName("created_at")
    val createdAt: Long? = null,
    @SerialName("listing_data")
    val listingData: List<PostListingDto>? = null
)

@Serializable
data class PostListingDto(
    @SerialName("fragrance_id")
    val fragranceId: String? = null,
    val price: Double? = null,
    val condition: String? = null,
    @SerialName("is_negotiable")
    val isNegotiable: Boolean? = null
)

@Serializable
data class ApiResponseDto<T>(
    val success: Boolean? = null,
    val data: T? = null,
    val error: String? = null,
    val message: String? = null,               // Additional error details
    val timestamp: Long? = null
)
```

#### Domain Models - Non-Null with Defaults
```kotlin
// shared/src/commonMain/kotlin/domain/model/DomainModels.kt

/**
 * Domain models enforce business rules and data integrity.
 * Use non-null properties with sensible defaults.
 * Never expose nullable properties to UI layer unless semantically meaningful.
 */

data class Fragrance(
    val id: String,
    val name: String,
    val brand: String,
    val imageUrls: List<String> = emptyList(),
    val description: String = "",
    val releaseYear: Int? = null,              // Legitimately unknown - keep nullable
    val gender: FragranceGender = FragranceGender.UNISEX,
    val concentration: String = "",
    val topNotes: List<String> = emptyList(),
    val middleNotes: List<String> = emptyList(),
    val baseNotes: List<String> = emptyList(),
    val mainAccords: List<String> = emptyList(),
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val fragellaId: String? = null
)

enum class FragranceGender {
    MASCULINE, FEMININE, UNISEX;

    companion object {
        fun fromString(value: String?): FragranceGender {
            return when (value?.lowercase()) {
                "masculine", "male", "men" -> MASCULINE
                "feminine", "female", "women" -> FEMININE
                else -> UNISEX
            }
        }
    }
}

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val profileImageUrl: String = "",
    val bio: String = "",
    val location: String = "",
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val createdAt: Long,
    val isVerifiedSeller: Boolean = false
)

data class Post(
    val id: String,
    val userId: String,
    val contentFormat: ContentFormat,
    val textContent: String = "",
    val mediaUrls: List<String> = emptyList(),
    val fragranceIds: List<String>,            // Non-empty list required
    val hashtags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val createdAt: Long,
    val listingData: List<PostListing> = emptyList()
)

enum class ContentFormat {
    TEXT, PHOTO, VIDEO;

    companion object {
        fun fromString(value: String?): ContentFormat {
            return when (value?.uppercase()) {
                "TEXT" -> TEXT
                "PHOTO", "IMAGE" -> PHOTO
                "VIDEO" -> VIDEO
                else -> TEXT // Default fallback
            }
        }
    }
}

data class PostListing(
    val fragranceId: String,
    val price: Double,
    val condition: String,
    val isNegotiable: Boolean = false
)
```

#### Mapper Pattern - Safe Conversion from DTO to Domain

**CRITICAL**: All DTO to Domain conversions MUST return `Either<AppError, DomainModel>`

```kotlin
// shared/src/commonMain/kotlin/data/mapper/FragranceMapper.kt

object FragranceMapper {

    /**
     * Maps nullable DTO to non-null Domain model.
     * Returns Left(AppError.NetworkError.ParseError) if required fields are missing.
     */
    fun FragranceDto.toDomain(): Result<Fragrance> {
        // Validate required fields
        if (id.isNullOrBlank()) {
            return AppError.NetworkError.ParseError(
                message = "Fragrance ID is missing from server response"
            ).asLeft()
        }

        if (name.isNullOrBlank()) {
            return AppError.NetworkError.ParseError(
                message = "Fragrance name is missing from server response"
            ).asLeft()
        }

        if (brand.isNullOrBlank()) {
            return AppError.NetworkError.ParseError(
                message = "Fragrance brand is missing from server response"
            ).asLeft()
        }

        return Fragrance(
            id = id,
            name = name,
            brand = brand,
            imageUrls = imageUrls?.filterNotNull() ?: emptyList(),
            description = description ?: "",
            releaseYear = releaseYear,
            gender = FragranceGender.fromString(gender),
            concentration = concentration ?: "",
            topNotes = topNotes?.filterNotNull() ?: emptyList(),
            middleNotes = middleNotes?.filterNotNull() ?: emptyList(),
            baseNotes = baseNotes?.filterNotNull() ?: emptyList(),
            mainAccords = mainAccords?.filterNotNull() ?: emptyList(),
            averageRating = averageRating ?: 0.0,
            reviewCount = reviewCount ?: 0,
            fragellaId = fragellaId
        ).asRight()
    }

    /**
     * Maps list of DTOs, filtering out invalid entries.
     * Never crashes - always returns a valid list (potentially empty).
     */
    fun List<FragranceDto>.toDomainList(): List<Fragrance> {
        return mapNotNull { dto ->
            dto.toDomain().getOrNull()
        }
    }
}

object UserMapper {

    fun UserDto.toDomain(): Result<User> {
        if (id.isNullOrBlank()) {
            return AppError.NetworkError.ParseError(
                message = "User ID is missing"
            ).asLeft()
        }

        if (username.isNullOrBlank()) {
            return AppError.NetworkError.ParseError(
                message = "Username is missing"
            ).asLeft()
        }

        if (email.isNullOrBlank()) {
            return AppError.NetworkError.ParseError(
                message = "Email is missing"
            ).asLeft()
        }

        if (createdAt == null) {
            return AppError.NetworkError.ParseError(
                message = "Created timestamp is missing"
            ).asLeft()
        }

        return User(
            id = id,
            username = username,
            displayName = displayName ?: username, // Fallback to username
            email = email,
            profileImageUrl = profileImageUrl ?: "",
            bio = bio ?: "",
            location = location ?: "",
            followerCount = followerCount ?: 0,
            followingCount = followingCount ?: 0,
            createdAt = createdAt,
            isVerifiedSeller = isVerifiedSeller ?: false
        ).asRight()
    }
}

object PostMapper {

    fun PostDto.toDomain(): Result<Post> {
        if (id.isNullOrBlank()) {
            return AppError.NetworkError.ParseError(
                message = "Post ID is missing"
            ).asLeft()
        }

        if (userId.isNullOrBlank()) {
            return AppError.NetworkError.ParseError(
                message = "User ID is missing"
            ).asLeft()
        }

        if (fragranceIds.isNullOrEmpty()) {
            return AppError.NetworkError.ParseError(
                message = "Post must have at least one fragrance linked"
            ).asLeft()
        }

        if (createdAt == null) {
            return AppError.NetworkError.ParseError(
                message = "Created timestamp is missing"
            ).asLeft()
        }

        return Post(
            id = id,
            userId = userId,
            contentFormat = ContentFormat.fromString(contentFormat),
            textContent = textContent ?: "",
            mediaUrls = mediaUrls?.filterNotNull() ?: emptyList(),
            fragranceIds = fragranceIds.filterNotNull(),
            hashtags = hashtags?.filterNotNull() ?: emptyList(),
            likeCount = likeCount ?: 0,
            commentCount = commentCount ?: 0,
            shareCount = shareCount ?: 0,
            createdAt = createdAt,
            listingData = listingData?.mapNotNull { it.toPostListing() } ?: emptyList()
        ).asRight()
    }

    private fun PostListingDto.toPostListing(): PostListing? {
        val fragranceId = fragranceId ?: return null
        val price = price ?: return null
        val condition = condition ?: return null

        return PostListing(
            fragranceId = fragranceId,
            price = price,
            condition = condition,
            isNegotiable = isNegotiable ?: false
        )
    }
}
```

#### Repository Implementation with Safe Mapping

```kotlin
// shared/src/commonMain/kotlin/data/repository/FragranceRepositoryImpl.kt

class FragranceRepositoryImpl(
    private val apiClient: ApiClient,
    private val cachedFragranceDao: CachedFragranceDao
) : FragranceRepository {

    override suspend fun searchFragrances(
        query: String,
        page: Int,
        limit: Int
    ): Result<List<Fragrance>> {
        return try {
            val response: ApiResponseDto<List<FragranceDto>> =
                apiClient.searchFragrances(query, page, limit)

            // Handle nullable API response
            if (response.success != true) {
                return AppError.NetworkError.ServerError(
                    statusCode = 0,
                    message = response.error ?: response.message ?: "Unknown error"
                ).asLeft()
            }

            val dtos = response.data ?: emptyList()

            // Safely map DTOs to domain models
            // Invalid entries are filtered out, not causing crashes
            val fragrances = dtos.toDomainList()

            // Even if all DTOs were invalid, return empty list (not an error)
            fragrances.asRight()

        } catch (e: SerializationException) {
            AppError.NetworkError.ParseError(
                message = "Failed to parse server response: ${e.message}",
                cause = e
            ).asLeft()
        } catch (e: UnknownHostException) {
            AppError.NetworkError.NoConnection().asLeft()
        } catch (e: SocketTimeoutException) {
            AppError.NetworkError.Timeout().asLeft()
        } catch (e: Exception) {
            AppError.Unknown(
                message = "Unexpected error: ${e.message}",
                cause = e
            ).asLeft()
        }
    }

    override suspend fun getFragranceById(id: String): Result<Fragrance> {
        return try {
            val response: ApiResponseDto<FragranceDto> =
                apiClient.getFragrance(id)

            if (response.success != true) {
                return AppError.ContentError.FragranceNotFound(
                    fragranceId = id
                ).asLeft()
            }

            val dto = response.data
            if (dto == null) {
                return AppError.ContentError.FragranceNotFound(
                    fragranceId = id
                ).asLeft()
            }

            // Map DTO to domain - returns Either
            dto.toDomain()

        } catch (e: SerializationException) {
            AppError.NetworkError.ParseError(
                message = "Failed to parse fragrance data",
                cause = e
            ).asLeft()
        } catch (e: Exception) {
            AppError.Unknown(
                message = "Failed to fetch fragrance: ${e.message}",
                cause = e
            ).asLeft()
        }
    }
}
```

#### JSON Configuration for Lenient Parsing

```kotlin
// shared/src/commonMain/kotlin/network/JsonConfig.kt

val JsonConfig = Json {
    // Lenient parsing - don't crash on malformed JSON
    isLenient = true

    // Ignore unknown keys - API might add new fields
    ignoreUnknownKeys = true

    // Don't crash if server sends null for non-nullable field
    coerceInputValues = true

    // Use defaults when values are missing
    encodeDefaults = true

    // Pretty print for debugging
    prettyPrint = BuildConfig.DEBUG

    // Handle polymorphic types
    classDiscriminator = "type"
}

// Usage in Ktor client
HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(JsonConfig)
    }
}
```

### 2. Null Safety Best Practices

#### ✅ DO: Use Nullable DTOs with Safe Mapping
```kotlin
// ✅ CORRECT: DTO is nullable, domain is not
@Serializable
data class FragranceDto(
    val id: String? = null,      // Nullable - might be missing
    val name: String? = null
)

data class Fragrance(
    val id: String,              // Non-null - required for business logic
    val name: String
)

fun FragranceDto.toDomain(): Result<Fragrance> {
    if (id == null || name == null) {
        return AppError.NetworkError.ParseError().asLeft()
    }
    return Fragrance(id, name).asRight()
}
```

#### ✅ DO: Provide Sensible Defaults
```kotlin
// ✅ CORRECT: Non-null with defaults
data class User(
    val id: String,
    val username: String,
    val bio: String = "",                    // Default empty string
    val profileImageUrl: String = "",        // Default empty string
    val followerCount: Int = 0,              // Default to zero
    val tags: List<String> = emptyList()     // Default empty list
)
```

#### ✅ DO: Use Nullable Only When Semantically Meaningful
```kotlin
// ✅ CORRECT: Nullable when absence has meaning
data class Fragrance(
    val id: String,
    val name: String,
    val releaseYear: Int? = null,            // Nullable - legitimately unknown
    val discontinuedDate: Long? = null       // Nullable - not all are discontinued
)
```

#### ❌ DON'T: Use Non-Null DTOs
```kotlin
// ❌ WRONG: Will crash if server doesn't send field
@Serializable
data class FragranceDto(
    val id: String,              // Crash if missing!
    val name: String             // Crash if missing!
)
```

#### ❌ DON'T: Expose Nullable Domain Models to UI
```kotlin
// ❌ WRONG: UI has to null-check everything
data class Fragrance(
    val id: String?,             // UI must handle null
    val name: String?,           // UI must handle null
    val brand: String?           // UI must handle null
)

// ✅ CORRECT: UI gets clean, non-null data
data class Fragrance(
    val id: String,
    val name: String,
    val brand: String
)
```

#### ❌ DON'T: Use !! (Force Unwrap)
```kotlin
// ❌ WRONG: Can crash at runtime
val name = dto.name!!        // Crash if null

// ✅ CORRECT: Handle null safely
val name = dto.name ?: "Unknown"
```

### 3. Safe Collection Handling

```kotlin
// ✅ CORRECT: Filter out nulls, never crash
@Serializable
data class PostDto(
    val tags: List<String?>? = null
)

fun PostDto.toDomain(): Post {
    return Post(
        // Handle nullable list and nullable elements
        tags = tags?.filterNotNull() ?: emptyList()
    )
}

// ✅ CORRECT: Safe iteration
fun processFragrances(fragrances: List<FragranceDto>?) {
    fragrances?.forEach { dto ->
        dto.toDomain().onRight { fragrance ->
            // Only process valid fragrances
        }
    }
}
```

### 4. Error Handling for Parse Failures

```kotlin
sealed class AppError {
    // ... existing errors ...

    sealed class NetworkError : AppError() {
        data class ParseError(
            override val message: String = "Failed to parse server response",
            override val cause: Throwable? = null,
            val fieldName: String? = null,           // Which field failed
            val receivedValue: String? = null        // What value was received
        ) : NetworkError()
    }
}

// Usage in mapper
fun FragranceDto.toDomain(): Result<Fragrance> {
    if (id.isNullOrBlank()) {
        return AppError.NetworkError.ParseError(
            message = "Fragrance ID is required but was missing",
            fieldName = "id",
            receivedValue = id
        ).asLeft()
    }
    // ... rest of mapping
}
```

---

## Core Architecture Principles (Continued)

**CRITICAL**: All operations that can fail MUST use `Either<AppError, T>` pattern. Never throw exceptions for expected failures.

#### Error Type Hierarchy
```kotlin
// shared/src/commonMain/kotlin/domain/error/AppError.kt
sealed class AppError {
    abstract val message: String
    abstract val cause: Throwable?

    // Network Errors
    sealed class NetworkError : AppError() {
        data class NoConnection(
            override val message: String = "No internet connection available",
            override val cause: Throwable? = null
        ) : NetworkError()

        data class Timeout(
            override val message: String = "Request timed out. Please try again",
            override val cause: Throwable? = null
        ) : NetworkError()

        data class ServerError(
            val statusCode: Int,
            override val message: String = "Server error occurred (Code: $statusCode)",
            override val cause: Throwable? = null
        ) : NetworkError()

        data class ParseError(
            override val message: String = "Failed to parse server response",
            override val cause: Throwable? = null
        ) : NetworkError()
    }

    // Authentication Errors
    sealed class AuthError : AppError() {
        data class InvalidCredentials(
            override val message: String = "Invalid email or password",
            override val cause: Throwable? = null
        ) : AuthError()

        data class UserAlreadyExists(
            override val message: String = "An account with this email already exists",
            override val cause: Throwable? = null
        ) : AuthError()

        data class TokenExpired(
            override val message: String = "Your session has expired. Please login again",
            override val cause: Throwable? = null
        ) : AuthError()

        data class Unauthorized(
            override val message: String = "You are not authorized to perform this action",
            override val cause: Throwable? = null
        ) : AuthError()
    }

    // Validation Errors
    sealed class ValidationError : AppError() {
        data class InvalidEmail(
            override val message: String = "Please enter a valid email address",
            override val cause: Throwable? = null
        ) : ValidationError()

        data class PasswordTooShort(
            val minLength: Int = 8,
            override val message: String = "Password must be at least $minLength characters",
            override val cause: Throwable? = null
        ) : ValidationError()

        data class RequiredFieldEmpty(
            val fieldName: String,
            override val message: String = "$fieldName is required",
            override val cause: Throwable? = null
        ) : ValidationError()

        data class InvalidInput(
            val fieldName: String,
            override val message: String = "Invalid $fieldName",
            override val cause: Throwable? = null
        ) : ValidationError()
    }

    // Content Errors
    sealed class ContentError : AppError() {
        data class FragranceNotFound(
            val fragranceId: String,
            override val message: String = "Fragrance not found",
            override val cause: Throwable? = null
        ) : ContentError()

        data class PostNotFound(
            val postId: String,
            override val message: String = "Post not found",
            override val cause: Throwable? = null
        ) : ContentError()

        data class UploadFailed(
            override val message: String = "Failed to upload media. Please try again",
            override val cause: Throwable? = null
        ) : ContentError()

        data class InsufficientPermissions(
            override val message: String = "You don't have permission to perform this action",
            override val cause: Throwable? = null
        ) : ContentError()
    }

    // Storage Errors
    sealed class StorageError : AppError() {
        data class ReadFailed(
            override val message: String = "Failed to read data from storage",
            override val cause: Throwable? = null
        ) : StorageError()

        data class WriteFailed(
            override val message: String = "Failed to save data",
            override val cause: Throwable? = null
        ) : StorageError()

        data class CacheMiss(
            override val message: String = "Data not found in cache",
            override val cause: Throwable? = null
        ) : StorageError()
    }

    // Generic/Unknown Errors
    data class Unknown(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError()
}
```

#### Either Type Implementation
```kotlin
// shared/src/commonMain/kotlin/domain/util/Either.kt
sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()
    data class Right<out R>(val value: R) : Either<Nothing, R>()

    val isRight get() = this is Right<R>
    val isLeft get() = this is Left<L>

    fun <L> left(a: L) = Left(a)
    fun <R> right(b: R) = Right(b)

    inline fun <C> fold(ifLeft: (L) -> C, ifRight: (R) -> C): C =
        when (this) {
            is Left -> ifLeft(value)
            is Right -> ifRight(value)
        }

    inline fun <C> map(f: (R) -> C): Either<L, C> =
        when (this) {
            is Left -> Left(value)
            is Right -> Right(f(value))
        }

    inline fun <C> flatMap(f: (R) -> Either<L, C>): Either<L, C> =
        when (this) {
            is Left -> Left(value)
            is Right -> f(value)
        }

    inline fun onRight(action: (R) -> Unit): Either<L, R> {
        if (this is Right) action(value)
        return this
    }

    inline fun onLeft(action: (L) -> Unit): Either<L, R> {
        if (this is Left) action(value)
        return this
    }

    fun getOrNull(): R? = when (this) {
        is Right -> value
        is Left -> null
    }

    fun leftOrNull(): L? = when (this) {
        is Left -> value
        is Right -> null
    }
}

// Convenience functions
fun <R> R.asRight(): Either<Nothing, R> = Either.Right(this)
fun <L> L.asLeft(): Either<L, Nothing> = Either.Left(this)

// Type alias for common use case
typealias Result<T> = Either<AppError, T>
```

### 2. Repository Pattern with Either

**ALL repository methods MUST return `Either<AppError, T>`**

```kotlin
// shared/src/commonMain/kotlin/domain/repository/FragranceRepository.kt
interface FragranceRepository {
    suspend fun searchFragrances(
        query: String,
        page: Int = 0,
        limit: Int = 20
    ): Result<List<Fragrance>>

    suspend fun getFragranceById(id: String): Result<Fragrance>

    suspend fun getSimilarFragrances(id: String): Result<List<Fragrance>>
}

// Implementation example
class FragranceRepositoryImpl(
    private val apiClient: ApiClient,
    private val localCache: FragranceCache
) : FragranceRepository {

    override suspend fun searchFragrances(
        query: String,
        page: Int,
        limit: Int
    ): Result<List<Fragrance>> {
        return try {
            // Try cache first
            val cached = localCache.search(query, page, limit)
            if (cached.isNotEmpty()) {
                return cached.asRight()
            }

            // Fetch from API
            val response = apiClient.searchFragrances(query, page, limit)

            when {
                response.isSuccessful && response.data != null -> {
                    localCache.save(response.data)
                    response.data.asRight()
                }
                response.statusCode == 404 -> {
                    emptyList<Fragrance>().asRight()
                }
                response.statusCode in 500..599 -> {
                    AppError.NetworkError.ServerError(
                        statusCode = response.statusCode,
                        message = response.message ?: "Server error"
                    ).asLeft()
                }
                else -> {
                    AppError.NetworkError.ParseError(
                        message = "Failed to parse response"
                    ).asLeft()
                }
            }
        } catch (e: UnknownHostException) {
            AppError.NetworkError.NoConnection().asLeft()
        } catch (e: SocketTimeoutException) {
            AppError.NetworkError.Timeout().asLeft()
        } catch (e: Exception) {
            AppError.Unknown(
                message = "Unexpected error: ${e.message}",
                cause = e
            ).asLeft()
        }
    }
}
```

### 3. ViewModel Error Handling

**ALL ViewModels MUST handle errors properly and expose UI state**

```kotlin
// android/src/main/kotlin/ui/base/UiState.kt
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val error: AppError) : UiState<Nothing>()
}

// android/src/main/kotlin/ui/base/BaseViewModel.kt
abstract class BaseViewModel : ViewModel() {

    private val _error = MutableSharedFlow<AppError>()
    val error: SharedFlow<AppError> = _error.asSharedFlow()

    protected fun handleError(error: AppError) {
        viewModelScope.launch {
            _error.emit(error)
        }
    }

    protected fun <T> Either<AppError, T>.handleResult(
        onSuccess: (T) -> Unit,
        onError: ((AppError) -> Unit)? = null
    ) {
        fold(
            ifLeft = { error ->
                onError?.invoke(error) ?: handleError(error)
            },
            ifRight = { data ->
                onSuccess(data)
            }
        )
    }
}

// Example ViewModel
class HomeViewModel(
    private val postRepository: PostRepository,
    private val fragranceRepository: FragranceRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<UiState<HomeScreenData>>(UiState.Idle)
    val uiState: StateFlow<UiState<HomeScreenData>> = _uiState.asStateFlow()

    fun loadFeed(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            postRepository.getFeed(page = 0, limit = 20)
                .handleResult(
                    onSuccess = { posts ->
                        _uiState.value = UiState.Success(
                            HomeScreenData(posts = posts)
                        )
                    },
                    onError = { error ->
                        _uiState.value = UiState.Error(error)
                        handleError(error)
                    }
                )
        }
    }

    fun searchFragrances(query: String) {
        if (query.isBlank()) {
            handleError(
                AppError.ValidationError.RequiredFieldEmpty("Search query")
            )
            return
        }

        viewModelScope.launch {
            fragranceRepository.searchFragrances(query)
                .handleResult(
                    onSuccess = { fragrances ->
                        // Handle success
                    },
                    onError = { error ->
                        // Error automatically handled by base class
                    }
                )
        }
    }
}
```

### 4. Composable Error Handling

#### Base Error Screen Component
```kotlin
// android/src/main/kotlin/ui/components/error/ErrorScreen.kt
@Composable
fun BaseErrorScreen(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val errorInfo = remember(error) { error.toErrorInfo() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = errorInfo.icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = errorInfo.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }

        if (onDismiss != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

data class ErrorInfo(
    val title: String,
    val icon: ImageVector
)

fun AppError.toErrorInfo(): ErrorInfo = when (this) {
    is AppError.NetworkError.NoConnection -> ErrorInfo(
        title = "No Internet Connection",
        icon = Icons.Default.WifiOff
    )
    is AppError.NetworkError.Timeout -> ErrorInfo(
        title = "Request Timed Out",
        icon = Icons.Default.HourglassEmpty
    )
    is AppError.NetworkError.ServerError -> ErrorInfo(
        title = "Server Error",
        icon = Icons.Default.Error
    )
    is AppError.AuthError.TokenExpired -> ErrorInfo(
        title = "Session Expired",
        icon = Icons.Default.Lock
    )
    is AppError.AuthError.InvalidCredentials -> ErrorInfo(
        title = "Invalid Credentials",
        icon = Icons.Default.Lock
    )
    is AppError.ValidationError -> ErrorInfo(
        title = "Invalid Input",
        icon = Icons.Default.Warning
    )
    is AppError.ContentError.FragranceNotFound -> ErrorInfo(
        title = "Fragrance Not Found",
        icon = Icons.Default.SearchOff
    )
    else -> ErrorInfo(
        title = "Something Went Wrong",
        icon = Icons.Default.Error
    )
}
```

#### Inline Error Display
```kotlin
// android/src/main/kotlin/ui/components/error/InlineError.kt
@Composable
fun InlineErrorMessage(
    error: AppError,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )

            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
```

#### Snackbar Error Display
```kotlin
// android/src/main/kotlin/ui/components/error/ErrorSnackbar.kt
@Composable
fun ErrorSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            actionColor = MaterialTheme.colorScheme.error
        )
    }
}

// Extension function for ViewModels
fun SnackbarHostState.showError(
    error: AppError,
    actionLabel: String? = null,
    scope: CoroutineScope
) {
    scope.launch {
        showSnackbar(
            message = error.message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Short
        )
    }
}
```

### 5. Screen-Level Error Handling Pattern

```kotlin
// android/src/main/kotlin/ui/home/HomeScreen.kt
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Observe errors from ViewModel
    LaunchedEffect(Unit) {
        viewModel.error.collect { error ->
            snackbarHostState.showError(
                error = error,
                actionLabel = if (error is AppError.NetworkError) "Retry" else null,
                scope = scope
            )
        }
    }

    Scaffold(
        snackbarHost = { ErrorSnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is UiState.Idle -> {
                // Initial state
            }

            is UiState.Loading -> {
                LoadingIndicator()
            }

            is UiState.Success -> {
                HomeContent(
                    data = state.data,
                    onRefresh = { viewModel.loadFeed(refresh = true) }
                )
            }

            is UiState.Error -> {
                BaseErrorScreen(
                    error = state.error,
                    onRetry = { viewModel.loadFeed(refresh = true) }
                )
            }
        }
    }
}
```

### 6. Form Validation with Either

```kotlin
// shared/src/commonMain/kotlin/domain/validation/Validator.kt
object Validator {

    fun validateEmail(email: String): Result<String> {
        return when {
            email.isBlank() -> AppError.ValidationError.RequiredFieldEmpty("Email").asLeft()
            !email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) ->
                AppError.ValidationError.InvalidEmail().asLeft()
            else -> email.asRight()
        }
    }

    fun validatePassword(password: String): Result<String> {
        return when {
            password.isBlank() -> AppError.ValidationError.RequiredFieldEmpty("Password").asLeft()
            password.length < 8 -> AppError.ValidationError.PasswordTooShort().asLeft()
            else -> password.asRight()
        }
    }

    fun validateUsername(username: String): Result<String> {
        return when {
            username.isBlank() -> AppError.ValidationError.RequiredFieldEmpty("Username").asLeft()
            username.length < 3 -> AppError.ValidationError.InvalidInput(
                fieldName = "Username",
                message = "Username must be at least 3 characters"
            ).asLeft()
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> AppError.ValidationError.InvalidInput(
                fieldName = "Username",
                message = "Username can only contain letters, numbers, and underscores"
            ).asLeft()
            else -> username.asRight()
        }
    }
}
```

---

## Navigation Architecture for Compose Multiplatform

**CRITICAL**: Since official Compose Multiplatform Navigation is not yet stable, use simple state-based navigation for now, designed for easy migration to official navigation later. The app uses a **bottom-nav / tabbed** structure: each tab owns an isolated back stack, and switching tabs preserves each tab's position.

### 1. Tab and Route Definitions (Current Approach)

The top-level navigation surface is a fixed set of four tabs. Each tab owns its own sealed route hierarchy listing **only** the destinations reachable within that tab's stack.

#### Tab Definition
```kotlin
// composeApp/src/commonMain/kotlin/navigation/Tab.kt
sealed interface Tab {
    data object Home : Tab
    data object Marketplace : Tab
    data object Search : Tab
    data object Profile : Tab

    companion object {
        val ROOT: Tab = Home                 // the tab back-press falls through to
        val all: List<Tab> = listOf(Home, Marketplace, Search, Profile)
    }
}
```

#### Per-Tab Route Definitions
```kotlin
// composeApp/src/commonMain/kotlin/navigation/Routes.kt

// Home — the short-form video feed and everything reachable from it.
sealed interface HomeRoute {
    data object Feed : HomeRoute
    data class PostDetail(val postId: String) : HomeRoute
    data class UserProfile(val userId: String) : HomeRoute          // shared destination
    data class FragranceDetail(val fragranceId: String) : HomeRoute  // shared destination
}

// Marketplace — the e-commerce section.
sealed interface MarketplaceRoute {
    data object Browse : MarketplaceRoute
    data class ListingDetail(val listingId: String) : MarketplaceRoute  // shared destination
    data class FragranceDetail(val fragranceId: String) : MarketplaceRoute // shared destination
    data object Cart : MarketplaceRoute
    data object Checkout : MarketplaceRoute
}

// Search — searches ALL content types (fragrances, listings, posts/users),
// not just marketplace items. Result rows navigate into shared detail screens.
sealed interface SearchRoute {
    data object Query : SearchRoute
    data class Results(val query: String) : SearchRoute
    data class FragranceDetail(val fragranceId: String) : SearchRoute  // shared destination
    data class ListingDetail(val listingId: String) : SearchRoute      // shared destination
    data class UserProfile(val userId: String) : SearchRoute           // shared destination
}

// Profile — all user / settings related screens.
sealed interface ProfileRoute {
    data object Profile : ProfileRoute
    data object Settings : ProfileRoute
    data object EditProfile : ProfileRoute
    data object MyListings : ProfileRoute
}
```

#### ✅ Shared Destinations Are Intentionally Duplicated

`FragranceDetail`, `ListingDetail`, and `UserProfile` appear in multiple tab hierarchies **by design**. Do NOT extract them into a shared `DetailRoute` sealed interface.

**Rationale:**
- Each tab's back stack stays fully self-contained and type-safe.
- Back navigation and up-navigation targets stay unambiguous per tab — "fragrance detail reached from Search" and "from Marketplace" can carry different back-stack context.
- A shared `DetailRoute` reintroduces "which tab does this detail belong to?" ambiguity and complicates the typed per-tab `NavigationState`.
  Accept the verbosity. Revisit this decision **only** if the shared-detail set grows large (many more shared destinations than tab-specific ones).

### 2. Per-Tab Navigation State

Each tab gets its **own** `NavigationState` instance holding an isolated back stack. `Route` is the common upper bound for all per-tab route hierarchies — keep it as a generic so each state instance is typed to its tab's routes.

```kotlin
// composeApp/src/commonMain/kotlin/navigation/NavigationState.kt
class NavigationState<Route>(private val root: Route) {
    private val _currentScreen = mutableStateOf(root)
    val currentScreen: State<Route> = _currentScreen

    private val _backStack = mutableStateListOf<Route>()

    /** True when this tab's stack is above its root (i.e. there is somewhere to pop to). */
    val canGoBack: Boolean get() = _backStack.isNotEmpty()

    /** True when this tab is sitting at its root screen. */
    val isAtRoot: Boolean get() = _backStack.isEmpty()

    fun navigateTo(screen: Route) {
        _backStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    /** Pops one entry. Returns false when already at root (nothing to pop). */
    fun goBack(): Boolean {
        val previous = _backStack.removeLastOrNull() ?: return false
        _currentScreen.value = previous
        return true
    }

    fun popToRoot() {
        _backStack.clear()
        _currentScreen.value = root
    }
}
```

### 3. App-Level Navigator (Ties the Tabs Together)

A single `AppNavigator` holds one `NavigationState` per tab in a `Map<Tab, NavigationState<*>>`, tracks the active tab, and centralises the global back-press policy.

```kotlin
// composeApp/src/commonMain/kotlin/navigation/AppNavigator.kt
class AppNavigator {
    private val _activeTab = mutableStateOf<Tab>(Tab.ROOT)
    val activeTab: State<Tab> = _activeTab

    // One isolated back stack per tab.
    val home = NavigationState<HomeRoute>(HomeRoute.Feed)
    val marketplace = NavigationState<MarketplaceRoute>(MarketplaceRoute.Browse)
    val search = NavigationState<SearchRoute>(SearchRoute.Query)
    val profile = NavigationState<ProfileRoute>(ProfileRoute.Profile)

    private val states: Map<Tab, NavigationState<*>> = mapOf(
        Tab.Home to home,
        Tab.Marketplace to marketplace,
        Tab.Search to search,
        Tab.Profile to profile,
    )

    private fun stateFor(tab: Tab): NavigationState<*> = states.getValue(tab)

    /** Tapping a bottom-nav item. Re-tapping the active tab pops it to root (standard behaviour). */
    fun selectTab(tab: Tab) {
        if (tab == _activeTab.value) {
            stateFor(tab).popToRoot()
        } else {
            _activeTab.value = tab
        }
    }

    /**
     * Global back-press policy:
     *  1. If the active tab can pop within its own stack, pop it.
     *  2. Else, if we're not on the ROOT (Home) tab, switch to Home.
     *  3. Else (Home tab, at its root) return false so the host can exit the app.
     */
    fun onBackPressed(): Boolean {
        val active = _activeTab.value
        val activeState = stateFor(active)

        if (activeState.canGoBack) {
            activeState.goBack()
            return true
        }

        if (active != Tab.ROOT) {
            _activeTab.value = Tab.ROOT
            return true
        }

        // On Home tab, already at root → let the platform close the app.
        return false
    }
}
```

**Back-press policy (explicit):**

| Situation | Result |
|---|---|
| Active tab has entries in its back stack | Pop one screen within the tab |
| Active tab is at its root, and it is **not** Home | Switch to the Home tab |
| Home tab, at its root | Return `false` → host exits the app |

### 4. Navigation Host

The host renders the active tab's current screen, wires the bottom bar to `selectTab`, and routes the platform back gesture through `onBackPressed`.

```kotlin
// composeApp/src/commonMain/kotlin/navigation/AppNavigation.kt
@Composable
fun AppNavigation(
    navigator: AppNavigator = remember { AppNavigator() },
    onExitApp: () -> Unit,               // Android: call (activity)::finish
) {
    val activeTab by navigator.activeTab

    // Route the system back gesture through the global policy.
    // BackHandler is Android/androidMain; wrap in an expect/actual for iOS.
    BackHandler(enabled = true) {
        val handled = navigator.onBackPressed()
        if (!handled) onExitApp()
    }

    Scaffold(
        bottomBar = {
            AppBottomBar(
                activeTab = activeTab,
                onTabSelected = navigator::selectTab,
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (activeTab) {
                Tab.Home -> HomeTabHost(navigator.home)
                Tab.Marketplace -> MarketplaceTabHost(navigator.marketplace)
                Tab.Search -> SearchTabHost(navigator.search)
                Tab.Profile -> ProfileTabHost(navigator.profile)
            }
        }
    }
}
```

Each tab host is a `when` over its own route type — for example:

```kotlin
@Composable
private fun HomeTabHost(state: NavigationState<HomeRoute>) {
    when (val screen = state.currentScreen.value) {
        is HomeRoute.Feed -> FeedScreen(
            onOpenPost = { state.navigateTo(HomeRoute.PostDetail(it)) },
            onOpenFragrance = { state.navigateTo(HomeRoute.FragranceDetail(it)) },
            onOpenUser = { state.navigateTo(HomeRoute.UserProfile(it)) },
        )
        is HomeRoute.PostDetail -> PostDetailScreen(
            postId = screen.postId,
            onBackClick = { state.goBack() },
        )
        is HomeRoute.FragranceDetail -> FragranceDetailScreen(
            fragranceId = screen.fragranceId,
            onBackClick = { state.goBack() },
        )
        is HomeRoute.UserProfile -> UserProfileScreen(
            userId = screen.userId,
            onBackClick = { state.goBack() },
        )
    }
}
```

### 5. Navigation Best Practices

#### ✅ DO: Pass Navigation Callbacks Down
```kotlin
@Composable
fun ParentScreen(
    onNavigateToDetail: (String) -> Unit
) {
    ChildComponent(
        onItemClick = onNavigateToDetail
    )
}
```

#### ❌ DON'T: Navigate from Deep Components
```kotlin
// ❌ WRONG: Deep component shouldn't know about navigation state
@Composable
fun DeepChildComponent(state: NavigationState<HomeRoute>) {
    Button(
        onClick = { state.navigateTo(HomeRoute.Feed) }
    ) { Text("Navigate") }
}

// ✅ CORRECT: Use callbacks
@Composable
fun DeepChildComponent(onNavigate: () -> Unit) {
    Button(onClick = onNavigate) { Text("Navigate") }
}
```

#### ✅ DO: Keep Shared Destinations Duplicated Per Tab
Navigate to the **current tab's own** copy of a shared destination, so the entry stays on that tab's stack:
```kotlin
// Inside the Search tab, opening a fragrance stays within Search's stack:
onOpenFragrance = { id -> searchState.navigateTo(SearchRoute.FragranceDetail(id)) }

// Inside Marketplace, the same tap uses Marketplace's own route:
onOpenFragrance = { id -> marketplaceState.navigateTo(MarketplaceRoute.FragranceDetail(id)) }
```

### 6. Migration Strategy to Official Navigation

The per-tab structure maps cleanly onto official Compose Multiplatform Navigation's **nested graphs** — each `Tab` becomes a nested graph, and each per-tab route becomes a destination within it.

```kotlin
// When official navigation becomes available:

// From (per-tab sealed route):
sealed interface MarketplaceRoute {
    data class FragranceDetail(val fragranceId: String) : MarketplaceRoute
}

// To (nested graph route):
object MarketplaceRoutes {
    const val GRAPH = "marketplace"
    const val FRAGRANCE_DETAIL = "marketplace/fragrance/{fragranceId}"
    fun fragranceDetail(id: String) = "marketplace/fragrance/$id"
}

// From:
marketplaceState.navigateTo(MarketplaceRoute.FragranceDetail("123"))

// To:
navController.navigate(MarketplaceRoutes.fragranceDetail("123"))
```

Because each tab already owns an isolated back stack, the per-tab `NavigationState` instances become per-graph `NavController` back stacks with no change to the tab structure or the shared-destination duplication.

## Mandatory Patterns

### ✅ DO
- ✅ Always return `Either<AppError, T>` from repositories
- ✅ Use sealed classes for all error types
- ✅ Handle errors in ViewModels with `UiState`
- ✅ Display errors using base error screen components
- ✅ Validate inputs before making network calls
- ✅ Use meaningful error messages for users
- ✅ Log errors with stack traces for debugging
- ✅ Provide retry mechanisms for recoverable errors
- ✅ Use type-safe error handling throughout
### ❌ DON'T
- ❌ Never throw exceptions for expected failures
- ❌ Don't use nullable types for error handling
- ❌ Don't catch generic `Exception` without re-wrapping
- ❌ Don't expose raw HTTP status codes to UI
- ❌ Don't use string error messages directly
- ❌ Don't ignore errors silently
- ❌ Don't use `try-catch` for flow control
- ❌ Don't create error types outside the sealed hierarchy
## Testing Error Scenarios

```kotlin
class FragranceRepositoryTest {

    @Test
    fun `searchFragrances returns Left when network unavailable`() = runTest {
        // Given
        val repository = FragranceRepositoryImpl(
            apiClient = FakeApiClient(simulateNoConnection = true),
            localCache = FakeCache()
        )

        // When
        val result = repository.searchFragrances("test")

        // Then
        assertTrue(result.isLeft)
        assertTrue(result.leftOrNull() is AppError.NetworkError.NoConnection)
    }

    @Test
    fun `validateEmail returns Left for invalid email`() {
        // When
        val result = Validator.validateEmail("invalid-email")

        // Then
        assertTrue(result.isLeft)
        assertTrue(result.leftOrNull() is AppError.ValidationError.InvalidEmail)
    }
}
```

## Error Logging

```kotlin
// shared/src/commonMain/kotlin/util/Logger.kt
object ErrorLogger {
    fun log(error: AppError, context: String? = null) {
        when (error) {
            is AppError.Unknown -> {
                // Log with full stack trace
                println("UNKNOWN ERROR in $context: ${error.message}")
                error.cause?.printStackTrace()
            }
            is AppError.NetworkError.ServerError -> {
                // Log server errors with status code
                println("SERVER ERROR ${error.statusCode} in $context: ${error.message}")
            }
            else -> {
                // Log other errors
                println("ERROR in $context: ${error::class.simpleName} - ${error.message}")
            }
        }
    }
}
```

---

## Dependency Injection with Koin

### 1. Koin Setup and Module Structure

**CRITICAL**: Use Koin for dependency injection. Structure modules by feature/layer for clarity and testability.

#### Koin Configuration
```kotlin
// android/src/main/kotlin/FragranceApplication.kt
class FragranceApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR) // Only show errors in production
            androidContext(this@FragranceApplication)
            modules(
                networkModule,
                databaseModule,
                repositoryModule,
                useCaseModule,
                viewModelModule
            )
        }
    }
}
```

#### Network Module
```kotlin
// android/src/main/kotlin/di/NetworkModule.kt
val networkModule = module {

    // HTTP Client
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        if (BuildConfig.DEBUG) {
                            println("HTTP: $message")
                        }
                    }
                }
                level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }

            // Add auth token to requests
            install(Auth) {
                bearer {
                    loadTokens {
                        val token = get<TokenManager>().getToken()
                        BearerTokens(token ?: "", "")
                    }
                }
            }
        }
    }

    // API Client
    single<ApiClient> {
        ApiClientImpl(
            httpClient = get(),
            baseUrl = BuildConfig.API_BASE_URL
        )
    }

    // Token Manager
    single {
        TokenManager(context = androidContext())
    }
}
```

#### Database Module
```kotlin
// android/src/main/kotlin/di/DatabaseModule.kt
val databaseModule = module {

    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            FragranceDatabase::class.java,
            "fragrance_database"
        )
            .fallbackToDestructiveMigration() // Remove in production
            .build()
    }

    // DAOs
    single { get<FragranceDatabase>().userFragranceDao() }
    single { get<FragranceDatabase>().cachedFragranceDao() }
    single { get<FragranceDatabase>().postDao() }
    single { get<FragranceDatabase>().listingDao() }

    // DataStore
    single {
        androidContext().dataStore
    }
}

// Extension for DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")
```

#### Repository Module
```kotlin
// android/src/main/kotlin/di/RepositoryModule.kt
val repositoryModule = module {

    single<AuthRepository> {
        AuthRepositoryImpl(
            apiClient = get(),
            tokenManager = get()
        )
    }

    single<FragranceRepository> {
        FragranceRepositoryImpl(
            apiClient = get(),
            cachedFragranceDao = get(),
            userFragranceDao = get()
        )
    }

    single<PostRepository> {
        PostRepositoryImpl(
            apiClient = get(),
            postDao = get()
        )
    }

    single<ListingRepository> {
        ListingRepositoryImpl(
            apiClient = get(),
            listingDao = get()
        )
    }

    single<UserRepository> {
        UserRepositoryImpl(
            apiClient = get(),
            dataStore = get()
        )
    }
}
```

#### Use Case Module (Optional but Recommended)
```kotlin
// android/src/main/kotlin/di/UseCaseModule.kt
val useCaseModule = module {

    // Authentication Use Cases
    factory { LoginUseCase(authRepository = get()) }
    factory { RegisterUseCase(authRepository = get()) }
    factory { LogoutUseCase(authRepository = get(), tokenManager = get()) }

    // Fragrance Use Cases
    factory { SearchFragrancesUseCase(fragranceRepository = get()) }
    factory { GetFragranceDetailsUseCase(fragranceRepository = get()) }
    factory { AddToCollectionUseCase(fragranceRepository = get()) }

    // Post Use Cases
    factory { CreatePostUseCase(postRepository = get()) }
    factory { GetFeedUseCase(postRepository = get()) }
    factory { LikePostUseCase(postRepository = get()) }
    factory { CommentOnPostUseCase(postRepository = get()) }

    // Marketplace Use Cases
    factory { CreateListingUseCase(listingRepository = get()) }
    factory { SearchListingsUseCase(listingRepository = get()) }
    factory { PurchaseListingUseCase(listingRepository = get()) }
}
```

#### ViewModel Module
```kotlin
// android/src/main/kotlin/di/ViewModelModule.kt
val viewModelModule = module {

    // Auth ViewModels
    viewModel { LoginViewModel(loginUseCase = get()) }
    viewModel { RegisterViewModel(registerUseCase = get()) }

    // Home ViewModels
    viewModel { HomeViewModel(getFeedUseCase = get(), fragranceRepository = get()) }

    // Post ViewModels
    viewModel {
        PostCreationViewModel(
            createPostUseCase = get(),
            searchFragrancesUseCase = get()
        )
    }

    viewModel { parameters ->
        PostDetailViewModel(
            postId = parameters.get(),
            postRepository = get()
        )
    }

    // Profile ViewModels
    viewModel { parameters ->
        ProfileViewModel(
            userId = parameters.get(),
            userRepository = get(),
            postRepository = get()
        )
    }

    // Marketplace ViewModels
    viewModel {
        MarketplaceViewModel(
            searchListingsUseCase = get(),
            fragranceRepository = get()
        )
    }

    viewModel { parameters ->
        ListingDetailViewModel(
            listingId = parameters.get(),
            listingRepository = get()
        )
    }
}
```

### 2. Use Case Pattern (Recommended)

**Use Cases encapsulate single business operations and make testing easier**

```kotlin
// shared/src/commonMain/kotlin/domain/usecase/UseCase.kt
interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): Result<R>
}

// No parameters use case
interface NoParamsUseCase<out R> {
    suspend operator fun invoke(): Result<R>
}

// Example Use Case
class SearchFragrancesUseCase(
    private val fragranceRepository: FragranceRepository
) : UseCase<SearchFragrancesUseCase.Params, List<Fragrance>> {

    data class Params(
        val query: String,
        val page: Int = 0,
        val limit: Int = 20
    )

    override suspend fun invoke(params: Params): Result<List<Fragrance>> {
        // Validate input
        if (params.query.isBlank()) {
            return AppError.ValidationError.RequiredFieldEmpty("Search query").asLeft()
        }

        // Call repository
        return fragranceRepository.searchFragrances(
            query = params.query,
            page = params.page,
            limit = params.limit
        )
    }
}

// Usage in ViewModel
class SearchViewModel(
    private val searchFragrancesUseCase: SearchFragrancesUseCase
) : BaseViewModel() {

    fun search(query: String) {
        viewModelScope.launch {
            searchFragrancesUseCase(
                SearchFragrancesUseCase.Params(query = query)
            ).handleResult(
                onSuccess = { fragrances ->
                    // Handle success
                },
                onError = { error ->
                    handleError(error)
                }
            )
        }
    }
}
```

### 3. Constructor Injection (Always Use This)

**✅ DO: Constructor Injection**
```kotlin
class HomeViewModel(
    private val getFeedUseCase: GetFeedUseCase,
    private val fragranceRepository: FragranceRepository
) : BaseViewModel() {
    // Dependencies are explicit and testable
}
```

**❌ DON'T: Service Locator Pattern**
```kotlin
class HomeViewModel : BaseViewModel() {
    private val getFeedUseCase = get<GetFeedUseCase>() // Hard to test
}
```

### 4. Testing with Dependency Injection

#### Test Module
```kotlin
// android/src/test/kotlin/di/TestModule.kt
val testModule = module {

    // Mock repositories
    single<FragranceRepository> { FakeFragranceRepository() }
    single<PostRepository> { FakePostRepository() }
    single<AuthRepository> { FakeAuthRepository() }

    // Real use cases (testing business logic)
    factory { SearchFragrancesUseCase(fragranceRepository = get()) }
    factory { CreatePostUseCase(postRepository = get()) }
}
```

#### Unit Test with Koin
```kotlin
class SearchFragrancesUseCaseTest : KoinTest {

    private lateinit var searchUseCase: SearchFragrancesUseCase
    private lateinit var fakeRepository: FakeFragranceRepository

    @Before
    fun setup() {
        startKoin {
            modules(testModule)
        }
        searchUseCase = get()
        fakeRepository = get<FragranceRepository>() as FakeFragranceRepository
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `search returns fragrances when query is valid`() = runTest {
        // Given
        val expectedFragrances = listOf(
            Fragrance(id = "1", name = "Test", brand = "Brand")
        )
        fakeRepository.setFragrances(expectedFragrances)

        // When
        val result = searchUseCase(
            SearchFragrancesUseCase.Params(query = "Test")
        )

        // Then
        assertTrue(result.isRight)
        assertEquals(expectedFragrances, result.getOrNull())
    }

    @Test
    fun `search returns validation error when query is empty`() = runTest {
        // When
        val result = searchUseCase(
            SearchFragrancesUseCase.Params(query = "")
        )

        // Then
        assertTrue(result.isLeft)
        assertTrue(result.leftOrNull() is AppError.ValidationError.RequiredFieldEmpty)
    }
}
```

#### ViewModel Test without Koin
```kotlin
class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel
    private lateinit var fakeGetFeedUseCase: FakeGetFeedUseCase
    private lateinit var fakeFragranceRepository: FakeFragranceRepository

    @Before
    fun setup() {
        fakeGetFeedUseCase = FakeGetFeedUseCase()
        fakeFragranceRepository = FakeFragranceRepository()

        viewModel = HomeViewModel(
            getFeedUseCase = fakeGetFeedUseCase,
            fragranceRepository = fakeFragranceRepository
        )
    }

    @Test
    fun `loadFeed updates state to Success when use case succeeds`() = runTest {
        // Given
        val expectedPosts = listOf(
            Post(id = "1", userId = "user1", contentFormat = ContentFormat.TEXT)
        )
        fakeGetFeedUseCase.setResult(expectedPosts.asRight())

        // When
        viewModel.loadFeed()

        // Then
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(expectedPosts, (state as UiState.Success).data.posts)
    }

    @Test
    fun `loadFeed updates state to Error when use case fails`() = runTest {
        // Given
        val error = AppError.NetworkError.NoConnection()
        fakeGetFeedUseCase.setResult(error.asLeft())

        // When
        viewModel.loadFeed()

        // Then
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals(error, (state as UiState.Error).error)
    }
}
```

---

## Unit Testing Best Practices

### 1. Test Structure (Arrange-Act-Assert)

```kotlin
@Test
fun `descriptive test name in backticks describing behavior`() = runTest {
    // Arrange (Given)
    val input = "test input"
    val expectedOutput = "expected result"
    repository.setupTestData()

    // Act (When)
    val result = systemUnderTest.performAction(input)

    // Assert (Then)
    assertEquals(expectedOutput, result)
    verify(mockDependency).wasCalledWith(input)
}
```

### 2. Test Naming Convention

**Pattern**: `[method/feature] [scenario] [expected result]`

```kotlin
// ✅ Good test names
@Test fun `login with valid credentials returns success`()
@Test fun `login with invalid credentials returns auth error`()
@Test fun `login with no network returns network error`()
@Test fun `search with empty query returns validation error`()

// ❌ Bad test names
@Test fun testLogin()
@Test fun test1()
@Test fun loginTest()
```

### 3. Fake Implementations for Testing

```kotlin
// android/src/test/kotlin/fakes/FakeFragranceRepository.kt
class FakeFragranceRepository : FragranceRepository {

    private var fragrances = mutableListOf<Fragrance>()
    private var shouldReturnError = false
    private var errorToReturn: AppError? = null

    fun setFragrances(fragrances: List<Fragrance>) {
        this.fragrances = fragrances.toMutableList()
    }

    fun setError(error: AppError) {
        shouldReturnError = true
        errorToReturn = error
    }

    override suspend fun searchFragrances(
        query: String,
        page: Int,
        limit: Int
    ): Result<List<Fragrance>> {
        if (shouldReturnError) {
            return (errorToReturn ?: AppError.Unknown()).asLeft()
        }

        val results = fragrances.filter { fragrance ->
            fragrance.name.contains(query, ignoreCase = true) ||
            fragrance.brand.contains(query, ignoreCase = true)
        }

        return results.asRight()
    }

    override suspend fun getFragranceById(id: String): Result<Fragrance> {
        if (shouldReturnError) {
            return (errorToReturn ?: AppError.Unknown()).asLeft()
        }

        val fragrance = fragrances.find { it.id == id }
        return fragrance?.asRight()
            ?: AppError.ContentError.FragranceNotFound(id).asLeft()
    }
}
```

### 4. Testing ViewModels with Turbine

```kotlin
// Add Turbine dependency
// testImplementation("app.cash.turbine:turbine:1.0.0")

class HomeViewModelTest {

    @Test
    fun `uiState emits Loading then Success when loadFeed succeeds`() = runTest {
        // Given
        val posts = listOf(Post(id = "1"))
        fakeGetFeedUseCase.setResult(posts.asRight())
        val viewModel = HomeViewModel(fakeGetFeedUseCase, fakeFragranceRepository)

        // When/Then
        viewModel.uiState.test {
            assertEquals(UiState.Idle, awaitItem())

            viewModel.loadFeed()

            assertEquals(UiState.Loading, awaitItem())

            val successState = awaitItem()
            assertTrue(successState is UiState.Success)
            assertEquals(posts, (successState as UiState.Success).data.posts)
        }
    }
}
```

### 5. Testing Coroutines

```kotlin
// Use runTest for coroutine tests
@Test
fun `async operation completes successfully`() = runTest {
    // This automatically waits for all coroutines to complete
    val result = repository.fetchData()
    assertTrue(result.isRight)
}

// Testing with delays
@Test
fun `operation with delay works correctly`() = runTest {
    val startTime = currentTime

    viewModel.performDelayedAction()
    advanceTimeBy(5000) // Fast-forward time

    val endTime = currentTime
    assertEquals(5000, endTime - startTime)
}
```

### 6. Testing Error Scenarios

```kotlin
class PostRepositoryTest {

    @Test
    fun `createPost returns NetworkError when network unavailable`() = runTest {
        // Given
        fakeApiClient.simulateNoConnection()

        // When
        val result = repository.createPost(createPostRequest)

        // Then
        assertTrue(result.isLeft)
        val error = result.leftOrNull()
        assertTrue(error is AppError.NetworkError.NoConnection)
        assertEquals("No internet connection available", error?.message)
    }

    @Test
    fun `createPost returns ValidationError when post has no fragrances`() = runTest {
        // Given
        val invalidPost = CreatePostRequest(
            contentFormat = ContentFormat.TEXT,
            fragranceIds = emptyList() // Invalid!
        )

        // When
        val result = repository.createPost(invalidPost)

        // Then
        assertTrue(result.isLeft)
        assertTrue(result.leftOrNull() is AppError.ValidationError)
    }
}
```

### 7. Test Coverage Guidelines

**Required Test Coverage:**
- ✅ All Use Cases: 100%
- ✅ All Repositories: 90%+
- ✅ All ViewModels: 80%+
- ✅ Validation Logic: 100%
- ✅ Error Mapping: 100%
  **Focus on:**
- ✅ Happy path (success scenarios)
- ✅ Error paths (network errors, validation errors)
- ✅ Edge cases (empty lists, null values)
- ✅ Boundary conditions (max/min values)
### 8. Integration Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class FragranceDatabaseTest {

    private lateinit var database: FragranceDatabase
    private lateinit var dao: UserFragranceDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            FragranceDatabase::class.java
        ).build()
        dao = database.userFragranceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveFragrance() = runTest {
        // Given
        val fragrance = UserFragrance(
            id = 0,
            name = "Test Fragrance",
            brand = "Test Brand"
        )

        // When
        dao.insertUserFragrance(fragrance)
        val retrieved = dao.getAllUserFragrances().first()

        // Then
        assertEquals(1, retrieved.size)
        assertEquals("Test Fragrance", retrieved[0].name)
    }
}
```

### 9. MockK for Advanced Mocking (Optional)

```kotlin
// For complex scenarios where fakes aren't sufficient
class ComplexViewModelTest {

    private val mockRepository = mockk<FragranceRepository>()
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        viewModel = SearchViewModel(mockRepository)
    }

    @Test
    fun `search calls repository with correct parameters`() = runTest {
        // Given
        val query = "test query"
        coEvery {
            mockRepository.searchFragrances(query, any(), any())
        } returns emptyList<Fragrance>().asRight()

        // When
        viewModel.search(query)

        // Then
        coVerify { mockRepository.searchFragrances(query, 0, 20) }
    }
}
```

### 10. Test Organization

```
app/src/
├── test/kotlin/                    # Unit tests
│   ├── domain/
│   │   ├── usecase/               # Use case tests
│   │   └── validation/            # Validation tests
│   ├── data/
│   │   └── repository/            # Repository tests
│   ├── ui/
│   │   └── viewmodel/             # ViewModel tests
│   └── fakes/                     # Fake implementations
│       ├── FakeFragranceRepository.kt
│       ├── FakePostRepository.kt
│       └── FakeApiClient.kt
│
└── androidTest/kotlin/             # Instrumented tests
    ├── database/                   # Database tests
    ├── ui/                        # UI tests (Compose)
    └── di/                        # DI integration tests
```

---

## Mandatory Testing Patterns

### ✅ DO
- ✅ Write tests before or alongside implementation (TDD)
- ✅ Test one behavior per test method
- ✅ Use descriptive test names with backticks
- ✅ Follow Arrange-Act-Assert pattern
- ✅ Use fake implementations instead of mocks when possible
- ✅ Test both success and error scenarios
- ✅ Use `runTest` for coroutine tests
- ✅ Make tests deterministic and repeatable
- ✅ Keep tests independent (no shared state)
### ❌ DON'T
- ❌ Don't test Android framework code
- ❌ Don't test third-party libraries
- ❌ Don't use real network calls in unit tests
- ❌ Don't share test data between tests
- ❌ Don't test implementation details
- ❌ Don't write tests that depend on execution order
- ❌ Don't ignore failing tests
---

## Summary

This error handling system provides:
1. **Type Safety**: All errors are compile-time checked
2. **Exhaustive Handling**: Sealed classes ensure all cases are handled
3. **User-Friendly**: Clear error messages and appropriate UI feedback
4. **Debuggable**: Errors include context and causes
5. **Testable**: Easy to test error scenarios
6. **Consistent**: Same pattern throughout the entire app
7. **Recoverable**: Retry mechanisms for appropriate errors
   Follow these patterns religiously for a robust, production-ready error handling system.
