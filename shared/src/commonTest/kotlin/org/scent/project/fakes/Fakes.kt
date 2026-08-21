package org.scent.project.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.scent.project.data.local.TokenStorage
import org.scent.project.data.remote.api.AuthApi
import org.scent.project.data.remote.api.FragranceApi
import org.scent.project.data.remote.api.ListingApi
import org.scent.project.data.remote.api.PostApi
import org.scent.project.data.remote.dto.AuthResponse
import org.scent.project.data.remote.dto.CreateListingRequest
import org.scent.project.data.remote.dto.CreatePostRequest
import org.scent.project.data.remote.dto.CreatePostResponseDto
import org.scent.project.data.remote.dto.FeedResponseDto
import org.scent.project.data.remote.dto.FragranceListResponseDto
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.data.remote.dto.LikeResponseDto
import org.scent.project.data.remote.dto.ListingListResponseDto
import org.scent.project.data.remote.dto.ListingResponse
import org.scent.project.data.remote.dto.LoginRequest
import org.scent.project.data.remote.dto.MeResponse
import org.scent.project.data.remote.dto.RegisterRequest
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.CreatePostParams
import org.scent.project.domain.model.FeedPage
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingPage
import org.scent.project.domain.model.Post
import org.scent.project.domain.repository.AuthRepository
import org.scent.project.domain.repository.FragranceRepository
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import org.scent.project.domain.validation.Validator
import org.scent.project.domain.validation.ValidatorContract

// -------------------------------------------------------------------------
// FakeAuthRepository
// -------------------------------------------------------------------------

class FakeAuthRepository : AuthRepository {
    var loginResult: Result<AuthUser> = AppError.Unknown().asLeft()
    var registerResult: Result<AuthUser> = AppError.Unknown().asLeft()
    var getCurrentUserResult: Result<AuthUser> = AppError.Unknown().asLeft()
    var logoutResult: Result<Unit> = Unit.asRight()

    var lastLoginEmail: String? = null
    var lastLoginPassword: String? = null
    var lastRegisterEmail: String? = null
    var lastRegisterPassword: String? = null
    var lastRegisterUsername: String? = null
    var lastRegisterDisplayName: String? = null

    @Suppress("ktlint:standard:backing-property-naming") // exposed via overridden observeAuthState(), not a property
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)

    fun setAuthState(state: AuthState) {
        _authState.value = state
    }

    override fun observeAuthState(): Flow<AuthState> = _authState

    override suspend fun login(
        email: String,
        password: String,
    ): Result<AuthUser> {
        lastLoginEmail = email
        lastLoginPassword = password
        return loginResult
    }

    override suspend fun register(
        email: String,
        password: String,
        username: String,
        displayName: String,
    ): Result<AuthUser> {
        lastRegisterEmail = email
        lastRegisterPassword = password
        lastRegisterUsername = username
        lastRegisterDisplayName = displayName
        return registerResult
    }

    override suspend fun getCurrentUser(): Result<AuthUser> = getCurrentUserResult

    override suspend fun logout(): Result<Unit> = logoutResult
}

// -------------------------------------------------------------------------
// FakeTokenStorage
// -------------------------------------------------------------------------

class FakeTokenStorage : TokenStorage {
    var storedToken: String? = null
    var saveError: AppError.StorageError? = null
    var getError: AppError.StorageError? = null
    var clearError: AppError.StorageError? = null

    override suspend fun saveToken(token: String): Result<Unit> {
        saveError?.let { return it.asLeft() }
        storedToken = token
        return Unit.asRight()
    }

    override suspend fun getToken(): Result<String?> {
        getError?.let { return it.asLeft() }
        return storedToken.asRight()
    }

    override suspend fun clearToken(): Result<Unit> {
        clearError?.let { return it.asLeft() }
        storedToken = null
        return Unit.asRight()
    }
}

// -------------------------------------------------------------------------
// FakeAuthApi
// -------------------------------------------------------------------------

class FakeAuthApi : AuthApi {
    var registerResponse: AuthResponse? = null
    var registerException: Exception? = null

