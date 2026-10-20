package ui.ask

import ui.surface.SurfaceEvent

data class AskActions(
    val onQueryChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onSurfaceEvent: (SurfaceEvent) -> Unit,
    val onRetry: () -> Unit,
    val onStartOver: () -> Unit,
) {
    companion object {
        /** No-op instance for @Preview composables and tests. */
        fun noOp() =
            AskActions(
                onQueryChange = {},
                onSubmit = {},
                onSurfaceEvent = {},
                onRetry = {},
                onStartOver = {},
            )
    }
}
