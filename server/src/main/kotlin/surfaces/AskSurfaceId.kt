package surfaces

import java.util.UUID

private val shape = Regex("^ask-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

/**
 * Ask surfaces get a fresh id per ask: A2UI v0.9 makes a duplicate createSurface an error. The server
 * keeps no state under the id, so [isValid] checks shape only, not that we issued it. That is enough:
 * the id just routes the reply on the caller's own device, and the action context is re-validated.
 */
object AskSurfaceId {
    fun new(): String = "ask-${UUID.randomUUID()}"

    fun isValid(id: String?): Boolean = id != null && shape.matches(id)
}
