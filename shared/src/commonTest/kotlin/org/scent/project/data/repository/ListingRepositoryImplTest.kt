package org.scent.project.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import org.scent.project.data.remote.dto.BrandListResponseDto
import org.scent.project.data.remote.dto.FragranceNoteDto
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.data.remote.dto.ListingListResponseDto
import org.scent.project.data.remote.dto.ListingResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.ListingQuery
import org.scent.project.domain.model.UpdateListingParams
import org.scent.project.fakes.FakeListingApi
import org.scent.project.fakes.FakeListingDao
import org.scent.project.fakes.FakeTokenStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Covers both ListingRepository's suspend surface (auth-gated mutations, the
 * transitional getMyListings) and its Flow SSOT contract from ADR-0001: Room is
 * the only reader, mutations write back through it, and browse membership stays
 * separate from rows cached by the detail and My Listings paths.
 */
class ListingRepositoryImplTest {
    private fun fragrance(id: Int = 1) =
        FragranceResponse(
            id = id,
            name = "Aventus",
            brand = "Creed",
            notes = listOf(FragranceNoteDto(note = "Pineapple", noteType = "TOP")),
        )

    private fun listing(
        id: Int,
        sellerId: Int = 7,
        price: Double = 100.0,
        createdAt: Long = 1_000L,
        fragranceId: Int = 1,
    ) = ListingResponse(
        id = id,
        fragrance = fragrance(fragranceId),
        sellerId = sellerId,
        price = price,
        condition = "NEW",
        createdAt = createdAt,
    )

    private val validListingResponse =
        ListingResponse(
            id = 1,
            fragrance = FragranceResponse(id = 10, name = "Sauvage", brand = "Dior"),
            sellerId = 5,
            price = 80.0,
            condition = "NEW",
        )

    // Mutations and My Listings are token-gated, so the default storage is
    // authenticated — an unauthenticated case is set up explicitly where tested.
    private fun repo(
        api: FakeListingApi = FakeListingApi(),
        dao: FakeListingDao = FakeListingDao(),
        storage: FakeTokenStorage = FakeTokenStorage().apply { storedToken = "token" },
    ) = ListingRepositoryImpl(api = api, tokenStorage = storage, listingDao = dao)

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
            val api = FakeListingApi().apply { createResponse = validListingResponse }

            val result =
                repo(api = api).createListing(
                    CreateListingParams(
                        fragranceId = 10,
                        price = 80.0,
                        condition = "NEW",
                    ),
                )

