package org.scent.project

import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pure standalone function that validates a required property from a [Properties] object.
 *
 * @param props     the [Properties] to read from
 * @param key       the property key to look up
 * @param buildType the build type name (used in the error message)
 * @return the non-blank value stored under [key]
 * @throws IllegalArgumentException if the key is absent or its value is blank
 */
fun requireProperty(props: Properties, key: String, buildType: String): String {
    val value = props.getProperty(key)
    if (value.isNullOrBlank()) {
        throw IllegalArgumentException(
            "Missing or empty required property '$key' in local.properties " +
            "(required for the '$buildType' build type)."
        )
    }
    return value
}

class RequirePropertyTest {

    @Test
    fun whenKeyPresentAndNonBlank_returnsValue() {
        val props = Properties().apply { setProperty("MY_KEY", "my-value") }
        val result = requireProperty(props, "MY_KEY", "debug")
        assertEquals("my-value", result)
    }

    @Test
    fun whenKeyPresentButBlank_throwsWithKeyAndBuildType() {
        val props = Properties().apply { setProperty("MY_KEY", "") }
        val ex = assertFailsWith<IllegalArgumentException> {
            requireProperty(props, "MY_KEY", "release")
        }
        assertTrue(
            ex.message?.contains("MY_KEY") == true,
            "Expected message to contain key name 'MY_KEY', but was: ${ex.message}"
        )
        assertTrue(
            ex.message?.contains("release") == true,
            "Expected message to contain build type 'release', but was: ${ex.message}"
        )
    }

    @Test
    fun whenKeyPresentButWhitespace_throwsWithKeyAndBuildType() {
        val props = Properties().apply { setProperty("MY_KEY", "   ") }
        val ex = assertFailsWith<IllegalArgumentException> {
            requireProperty(props, "MY_KEY", "debug")
        }
        assertTrue(
            ex.message?.contains("MY_KEY") == true,
            "Expected message to contain key name 'MY_KEY', but was: ${ex.message}"
        )
        assertTrue(
            ex.message?.contains("debug") == true,
            "Expected message to contain build type 'debug', but was: ${ex.message}"
        )
    }

    @Test
    fun whenKeyAbsent_throwsWithKeyAndBuildType() {
        val props = Properties() // key not set at all
        val ex = assertFailsWith<IllegalArgumentException> {
            requireProperty(props, "MISSING_KEY", "release")
        }
        assertTrue(
            ex.message?.contains("MISSING_KEY") == true,
            "Expected message to contain key name 'MISSING_KEY', but was: ${ex.message}"
        )
        assertTrue(
            ex.message?.contains("release") == true,
            "Expected message to contain build type 'release', but was: ${ex.message}"
        )
    }
}
