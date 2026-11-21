package ui.feed

internal fun Int.toDisplayCount(): String = if (this >= 1000) "${this / 1000}k" else toString()
