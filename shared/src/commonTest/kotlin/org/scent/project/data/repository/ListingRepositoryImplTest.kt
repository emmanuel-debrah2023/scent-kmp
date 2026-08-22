package org.scent.project.data.repository

import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.scent.project.data.remote.dto.BrandListResponseDto
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.data.remote.dto.ListingListResponseDto
import org.scent.project.data.remote.dto.ListingResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.UpdateListingParams
import org.scent.project.fakes.FakeListingApi
import org.scent.project.fakes.FakeTokenStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ListingRepositoryImplTest {
    private fun repo(
        api: FakeListingApi = FakeListingApi(),
        storage: FakeTokenStorage = FakeTokenStorage(),
    ) = ListingRepositoryImpl(api = api, tokenStorage = storage)

    private val validListingResponse =
        ListingResponse(
            id = 1,
            fragrance = FragranceResponse(id = 10, name = "Sauvage", brand = "Dior"),
            sellerId = 5,
            price = 80.0,
            condition = "NEW",
        )

    // -------------------------------------------------------------------------
    // getListings
    // -------------------------------------------------------------------------

    @Test
    fun `getListings returns Right on success`() =
        runTest {
            val api =
                FakeListingApi().apply {
                    listingsResponse =
                        ListingListResponseDto(listings = listOf(validListingResponse))
                }

            val result = repo(api).getListings()

            assertTrue(result.isRight)
            assertEquals(1, requireNotNull(result.getOrNull()).listings.size)
        }

    @Test
    fun `getListings surfaces nextCursor from the response`() =
        runTest {
            val api =
                FakeListingApi().apply {
                    listingsResponse =
                        ListingListResponseDto(
                            listings = listOf(validListingResponse),
                            nextCursor = "cursor-2",
                        )
                }

            val result = repo(api).getListings()

            assertEquals("cursor-2", requireNotNull(result.getOrNull()).nextCursor)
        }

    @Test
    fun `getListings surfaces totalCount from the response`() =
        runTest {
            val api =
                FakeListingApi().apply {
                    listingsResponse =
                        ListingListResponseDto(
                            listings = listOf(validListingResponse),
                            totalCount = 312,
                        )
                }

            val result = repo(api).getListings()

            assertEquals(312, requireNotNull(result.getOrNull()).totalCount)
        }

    @Test
    fun `getListings returns NoConnection on IOException`() =
        runTest {
            val api = FakeListingApi().apply { listingsException = IOException("offline") }

            val result = repo(api).getListings()

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }

    @Test
    fun `getListings forwards brand condition and volume to the api`() =
        runTest {
            val api =
                FakeListingApi().apply {
                    listingsResponse = ListingListResponseDto(listings = listOf(validListingResponse))
                }

            repo(api).getListings(brand = "Dior", condition = "NEW", volume = 50)

            assertEquals("Dior", api.lastListingsBrand)
            assertEquals("NEW", api.lastListingsCondition)
            assertEquals(50, api.lastListingsVolume)
        }

    @Test
    fun `getListings forwards minPrice and maxPrice to the api`() =
        runTest {
            val api =
                FakeListingApi().apply {
                    listingsResponse = ListingListResponseDto(listings = listOf(validListingResponse))
                }

            repo(api).getListings(minPrice = 50.0, maxPrice = 200.0)

            assertEquals(50.0, api.lastListingsMinPrice)
            assertEquals(200.0, api.lastListingsMaxPrice)
        }

    @Test
    fun `getListings returns ParseError on SerializationException`() =
        runTest {
            val api =
                FakeListingApi().apply { listingsException = SerializationException("bad") }

            val result = repo(api).getListings()

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.ParseError>(result.leftOrNull())
        }

    // -------------------------------------------------------------------------
    // getBrandSuggestions
    // -------------------------------------------------------------------------

    @Test
    fun `getBrandSuggestions forwards query and limit to the api`() =
        runTest {
            val api =
                FakeListingApi().apply { brandsResponse = BrandListResponseDto(brands = listOf("Dior")) }

            repo(api).getBrandSuggestions("dio", limit = 5)

            assertEquals("dio", api.lastBrandQuery)
            assertEquals(5, api.lastBrandLimit)
        }

    @Test
    fun `getBrandSuggestions maps a null brands list to an empty list`() =
        runTest {
            val api =
                FakeListingApi().apply { brandsResponse = BrandListResponseDto(brands = null) }

            val result = repo(api).getBrandSuggestions("dio")

            assertTrue(result.isRight)
            assertEquals(emptyList(), result.getOrNull())
        }

    @Test
    fun `getBrandSuggestions drops blank brand entries`() =
        runTest {
            val api =
                FakeListingApi().apply {
                    brandsResponse = BrandListResponseDto(brands = listOf("Dior", "", " "))
                }

            val result = repo(api).getBrandSuggestions("dio")

            assertTrue(result.isRight)
            assertEquals(listOf("Dior"), result.getOrNull())
        }

    @Test
    fun `getBrandSuggestions returns NoConnection on IOException`() =
        runTest {
            val api = FakeListingApi().apply { brandsException = IOException("offline") }

            val result = repo(api).getBrandSuggestions("dio")

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }

    // -------------------------------------------------------------------------
    // createListing
    // -------------------------------------------------------------------------

    @Test
    fun `createListing returns Unauthorized when no token`() =
        runTest {
            val storage = FakeTokenStorage() // no token

            val result =
                repo(storage = storage).createListing(
                    CreateListingParams(
                        fragranceId = 10,
                        price = 50.0,
                        condition = "NEW",
                    ),
                )

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }

    @Test
    fun `createListing returns Right on success`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "tok" }
            val api = FakeListingApi().apply { createResponse = validListingResponse }

            val result =
                repo(api = api, storage = storage).createListing(
                    CreateListingParams(
                        fragranceId = 10,
                        price = 80.0,
                        condition = "NEW",
                    ),
                )

            assertTrue(result.isRight)
            assertEquals("Sauvage", result.getOrNull()!!.fragrance.name)
        }

    @Test
    fun `createListing returns NoConnection on IOException`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "tok" }
            val api = FakeListingApi().apply { createException = IOException("no net") }

            val result =
                repo(api = api, storage = storage).createListing(
                    CreateListingParams(
                        fragranceId = 10,
                        price = 50.0,
                        condition = "NEW",
                    ),
                )

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }

    // -------------------------------------------------------------------------
    // getListing
    // -------------------------------------------------------------------------

    @Test
    fun `getListing returns Right on success`() =
        runTest {
            val api = FakeListingApi().apply { getListingResponse = validListingResponse }

            val result = repo(api).getListing(1)

            assertTrue(result.isRight)
            assertEquals(1, result.getOrNull()!!.id)
        }

    // -------------------------------------------------------------------------
    // updateListing / setListingActive
    // -------------------------------------------------------------------------

    @Test
    fun `updateListing returns Unauthorized when no token`() =
        runTest {
            val storage = FakeTokenStorage()

            val result = repo(storage = storage).updateListing(1, UpdateListingParams(price = 50.0))

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }

    @Test
    fun `setListingActive delegates to updateListing with isActive set`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "tok" }
            val api = FakeListingApi().apply { updateResponse = validListingResponse.copy(isActive = false) }

            val result = repo(api = api, storage = storage).setListingActive(1, active = false)

            assertTrue(result.isRight)
            assertEquals(1, api.lastUpdateListingId)
        }

    // -------------------------------------------------------------------------
    // deleteListing
    // -------------------------------------------------------------------------

    @Test
    fun `deleteListing returns Right on success`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "tok" }
            val api = FakeListingApi()

            val result = repo(api = api, storage = storage).deleteListing(1)

            assertTrue(result.isRight)
            assertEquals(1, api.lastDeleteListingId)
        }

    @Test
    fun `deleteListing returns Unauthorized when no token`() =
        runTest {
            val storage = FakeTokenStorage()

            val result = repo(storage = storage).deleteListing(1)

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }

    // -------------------------------------------------------------------------
    // getMyListings
    // -------------------------------------------------------------------------

    @Test
    fun `getMyListings returns Right with listings on success`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "tok" }
            val api =
                FakeListingApi().apply {
                    myListingsResponse =
                        ListingListResponseDto(listings = listOf(validListingResponse))
                }

            val result = repo(api = api, storage = storage).getMyListings()

            assertTrue(result.isRight)
            assertEquals(1, result.getOrNull()!!.size)
        }

    @Test
    fun `getMyListings returns Unauthorized when no token`() =
        runTest {
            val storage = FakeTokenStorage()

            val result = repo(storage = storage).getMyListings()

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }
}
