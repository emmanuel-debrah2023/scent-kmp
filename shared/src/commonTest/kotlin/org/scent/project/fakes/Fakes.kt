package org.scent.project.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.scent.project.data.local.TokenStorage
import org.scent.project.data.remote.api.AuthApi
import org.scent.project.data.remote.api.FragranceApi
import org.scent.project.data.remote.api.ListingApi
import org.scent.project.data.remote.api.PostApi
import org.scent.project.data.remote.dto.AuthResponse
import org.scent.project.data.remote.dto.BrandListResponseDto
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
import org.scent.project.data.remote.dto.UpdateListingRequestDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.CreatePostParams
import org.scent.project.domain.model.FeedPage
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.model.ListingPage
import org.scent.project.domain.model.ListingQuery
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.UpdateListingParams
import org.scent.project.domain.repository.AuthRepository
import org.scent.project.domain.repository.FragranceRepository
import org.scent.project.domain.repository.ListingRepository
import org.scent.project.domain.repository.MediaRepository
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.repository.UploadTarget
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
    private val priceResult: Result<Double>? = null,
    private val fillResult: Result<Int>? = null,
) : ValidatorContract {
    override fun validateEmail(email: String): Result<String> = emailResult ?: email.asRight()

    override fun validatePassword(password: String): Result<String> = passwordResult ?: password.asRight()

    override fun validateUsername(username: String): Result<String> = usernameResult ?: username.asRight()

    override fun validateDisplayName(displayName: String): Result<String> = displayNameResult ?: displayName.asRight()

    override fun validatePriceRange(
        minRaw: String,
        maxRaw: String,
    ): Result<ClosedRange<Double>> = priceRangeResult ?: Validator.validatePriceRange(minRaw, maxRaw)

    override fun validatePrice(raw: String): Result<Double> = priceResult ?: Validator.validatePrice(raw)

    override fun validateFill(
        kind: ListingKind,
        nominalSizeMl: Int?,
        remainingMl: Int?,
    ): Result<Int> = fillResult ?: Validator.validateFill(kind, nominalSizeMl, remainingMl)
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

    /** Drive the SSOT read by emitting into this from a test. */
    val feedFlow = MutableStateFlow<Result<List<Post>>>(emptyList<Post>().asRight())
    var refreshFeedResult: Result<Unit> = Unit.asRight()
    var loadMoreFeedResult: Result<Unit> = Unit.asRight()
    var refreshFeedCallCount: Int = 0
    var loadMoreFeedCallCount: Int = 0

    override fun getFeedFlow(): Flow<Result<List<Post>>> = feedFlow

    override suspend fun refreshFeed(limit: Int): Result<Unit> {
        refreshFeedCallCount++
        lastFeedLimit = limit
        return refreshFeedResult
    }

    override suspend fun loadMoreFeed(limit: Int): Result<Unit> {
        loadMoreFeedCallCount++
        lastFeedLimit = limit
        return loadMoreFeedResult
    }

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
    /** Drive the SSOT reads by emitting into these from a test. */
    val listingsFlow = MutableStateFlow<Result<List<Listing>>>(emptyList<Listing>().asRight())
    val userListingsFlow = MutableStateFlow<Result<List<Listing>>>(emptyList<Listing>().asRight())
    val listingDetailFlow = MutableStateFlow<Result<Listing>>(AppError.Unknown().asLeft())

    var refreshListingsResult: Result<Unit> = Unit.asRight()
    var loadMoreListingsResult: Result<Unit> = Unit.asRight()
    var refreshListingResult: Result<Unit> = Unit.asRight()
    var refreshMyListingsResult: Result<Unit> = Unit.asRight()

    var refreshListingsCallCount: Int = 0
    var loadMoreListingsCallCount: Int = 0
    var refreshMyListingsCallCount: Int = 0
    var lastQuery: ListingQuery? = null
    var lastRefreshedListingId: Int? = null

    override fun getListingsFlow(): Flow<Result<List<Listing>>> = listingsFlow

    override fun getListingDetailFlow(id: Int): Flow<Result<Listing>> = listingDetailFlow

    override fun getUserListingsFlow(sellerId: Int): Flow<Result<List<Listing>>> = userListingsFlow

    override suspend fun refreshListings(
        query: ListingQuery,
        limit: Int,
    ): Result<Unit> {
        refreshListingsCallCount++
        lastQuery = query
        return refreshListingsResult
    }

    override suspend fun loadMoreListings(limit: Int): Result<Unit> {
        loadMoreListingsCallCount++
        return loadMoreListingsResult
    }

    override suspend fun refreshListing(id: Int): Result<Unit> {
        lastRefreshedListingId = id
        return refreshListingResult
    }

    override suspend fun refreshMyListings(): Result<Unit> {
        refreshMyListingsCallCount++
        return refreshMyListingsResult
    }

    var getListingsResult: Result<ListingPage> = AppError.Unknown().asLeft()
    var brandSuggestionsResult: Result<List<String>> = emptyList<String>().asRight()
    var createListingResult: Result<Listing> = AppError.Unknown().asLeft()

    var lastListingsCursor: String? = null
    var lastListingsLimit: Int? = null
    var lastListingsBrand: String? = null
    var lastListingsCondition: String? = null
    var lastListingsVolume: Int? = null
    var lastListingsMinPrice: Double? = null
    var lastListingsMaxPrice: Double? = null
    var lastBrandQuery: String? = null
    var lastBrandLimit: Int? = null
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

    override suspend fun getBrandSuggestions(
        query: String,
        limit: Int,
    ): Result<List<String>> {
        lastBrandQuery = query
        lastBrandLimit = limit
        return brandSuggestionsResult
    }

    override suspend fun createListing(params: CreateListingParams): Result<Listing> {
        lastCreateParams = params
        return createListingResult
    }

    var getListingResult: Result<Listing> = AppError.Unknown().asLeft()
    var updateListingResult: Result<Listing> = AppError.Unknown().asLeft()
    var setActiveResult: Result<Listing> = AppError.Unknown().asLeft()
    var deleteListingResult: Result<Unit> = AppError.Unknown().asLeft()
    var myListingsResult: Result<List<Listing>> = emptyList<Listing>().asRight()

    var lastGetListingId: Int? = null
    var lastUpdateListingId: Int? = null
    var lastUpdateParams: UpdateListingParams? = null
    var lastSetActiveId: Int? = null
    var lastSetActiveValue: Boolean? = null
    var lastDeleteListingId: Int? = null

    override suspend fun getListing(id: Int): Result<Listing> {
        lastGetListingId = id
        return getListingResult
    }

    override suspend fun updateListing(
        id: Int,
        params: UpdateListingParams,
    ): Result<Listing> {
        lastUpdateListingId = id
        lastUpdateParams = params
        return updateListingResult
    }

    override suspend fun setListingActive(
        id: Int,
        active: Boolean,
    ): Result<Listing> {
        lastSetActiveId = id
        lastSetActiveValue = active
        return setActiveResult
    }

    override suspend fun deleteListing(id: Int): Result<Unit> {
        lastDeleteListingId = id
        return deleteListingResult
    }

    override suspend fun getMyListings(): Result<List<Listing>> = myListingsResult
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

    var brandsResponse: BrandListResponseDto? = null
    var brandsException: Exception? = null

    var createResponse: ListingResponse? = null
    var createException: Exception? = null

    var lastListingsBrand: String? = null
    var lastListingsCondition: String? = null
    var lastListingsVolume: Int? = null
    var lastListingsMinPrice: Double? = null
    var lastListingsMaxPrice: Double? = null
    var lastBrandQuery: String? = null
    var lastBrandLimit: Int? = null

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

    override suspend fun getBrandSuggestions(
        query: String,
        limit: Int,
    ): BrandListResponseDto {
        lastBrandQuery = query
        lastBrandLimit = limit
        brandsException?.let { throw it }
        return brandsResponse ?: error("FakeListingApi.brandsResponse not set")
    }

    override suspend fun createListing(
        request: CreateListingRequest,
        token: String,
    ): ListingResponse {
        createException?.let { throw it }
        return createResponse ?: error("FakeListingApi.createResponse not set")
    }

    var getListingResponse: ListingResponse? = null
    var getListingException: Exception? = null
    var updateResponse: ListingResponse? = null
    var updateException: Exception? = null
    var deleteException: Exception? = null
    var myListingsResponse: ListingListResponseDto? = null
    var myListingsException: Exception? = null

    var lastUpdateListingId: Int? = null
    var lastDeleteListingId: Int? = null

    override suspend fun getListing(id: Int): ListingResponse {
        getListingException?.let { throw it }
        return getListingResponse ?: error("FakeListingApi.getListingResponse not set")
    }

    override suspend fun updateListing(
        id: Int,
        request: UpdateListingRequestDto,
        token: String,
    ): ListingResponse {
        lastUpdateListingId = id
        updateException?.let { throw it }
        return updateResponse ?: error("FakeListingApi.updateResponse not set")
    }

    override suspend fun deleteListing(
        id: Int,
        token: String,
    ) {
        lastDeleteListingId = id
        deleteException?.let { throw it }
    }

    override suspend fun getMyListings(token: String): ListingListResponseDto {
        myListingsException?.let { throw it }
        return myListingsResponse ?: error("FakeListingApi.myListingsResponse not set")
    }
}