    var loginResponse: AuthResponse? = null
    var loginException: Exception? = null

    var meResponse: MeResponse? = null
    var meException: Exception? = null

    override suspend fun register(request: RegisterRequest): AuthResponse {
        registerException?.let { throw it }
        return registerResponse ?: error("FakeAuthApi.registerResponse not set")
    }

    override suspend fun login(request: LoginRequest): AuthResponse {
        loginException?.let { throw it }
        return loginResponse ?: error("FakeAuthApi.loginResponse not set")
    }

    override suspend fun getCurrentUser(token: String): MeResponse {
        meException?.let { throw it }
        return meResponse ?: error("FakeAuthApi.meResponse not set")
    }
}

// -------------------------------------------------------------------------
// FakeValidator — pass-through by default, configurable per field
// -------------------------------------------------------------------------

class FakeValidator(
    private val emailResult: Result<String>? = null,
    private val passwordResult: Result<String>? = null,
    private val usernameResult: Result<String>? = null,
    private val displayNameResult: Result<String>? = null,
    private val priceRangeResult: Result<ClosedRange<Double>>? = null,
) : ValidatorContract {
    override fun validateEmail(email: String): Result<String> = emailResult ?: email.asRight()

    override fun validatePassword(password: String): Result<String> = passwordResult ?: password.asRight()

    override fun validateUsername(username: String): Result<String> = usernameResult ?: username.asRight()

    override fun validateDisplayName(displayName: String): Result<String> = displayNameResult ?: displayName.asRight()

    override fun validatePriceRange(
        minRaw: String,
        maxRaw: String,
    ): Result<ClosedRange<Double>> = priceRangeResult ?: Validator.validatePriceRange(minRaw, maxRaw)
}

// -------------------------------------------------------------------------
// FakePostRepository
// -------------------------------------------------------------------------

class FakePostRepository : PostRepository {
    var getFeedResult: Result<FeedPage> = AppError.Unknown().asLeft()
    var likePostResult: Result<LikeResult> = AppError.Unknown().asLeft()
    var createPostResult: Result<Post> = AppError.Unknown().asLeft()

    var lastFeedCursor: String? = null
    var lastFeedLimit: Int? = null
    var lastLikePostId: String? = null
    var lastCreatePostParams: CreatePostParams? = null

    override suspend fun getFeed(
        cursor: String?,
        limit: Int,
    ): Result<FeedPage> {
        lastFeedCursor = cursor
        lastFeedLimit = limit
        return getFeedResult
    }

    override suspend fun likePost(postId: String): Result<LikeResult> {
        lastLikePostId = postId
        return likePostResult
    }

    override suspend fun createPost(params: CreatePostParams): Result<Post> {
        lastCreatePostParams = params
        return createPostResult
    }
}

// -------------------------------------------------------------------------
// FakeFragranceRepository
// -------------------------------------------------------------------------

class FakeFragranceRepository : FragranceRepository {
    var searchResult: Result<List<Fragrance>> = AppError.Unknown().asLeft()
    var getDetailResult: Result<Fragrance> = AppError.Unknown().asLeft()

    var lastSearchQuery: String? = null
    var lastSearchCursor: String? = null
    var lastSearchLimit: Int? = null
    var lastDetailId: Int? = null

    override suspend fun searchFragrances(
        query: String,
        cursor: String?,
        limit: Int,
    ): Result<List<Fragrance>> {
        lastSearchQuery = query
        lastSearchCursor = cursor
        lastSearchLimit = limit
        return searchResult
    }

    override suspend fun getFragranceDetail(fragranceId: Int): Result<Fragrance> {
        lastDetailId = fragranceId
        return getDetailResult
    }
}

// -------------------------------------------------------------------------
// FakeListingRepository
// -------------------------------------------------------------------------

class FakeListingRepository : ListingRepository {
    var getListingsResult: Result<ListingPage> = AppError.Unknown().asLeft()
    var createListingResult: Result<Listing> = AppError.Unknown().asLeft()

