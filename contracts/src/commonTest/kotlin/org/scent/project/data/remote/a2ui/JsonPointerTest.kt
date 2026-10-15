package org.scent.project.data.remote.a2ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonPointerTest {
    @Test
    fun `the empty pointer and well formed pointers are valid`() {
        assertTrue(JsonPointer.isValid(""))
        assertTrue(JsonPointer.isValid("/"))
        assertTrue(JsonPointer.isValid("/results/0/price"))
        assertTrue(JsonPointer.isValid("/a~0b"))
        assertTrue(JsonPointer.isValid("/a~1b"))
    }

    @Test
    fun `a pointer must start with a slash`() {
        assertFalse(JsonPointer.isValid("a/b"))
        assertFalse(JsonPointer.isValid("#/a"))
    }

    @Test
    fun `a tilde must be followed by 0 or 1`() {
        assertFalse(JsonPointer.isValid("/a~2"))
        assertFalse(JsonPointer.isValid("/a~"))
        assertFalse(JsonPointer.isValid("/~x/b"))
    }
}
