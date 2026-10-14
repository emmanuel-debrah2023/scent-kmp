package org.scent.project

import preferences.ScentFamily
import routing.seedFragrances
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScentFamilyTest {
    private val seededNotes = seedFragrances.flatMap { it.notes.all }.toSet()

    private fun assertEverySeededNote(family: ScentFamily) {
        assertTrue(family.notes.isNotEmpty(), "$family maps to no notes")
        val missing = family.notes - seededNotes
        assertTrue(missing.isEmpty(), "$family maps to notes missing from the seed data: $missing")
    }

    @Test
    fun `fresh notes all exist in the seed data`() = assertEverySeededNote(ScentFamily.FRESH)

    @Test
    fun `woody notes all exist in the seed data`() = assertEverySeededNote(ScentFamily.WOODY)

    @Test
    fun `spicy notes all exist in the seed data`() = assertEverySeededNote(ScentFamily.SPICY)

    @Test
    fun `floral notes all exist in the seed data`() = assertEverySeededNote(ScentFamily.FLORAL)

    @Test
    fun `gourmand notes all exist in the seed data`() = assertEverySeededNote(ScentFamily.GOURMAND)

    @Test
    fun `no note belongs to two families`() {
        val all = ScentFamily.entries.flatMap { it.notes }
        assertEquals(all.toSet().size, all.size, "a note is mapped to more than one family")
    }

    @Test
    fun `family notes are lowercase so they match stored notes`() {
        ScentFamily.entries.forEach { family ->
            assertTrue(family.notes.all { it == it.lowercase() }, "$family has a non-lowercase note")
        }
    }
}