    var lastListingsCursor: String? = null
    var lastListingsLimit: Int? = null
    var lastListingsBrand: String? = null
    var lastListingsCondition: String? = null
    var lastListingsVolume: Int? = null
    var lastListingsMinPrice: Double? = null
    var lastListingsMaxPrice: Double? = null
    var lastCreateParams: CreateListingParams? = null

    override suspend fun getListings(
        cursor: String?,
        limit: Int,
        brand: String?,
        condition: String?,
        volume: Int?,
        minPrice: Double?,
        maxPrice: Double?,
    ): Result<ListingPage> {
        lastListingsCursor = cursor
        lastListingsLimit = limit
        lastListingsBrand = brand
        lastListingsCondition = condition
        lastListingsVolume = volume
        lastListingsMinPrice = minPrice
        lastListingsMaxPrice = maxPrice
        return getListingsResult
    }

    override suspend fun createListing(params: CreateListingParams): Result<Listing> {
        lastCreateParams = params
        return createListingResult
    }
}

// -------------------------------------------------------------------------
// FakePostApi
// -------------------------------------------------------------------------

class FakePostApi : PostApi {
    var feedResponse: FeedResponseDto? = null
    var feedException: Exception? = null

    var likeResponse: LikeResponseDto? = null
    var likeException: Exception? = null

    var createPostResponse: CreatePostResponseDto? = null
    var createPostException: Exception? = null

    override suspend fun getFeed(
        cursor: String?,
        limit: Int,
        token: String?,
    ): FeedResponseDto {
        feedException?.let { throw it }
        return feedResponse ?: error("FakePostApi.feedResponse not set")
    }

    override suspend fun likePost(
        postId: String,
        token: String,
    ): LikeResponseDto {
        likeException?.let { throw it }
        return likeResponse ?: error("FakePostApi.likeResponse not set")
    }

    override suspend fun createPost(
        request: CreatePostRequest,
        token: String,
    ): CreatePostResponseDto {
        createPostException?.let { throw it }
        return createPostResponse ?: error("FakePostApi.createPostResponse not set")
    }
}

// -------------------------------------------------------------------------
// FakeFragranceApi
// -------------------------------------------------------------------------

class FakeFragranceApi : FragranceApi {
    var searchResponse: FragranceListResponseDto? = null
    var searchException: Exception? = null

    var detailResponse: FragranceResponse? = null
    var detailException: Exception? = null

    override suspend fun searchFragrances(
        query: String,
        cursor: String?,
        limit: Int,
    ): FragranceListResponseDto {
        searchException?.let { throw it }
        return searchResponse ?: error("FakeFragranceApi.searchResponse not set")
    }

    override suspend fun getFragranceDetail(fragranceId: Int): FragranceResponse {
        detailException?.let { throw it }
        return detailResponse ?: error("FakeFragranceApi.detailResponse not set")
    }
}

// -------------------------------------------------------------------------
// FakeListingApi
// -------------------------------------------------------------------------

class FakeListingApi : ListingApi {
    var listingsResponse: ListingListResponseDto? = null
    var listingsException: Exception? = null

    var createResponse: ListingResponse? = null
    var createException: Exception? = null

    var lastListingsBrand: String? = null
    var lastListingsCondition: String? = null
    var lastListingsVolume: Int? = null
    var lastListingsMinPrice: Double? = null
    var lastListingsMaxPrice: Double? = null

    override suspend fun getListings(
        cursor: String?,
        limit: Int,
        brand: String?,
        condition: String?,
        volume: Int?,
        minPrice: Double?,
        maxPrice: Double?,
    ): ListingListResponseDto {
        lastListingsBrand = brand
        lastListingsCondition = condition
        lastListingsVolume = volume
        lastListingsMinPrice = minPrice
        lastListingsMaxPrice = maxPrice
        listingsException?.let { throw it }
        return listingsResponse ?: error("FakeListingApi.listingsResponse not set")
    }

    override suspend fun createListing(
        request: CreateListingRequest,
        token: String,
    ): ListingResponse {
        createException?.let { throw it }
        return createResponse ?: error("FakeListingApi.createResponse not set")
    }
}
