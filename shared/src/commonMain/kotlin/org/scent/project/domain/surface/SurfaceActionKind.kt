package org.scent.project.domain.surface

import org.scent.project.data.remote.a2ui.catalog.ScentAction

/**
 * True for the action that only reopens the answers: the server replays them without a model call.
 * Lives here so the UI layer can tell the two actions apart without seeing the wire-format allowlist.
 */
val SurfaceAction.reopensAnswers: Boolean
    get() = name == ScentAction.EDIT_ANSWERS.wireName
