package ui.surface

import kotlin.test.Test
import kotlin.test.assertEquals

class SurfaceLabelsTest {
    @Test
    fun `pounds shows whole pounds`() {
        assertEquals("£80", SurfaceLabels.pounds(80.0))
        assertEquals("£80", SurfaceLabels.pounds(79.6))
    }

    @Test
    fun `budget value reads as a phrase`() {
        assertEquals("up to £80", SurfaceLabels.budgetValue(80.0))
    }

    @Test
    fun `summary joins the answers with a middle dot`() {
        assertEquals(
            "Woody · up to £80 · Full bottle",
            SurfaceLabels.summary(listOf("Woody", "up to £80", "Full bottle")),
        )
        assertEquals("Woody", SurfaceLabels.summary(listOf("Woody")))
    }
}
