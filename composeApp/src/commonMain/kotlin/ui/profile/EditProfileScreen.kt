package ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.scent.project.domain.model.User
import ui.accessibility.accessiblePane
import ui.base.UiState
import ui.components.EmptyState
import ui.components.ScentDivider
import ui.components.ScentTextField
import ui.components.StackedTopBar
import ui.components.buttons.ScentPrimaryButton
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

@Composable
fun EditProfileScreen(
    userId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val viewModel: EditProfileViewModel = koinViewModel(parameters = { parametersOf(userId) })
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.error.collect { error ->
            snackbarHostState.showSnackbar(error.message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            snackbarHostState.showSnackbar("Profile saved")
        }
    }

    when (val state = uiState) {
        is UiState.Idle, is UiState.Loading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

        is UiState.Error ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(title = "Something went wrong", message = state.error.message)
            }

        is UiState.Success ->
            EditProfileForm(
                user = state.data,
                formState = formState,
                onDisplayNameChange = viewModel::onDisplayNameChange,
                onBioChange = viewModel::onBioChange,
                onSave = viewModel::save,
                onBack = onBack,
                modifier = modifier,
            )
    }
}

@Composable
private fun EditProfileForm(
    user: User,
    formState: EditProfileFormState,
    onDisplayNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().accessiblePane("Edit profile")) {
        StackedTopBar(title = "Edit profile", onBack = onBack)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = ScentThemeExtras.spacing.authMaxWidth)
                        .padding(horizontal = ScentThemeExtras.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xl),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // TODO(feature/settings-suite): "Change photo" — users.avatar_url and
                    // /api/v1/media/image-upload-url exist, but no endpoint writes a user's
                    // avatar yet.
                    ProfileAvatar(
                        displayName = user.displayName,
                        avatarUrl = user.avatarUrl,
                        size = ScentThemeExtras.spacing.avatarSizeLarge,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        initialsColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        accentRing = true,
                    )
                    Column {
                        Text(text = user.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                ScentTextField(
                    value = formState.displayName,
                    onValueChange = { if (it.length <= 100) onDisplayNameChange(it) },
                    label = "Display name",
                    helperText = "Shown on your listings, reviews and messages",
                    error = formState.displayNameError,
                )

                // TODO(feature/settings-suite): no bio length limit exists client- or
                // server-side yet — needs Validator.validateBio plus a live "n / max" counter.
                ScentTextField(
                    value = formState.bio,
                    onValueChange = onBioChange,
                    label = "Bio",
                    placeholder = "A line about your taste in fragrance",
                    helperText = "Optional",
                    singleLine = false,
                    minLines = 3,
                )

                Spacer(modifier = Modifier.height(ScentThemeExtras.spacing.xl))
            }
        }

        ScentDivider()
        ScentPrimaryButton(
            text = "Save changes",
            onClick = onSave,
            enabled = formState.canSave,
            modifier =
                Modifier.padding(
                    start = ScentThemeExtras.spacing.md,
                    end = ScentThemeExtras.spacing.md,
                    top = ScentThemeExtras.spacing.md,
                    bottom = ScentThemeExtras.spacing.lg,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProfileFormPreview() {
    ScentTheme {
        EditProfileForm(
            user = User(id = 1, username = "edebrah", displayName = "Emmanuel Debrah", bio = "Fragrance collector."),
            formState =
                EditProfileFormState(
                    displayName = "Emmanuel Debrah",
                    bio = "Fragrance collector. Niche over designer, always.",
                ),
            onDisplayNameChange = {},
            onBioChange = {},
            onSave = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProfileFormDirtyPreview() {
    ScentTheme {
        EditProfileForm(
            user = User(id = 1, username = "edebrah", displayName = "Emmanuel Debrah"),
            formState =
                EditProfileFormState(
                    displayName = "Emmanuel D.",
                    isDirty = true,
                    canSave = true,
                ),
            onDisplayNameChange = {},
            onBioChange = {},
            onSave = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProfileFormErrorPreview() {
    ScentTheme {
        EditProfileForm(
            user = User(id = 1, username = "edebrah", displayName = "Emmanuel Debrah"),
            formState =
                EditProfileFormState(
                    displayName = "",
                    isDirty = true,
                    displayNameError = "Enter a display name",
                ),
            onDisplayNameChange = {},
            onBioChange = {},
            onSave = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProfileLoadingPreview() {
    ScentTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