            assertTrue(result.isRight)
            assertEquals("Sauvage", result.getOrNull()?.fragrance?.name)
        }

    @Test
    fun `createListing caches the result so it appears in My Listings`() =
        runTest {
            val dao = FakeListingDao()
            val api = FakeListingApi().apply { createResponse = validListingResponse }

            repo(api = api, dao = dao).createListing(
                CreateListingParams(fragranceId = 10, price = 80.0, condition = "NEW"),
            )

            repo(dao = dao).getUserListingsFlow(sellerId = 5).test {
                assertEquals(listOf(1), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `createListing returns NoConnection on IOException`() =
        runTest {
            val api = FakeListingApi().apply { createException = IOException("no net") }

            val result =
                repo(api = api).createListing(
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
            assertEquals(1, result.getOrNull()?.id)
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
            val api = FakeListingApi().apply { updateResponse = validListingResponse.copy(isActive = false) }

            val result = repo(api = api).setListingActive(1, active = false)

            assertTrue(result.isRight)
            assertEquals(1, api.lastUpdateListingId)
        }

    // -------------------------------------------------------------------------
    // deleteListing
    // -------------------------------------------------------------------------

    @Test
    fun `deleteListing returns Right on success`() =
        runTest {
            val api = FakeListingApi()

            val result = repo(api = api).deleteListing(1)

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
    // getListingsFlow / refreshListings / loadMoreListings — Flow SSOT (ADR-0001)
    // -------------------------------------------------------------------------

    @Test
    fun `refreshListings writes to Room and the open Flow re-emits`() =
        runTest {
            val api = FakeListingApi()
            val repository = repo(api)

            repository.getListingsFlow().test {
                assertEquals(emptyList(), awaitItem().getOrNull())

                api.listingsResponse = ListingListResponseDto(listings = listOf(listing(1), listing(2)))
                repository.refreshListings()

                assertEquals(listOf(1, 2), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getBrowseTotalCountFlow reflects the server's totalCount after refreshListings`() =
        runTest {
            val api = FakeListingApi()
            val repository = repo(api)

            repository.getBrowseTotalCountFlow().test {
                assertEquals(null, awaitItem().getOrNull())

                api.listingsResponse =
                    ListingListResponseDto(listings = listOf(listing(1)), totalCount = 312)
                repository.refreshListings()

                assertEquals(312, awaitItem().getOrNull())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `getBrowseTotalCountFlow updates again after loadMoreListings`() =
        runTest {
            val api = FakeListingApi()
            val repository = repo(api)

            api.listingsResponse =
                ListingListResponseDto(listings = listOf(listing(1)), nextCursor = "c1", totalCount = 312)
            repository.refreshListings()

            repository.getBrowseTotalCountFlow().test {
                assertEquals(312, awaitItem().getOrNull())

                api.listingsResponse =
                    ListingListResponseDto(listings = listOf(listing(2)), totalCount = 400)
                repository.loadMoreListings()

                assertEquals(400, awaitItem().getOrNull())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the fragrance and its notes survive the round trip through Room`() =
        runTest {
            val api = FakeListingApi()
            val repository = repo(api)

            api.listingsResponse = ListingListResponseDto(listings = listOf(listing(1)))
            repository.refreshListings()

            repository.getListingsFlow().test {
                val fragrance =
                    awaitItem()
                        .getOrNull()
                        .orEmpty()
                        .single()
                        .fragrance
                assertEquals("Aventus", fragrance.name)
                assertEquals("Creed", fragrance.brand)
                assertEquals(listOf("Pineapple"), fragrance.notes.map { it.note })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refreshListings drops listings that left the results`() =
        runTest {
            val api = FakeListingApi()
            val repository = repo(api)

            api.listingsResponse = ListingListResponseDto(listings = listOf(listing(1), listing(2)))
            repository.refreshListings()

            api.listingsResponse = ListingListResponseDto(listings = listOf(listing(1)))
            repository.refreshListings()

            repository.getListingsFlow().test {
                assertEquals(listOf(1), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `loadMoreListings appends after the existing results`() =
        runTest {
            val api = FakeListingApi()
            val repository = repo(api)

            api.listingsResponse = ListingListResponseDto(listings = listOf(listing(1)), nextCursor = "c1")
            repository.refreshListings()

            api.listingsResponse = ListingListResponseDto(listings = listOf(listing(2)))
            repository.loadMoreListings()

            repository.getListingsFlow().test {
                assertEquals(listOf(1, 2), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refreshListings under a new filter does not leak the previous filter's cursor`() =
        runTest {
            val api = FakeListingApi()
            val repository = repo(api)

            api.listingsResponse =
                ListingListResponseDto(listings = listOf(listing(1)), nextCursor = "cursor-for-brand-a")
            repository.refreshListings(ListingQuery(brand = "A"))

            // Switching filters must reset pagination, not continue brand A's cursor.
            api.listingsResponse = ListingListResponseDto(listings = listOf(listing(2)), nextCursor = null)
            repository.refreshListings(ListingQuery(brand = "B"))

            repository.getListingsFlow().test {
                assertEquals(listOf(2), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }

            // loadMore now continues brand B, the last-refreshed query — exhausted, so a no-op.
            api.listingsResponse = null
            assertTrue(repository.loadMoreListings().isRight)
        }

    @Test
    fun `a My Listings fetch does not push rows into the browse list`() =
        runTest {
            val api = FakeListingApi()
            val dao = FakeListingDao()
            val repository = repo(api, dao)

            api.listingsResponse = ListingListResponseDto(listings = listOf(listing(1)))
            repository.refreshListings()

            // A seller's own listings are cached, but browse must not grow.
            api.myListingsResponse = ListingListResponseDto(listings = listOf(listing(99, sellerId = 7)))
            repository.refreshMyListings()

            repository.getListingsFlow().test {
                assertEquals(listOf(1), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
            repository.getUserListingsFlow(sellerId = 7).test {
                assertEquals(
                    listOf(1, 99),
                    awaitItem()
                        .getOrNull()
                        .orEmpty()
                        .map { it.id }
                        .sorted(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `updating a listing re-emits to an open detail collector`() =
        runTest {
            val api = FakeListingApi()
            val repository = repo(api)

            api.listingsResponse = ListingListResponseDto(listings = listOf(listing(1, price = 100.0)))
            repository.refreshListings()

            repository.getListingDetailFlow(1).test {
                assertEquals(100.0, awaitItem().getOrNull()?.price)

                api.updateResponse = listing(1, price = 80.0)
                repository.setListingActive(1, active = true)

                assertEquals(80.0, awaitItem().getOrNull()?.price)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `deleting a listing removes it from an open collector`() =
        runTest {
            val api = FakeListingApi()
            val repository = repo(api)

            api.listingsResponse = ListingListResponseDto(listings = listOf(listing(1), listing(2)))
            repository.refreshListings()

            repository.getListingsFlow().test {
                assertEquals(listOf(1, 2), awaitItem().getOrNull().orEmpty().map { it.id })

                repository.deleteListing(1)

                assertEquals(listOf(2), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a detail read for an uncached listing reports not found rather than empty`() =
        runTest {
            repo().getListingDetailFlow(404).test {
                assertIs<AppError.NetworkError.NotFound>(awaitItem().leftOrNull())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a network failure leaves the cached listings intact`() =
        runTest {
            val api = FakeListingApi()
            val repository = repo(api)

            api.listingsResponse = ListingListResponseDto(listings = listOf(listing(1)))
            repository.refreshListings()

            api.listingsException = IOException("offline")
            val result = repository.refreshListings()

            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
            repository.getListingsFlow().test {
                assertEquals(listOf(1), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a listing whose fragrance failed to map is dropped rather than shown blank`() =
        runTest {
            val api = FakeListingApi()
            val repository = repo(api)

            api.listingsResponse =
                ListingListResponseDto(
                    listings =
                        listOf(
                            listing(1),
                            ListingResponse(id = 2, fragrance = null, price = 5.0, condition = "NEW"),
                        ),
                )
            repository.refreshListings()

            repository.getListingsFlow().test {
                assertEquals(listOf(1), awaitItem().getOrNull().orEmpty().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `re-caching a fragrance with fewer notes drops the stale ones`() =
        runTest {
            val api = FakeListingApi()
            val dao = FakeListingDao()
            val repository = repo(api, dao)

            api.listingsResponse = ListingListResponseDto(listings = listOf(listing(1)))
            repository.refreshListings()
            assertEquals(1, dao.currentNotes.size)

            api.listingsResponse =
                ListingListResponseDto(
                    listings = listOf(listing(1).copy(fragrance = fragrance().copy(notes = emptyList()))),
                )
            repository.refreshListings()

            assertTrue(dao.currentNotes.isEmpty())
        }

    @Test
    fun `a failing cache read surfaces as Left rather than crashing the collector`() =
        runTest {
            val dao = FakeListingDao().apply { readException = IllegalStateException("db corrupt") }

            repo(dao = dao).getListingsFlow().test {
                assertIs<AppError.Unknown>(awaitItem().leftOrNull())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
