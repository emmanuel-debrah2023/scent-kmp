package org.scent.project.data.repository

import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import org.scent.project.data.remote.dto.FragranceListResponseDto
import org.scent.project.data.remote.dto.FragranceResponse
import org.scent.project.domain.error.AppError
import org.scent.project.fakes.FakeFragranceApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FragranceRepositoryImplTest {
    private fun repo(api: FakeFragranceApi = FakeFragranceApi()) = FragranceRepositoryImpl(api = api)

    // -------------------------------------------------------------------------
    // searchFragrances
    // -------------------------------------------------------------------------

    @Test
    fun `searchFragrances returns Right on success`() =
        runTest {
            val api =
                FakeFragranceApi().apply {
                    searchResponse =
                        FragranceListResponseDto(
                            fragrances =
                                listOf(
                                    FragranceResponse(id = 1, name = "Sauvage", brand = "Dior"),
                                ),
                        )
                }

            val result = repo(api).searchFragrances("sauvage")

            assertTrue(result.isRight)
            assertEquals(1, result.getOrNull()!!.size)
        }

    @Test
    fun `searchFragrances returns NoConnection on IOException`() =
        runTest {
            val api = FakeFragranceApi().apply { searchException = IOException("offline") }

            val result = repo(api).searchFragrances("test")

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }

    @Test
    fun `searchFragrances returns ParseError on SerializationException`() =
        runTest {
            val api =
                FakeFragranceApi().apply { searchException = SerializationException("bad data") }

            val result = repo(api).searchFragrances("test")

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.ParseError>(result.leftOrNull())
        }

    // -------------------------------------------------------------------------
    // getFragranceDetail
    // -------------------------------------------------------------------------

    @Test
    fun `getFragranceDetail returns Right on success`() =
        runTest {
            val api =
                FakeFragranceApi().apply {
                    detailResponse = FragranceResponse(id = 1, name = "Sauvage", brand = "Dior")
                }

            val result = repo(api).getFragranceDetail(1)

            assertTrue(result.isRight)
            assertEquals("Sauvage", result.getOrNull()!!.name)
        }

    @Test
    fun `getFragranceDetail returns NoConnection on IOException`() =
        runTest {
            val api = FakeFragranceApi().apply { detailException = IOException("no net") }

            val result = repo(api).getFragranceDetail(1)

            assertTrue(result.isLeft)
            assertIs<AppError.NetworkError.NoConnection>(result.leftOrNull())
        }
}
