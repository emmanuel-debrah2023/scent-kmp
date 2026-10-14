package org.scent.project

import data.schema.FragranceCondition
import data.schema.FragranceNotesTable
import data.schema.ListingsTable
import data.schema.NoteType
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.util.Result
import preferences.ListingCandidate
import preferences.MAX_CANDIDATES
import preferences.NotesOverlapCandidateSource
import preferences.ScentFamily
import preferences.ScentPreferences
import preferences.SizePreference
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ListingCandidateSourceTest {
    private val source = NotesOverlapCandidateSource()
    private var sellerId = 0

    @BeforeTest
    fun setUp() {
        initListingTestDatabase()
        sellerId = seedUser("seller")
    }

    /** Seeds one listing for a fresh fragrance carrying [notes]; returns the listing id. */
    private fun listing(
        price: Double,
        kind: ListingKind = ListingKind.SEALED,
        notes: List<String> = emptyList(),
    ): Int {
        val fragranceId = seedFragrance(sellerId)
        transaction {
            notes.forEach { stored ->
                FragranceNotesTable.insert {
                    it[FragranceNotesTable.fragranceId] = fragranceId
                    it[note] = stored
                    it[noteType] = NoteType.TOP
                }
            }
        }
        return seedListing(sellerId, fragranceId, FragranceCondition.NEW, price, kind.name)
    }

    private fun find(preferences: ScentPreferences): List<ListingCandidate> =
        runBlocking { source.find(preferences) }.let { assertNotNullRight(it) }

    private fun assertNotNullRight(result: Result<List<ListingCandidate>>): List<ListingCandidate> =
        requireNotNull(result.getOrNull()) { "expected Right but got ${result.leftOrNull()}" }

    private fun List<ListingCandidate>.ids() = map { it.listingId }

    @Test
    fun `a budget cap excludes listings over maxPrice and keeps one at exactly the cap`() {
        val cheap = listing(price = 60.0)
        val atCap = listing(price = 80.0)
        listing(price = 80.01)

        val found = find(ScentPreferences(maxPrice = 80.0))

        assertEquals(listOf(cheap, atCap), found.ids())
    }

    @Test
    fun `decant preference returns only decant listings`() {
        listing(price = 20.0, kind = ListingKind.SEALED)
        val decant = listing(price = 15.0, kind = ListingKind.DECANT)

        val found = find(ScentPreferences(size = SizePreference.DECANT))

        assertEquals(listOf(decant), found.ids())
    }

    @Test
    fun `full preference excludes decants and keeps every other kind`() {
        val sealed = listing(price = 10.0, kind = ListingKind.SEALED)
        val opened = listing(price = 20.0, kind = ListingKind.OPENED)
        val tester = listing(price = 30.0, kind = ListingKind.TESTER)
        listing(price = 5.0, kind = ListingKind.DECANT)

        val found = find(ScentPreferences(size = SizePreference.FULL))

        assertEquals(listOf(sealed, opened, tester), found.ids())
    }

    @Test
    fun `no size preference returns every kind`() {
        val sealed = listing(price = 10.0, kind = ListingKind.SEALED)
        val decant = listing(price = 5.0, kind = ListingKind.DECANT)

        assertEquals(listOf(decant, sealed), find(ScentPreferences()).ids())
    }

    @Test
    fun `higher family overlap ranks first, then lower price`() {
        val twoNotesDear = listing(100.0, notes = listOf("bergamot", "lemon"))
        val oneNoteCheap = listing(50.0, notes = listOf("mint", "oud"))
        val twoNotesCheaper = listing(80.0, notes = listOf("grapefruit", "mint"))

        val found = find(ScentPreferences(family = ScentFamily.FRESH))

        assertEquals(listOf(twoNotesCheaper, twoNotesDear, oneNoteCheap), found.ids())
        assertEquals(listOf(2, 2, 1), found.map { it.familyOverlap })
    }

    @Test
    fun `with no family candidates are ordered by price alone`() {
        val dear = listing(90.0, notes = listOf("bergamot"))
        val cheap = listing(30.0, notes = listOf("oud"))

        val found = find(ScentPreferences())

        assertEquals(listOf(cheap, dear), found.ids())
        assertTrue(found.all { it.familyOverlap == 0 })
    }

    @Test
    fun `stored note case does not stop a note matching the family`() {
        listing(40.0, notes = listOf("Bergamot", "LEMON"))

        val found = find(ScentPreferences(family = ScentFamily.FRESH))

        assertEquals(2, found.single().familyOverlap)
    }

    @Test
    fun `a candidate carries every note on its fragrance, not only the matching ones`() {
        listing(40.0, notes = listOf("bergamot", "oud"))

        val found = find(ScentPreferences(family = ScentFamily.FRESH))

        assertEquals(setOf("bergamot", "oud"), found.single().notes.toSet())
    }

    @Test
    fun `inactive and soft-deleted listings are never candidates`() {
        val live = listing(price = 10.0)
        val inactive = listing(price = 11.0)
        val deleted = listing(price = 12.0)
        transaction {
            ListingsTable.update({ ListingsTable.id eq inactive }) { it[isActive] = false }
            ListingsTable.update({ ListingsTable.id eq deleted }) {
                it[deletedAt] = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }

        assertEquals(listOf(live), find(ScentPreferences()).ids())
    }

    @Test
    fun `no matching listings is an empty list, not an error`() {
        listing(price = 500.0)

        val result = runBlocking { source.find(ScentPreferences(maxPrice = 10.0)) }

        assertEquals(emptyList(), result.getOrNull())
    }

    @Test
    fun `results are capped at the candidate limit`() {
        repeat(MAX_CANDIDATES + 5) { listing(price = 10.0 + it) }

        assertEquals(MAX_CANDIDATES, find(ScentPreferences()).size)
    }

    @Test
    fun `a database failure is a storage read error, not a thrown exception`() {
        listing(price = 10.0)
        transaction { SchemaUtils.drop(FragranceNotesTable) }

        val result = runBlocking { source.find(ScentPreferences()) }

        assertIs<AppError.StorageError.ReadFailed>(result.leftOrNull())
    }
}
