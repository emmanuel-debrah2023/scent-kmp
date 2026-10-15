package org.scent.project

import data.schema.FragranceNotesTable
import data.schema.NoteType
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import preferences.ScentFamily
import preferences.StoredNoteVocabulary
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoteVocabularyTest {
    @BeforeTest
    fun setUp() {
        initListingTestDatabase()
    }

    private fun addNote(
        fragranceId: Int,
        stored: String,
    ) = transaction {
        FragranceNotesTable.insert {
            it[FragranceNotesTable.fragranceId] = fragranceId
            it[note] = stored
            it[noteType] = NoteType.TOP
        }
    }

    @Test
    fun `vocabulary holds every distinct note lowercased, including ones outside any scent family`() {
        val seller = seedUser("seller")
        val one = seedFragrance(seller)
        val two = seedFragrance(seller)
        addNote(one, "Cedar")
        addNote(two, "cedar")
        addNote(two, "Saffron")

        val notes = runBlocking { StoredNoteVocabulary().notes() }.getOrNull()

        assertEquals(setOf("cedar", "saffron"), notes)
        assertTrue(ScentFamily.entries.none { "saffron" in it.notes })
    }

    @Test
    fun `vocabulary is empty when no fragrance has notes`() {
        assertEquals(emptySet(), runBlocking { StoredNoteVocabulary().notes() }.getOrNull())
    }
}
