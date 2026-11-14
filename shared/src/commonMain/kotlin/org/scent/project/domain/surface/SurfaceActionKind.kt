package org.scent.project.domain.surface

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.scent.project.data.remote.a2ui.catalog.ScentAction

internal const val LISTING_ID_KEY = "listingId"

/**
 * True for the action that only reopens the answers: the server replays them without a model call.
 * Lives here so the UI layer can tell the actions apart without seeing the wire-format allowlist.
 */
val SurfaceAction.reopensAnswers: Boolean
    get() = name == ScentAction.EDIT_ANSWERS.wireName

/**
 * True for the thumbs on AI-written reasons. A rating is feedback, not a round trip: the surface stays on
 * screen and nothing else waits on it.
 */
val SurfaceAction.ratesReasons: Boolean
    get() = name == ScentAction.RATE_REASONS.wireName

/** True for an action the app handles itself and must never send to the server (open_listing). */
val SurfaceAction.handledOnClient: Boolean
    get() = ScentAction.fromWireName(name)?.handledOnClient == true

/**
 * The listing id when this is the client-handled open_listing action and its context holds a valid
 * id; null otherwise. The app, not the server, acts on it, so the id is re-validated here.
 */
val SurfaceAction.listingToOpen: Int?
    get() = if (name == ScentAction.OPEN_LISTING.wireName) context[LISTING_ID_KEY].listingId() else null

/** A listing id is a whole JSON number above zero: not a string, not fractional, not out of Int range. */
internal fun JsonElement?.listingId(): Int? =
    (this as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull?.takeIf { it > 0 }