// -------------------------------------------------------------------------
// FakeMediaRepository
// -------------------------------------------------------------------------

class FakeMediaRepository : MediaRepository {
    var getImageUploadUrlResult: Result<UploadTarget> = AppError.Unknown().asLeft()
    var uploadBytesResult: Result<Unit> = AppError.Unknown().asLeft()
    var completeUploadResult: Result<Int> = AppError.Unknown().asLeft()

    var lastContentType: String? = null
    var lastUploadedBytes: ByteArray? = null
    var lastCompletedUid: String? = null
    var lastProgress: Pair<Long, Long>? = null

    override suspend fun getImageUploadUrl(contentType: String): Result<UploadTarget> {
        lastContentType = contentType
        return getImageUploadUrlResult
    }

    override suspend fun uploadBytes(
        target: UploadTarget,
        bytes: ByteArray,
        contentType: String,
        onProgress: (bytesSent: Long, totalBytes: Long) -> Unit,
    ): Result<Unit> {
        lastUploadedBytes = bytes
        if (uploadBytesResult.isRight) {
            onProgress(bytes.size.toLong(), bytes.size.toLong())
            lastProgress = bytes.size.toLong() to bytes.size.toLong()
        }
        return uploadBytesResult
    }

    override suspend fun completeUpload(uid: String): Result<Int> {
        lastCompletedUid = uid
        return completeUploadResult
    }
}
