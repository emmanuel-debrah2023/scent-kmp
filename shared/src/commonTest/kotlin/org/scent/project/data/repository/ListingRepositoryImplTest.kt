package org.scent.project.data.repository

import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.data.remote.dto.ListingListResponseDto
import org.scent.project.data.remote.dto.ListingResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreateListingParams
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
    fun `getListings returns NoConnection on IOException`() =
        runTest {
            val api = FakeListingApi().apply { listingsException = IOException("offline") }

            val result = repo(api).getListings()

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
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
    // createListing
    // -------------------------------------------------------------------------

    @Test
    fun `createListing returns Unauthorized when no token`() =
        runTest {
            val storage = FakeTokenStorage() // no token

            val result =
                repo(storage = storage).createListing(
                    CreateListingParams(
                        name = "Test",
                        brand = "Brand",
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
                        name = "Sauvage",
                        brand = "Dior",
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
                        name = "Test",
                        brand = "Brand",
                        price = 50.0,
                        condition = "NEW",
                    ),
                )

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }
}
