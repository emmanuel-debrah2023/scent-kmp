package org.scent.project

import askback.askDailyLimit
import kotlin.test.Test
import kotlin.test.assertEquals

class AskDailyLimitTest {
    @Test
    fun `a limit setting falls back to the default for blank, garbage, zero or negative`() {
        listOf(null, "", " ", "lots", "0", "-3", "2.5").forEach {
            assertEquals(20, askDailyLimit(it, 20), "accepted: $it")
        }
    }

    @Test
    fun `a whole number above zero is used, trimmed`() {
        assertEquals(5, askDailyLimit(" 5 ", 20))
        assertEquals(1000, askDailyLimit("1000", 200))
    }
}
