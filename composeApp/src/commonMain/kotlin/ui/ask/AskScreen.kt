package ui.ask

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.scent.project.domain.error.AppError
import ui.accessibility.accessibleLiveRegion
import ui.accessibility.accessiblePane
import ui.accessibility.decorative
import ui.components.ErrorState
import ui.components.ScentTextField
import ui.components.buttons.ScentPrimaryButton
import ui.surface.ASK_SURFACE_ID
import ui.surface.AskFixtures
import ui.surface.ScentSurface
import ui.surface.SurfaceFixture
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

private const val ASK_BUTTON_LABEL = "Ask"
private const val QUERY_MIN_LINES = 3

@Composable
fun AskScreen(
    modifier: Modifier = Modifier,
    viewModel: AskViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val actions =
        remember(viewModel) {
            AskActions(
                onQueryChange = viewModel::onQueryChange,
                onSubmit = viewModel::submit,
                onSurfaceEvent = viewModel::onSurfaceEvent,
                onRetry = viewModel::retry,
                onStartOver = viewModel::startOver,
            )
        }
    AskContent(model = state.toUiModel(), actions = actions, modifier = modifier)
}

@Composable
fun AskContent(
    model: AskUiModel,
    actions: AskActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .accessiblePane(ASK_PANE_TITLE)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(ScentThemeExtras.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.md),
    ) {
        ScentTextField(
            value = model.query,
            onValueChange = actions.onQueryChange,
            label = ASK_QUERY_LABEL,
            placeholder = ASK_QUERY_PLACEHOLDER,
            enabled = model.queryEnabled,
            error = model.queryError,
            helperText = ASK_QUERY_HELPER,
            singleLine = false,
            minLines = QUERY_MIN_LINES,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
            keyboardActions = KeyboardActions(onSend = { actions.onSubmit() }),
        )
        ScentPrimaryButton(
            text = ASK_BUTTON_LABEL,
            onClick = actions.onSubmit,
            enabled = model.queryEnabled,
        )
        model.statusText?.let { status ->
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.accessibleLiveRegion(),
            )
        }
        AskBody(model.content, actions)
    }
}

@Composable
private fun AskBody(
    content: AskContentModel,
    actions: AskActions,
) {
    when (content) {
        AskContentModel.Empty -> Unit
        is AskContentModel.Progress -> AskProgress(content.slowNote)
        is AskContentModel.Results -> {
            ScentSurface(surface = content.surface, onEvent = actions.onSurfaceEvent)
            content.actionError?.let { AskError(it, actions) }
        }
        is AskContentModel.Failure -> AskError(content.error, actions)
    }
}

@Composable
private fun AskProgress(slowNote: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.sm),
    ) {
        // The status line above announces the wait; the spinner would only add noise.
        CircularProgressIndicator(modifier = Modifier.decorative())
        slowNote?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AskError(
    error: AskErrorModel,
    actions: AskActions,
) {
    ErrorState(
        variant = error.variant,
        title = error.title,
        message = error.message,
        actionLabel = error.retryLabel,
        onAction = actions.onRetry,
        secondaryActionLabel = error.startOverLabel,
        onSecondaryAction = actions.onStartOver,
        announce = true,
    )
}

private val previewQuestions = SurfaceFixture(AskFixtures.QUESTIONS).state.surfaces.getValue(ASK_SURFACE_ID)

private val previewResults =
    SurfaceFixture(AskFixtures.QUESTIONS, AskFixtures.RESULTS).state.surfaces.getValue(ASK_SURFACE_ID)

@Preview(showBackground = true)
@Composable
private fun AskEntryPreview() {
    ScentTheme { AskContent(AskUiState().toUiModel(), AskActions.noOp()) }
}

@Preview(showBackground = true)
@Composable
private fun AskAskingPreview() {
    ScentTheme {
        AskContent(
            AskUiState(query = "a gift for my dad", phase = AskPhase.Asking, isSlow = true).toUiModel(),
            AskActions.noOp(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AskQuestionsPreview() {
    ScentTheme {
        AskContent(
            AskUiState(query = "a gift for my dad", phase = AskPhase.Showing(previewQuestions)).toUiModel(),
            AskActions.noOp(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AskResultsPreview() {
    ScentTheme {
        AskContent(
            AskUiState(query = "a gift for my dad", phase = AskPhase.Showing(previewResults)).toUiModel(),
            AskActions.noOp(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AskFailedPreview() {
    ScentTheme {
        AskContent(
            AskUiState(
                query = "a gift for my dad",
                phase = AskPhase.Failed(AppError.NetworkError.NoConnection()),
            ).toUiModel(),
            AskActions.noOp(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AskActionFailedPreview() {
    ScentTheme {
        AskContent(
            AskUiState(
                query = "a gift for my dad",
                phase = AskPhase.Showing(previewQuestions, actionError = AppError.NetworkError.Timeout()),
            ).toUiModel(),
            AskActions.noOp(),
        )
    }
}
