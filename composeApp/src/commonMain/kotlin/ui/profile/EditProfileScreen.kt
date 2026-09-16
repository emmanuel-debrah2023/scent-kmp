package ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ui.accessibility.accessiblePane
import ui.base.UiState
import ui.components.EmptyState
import ui.components.ScentTextField
import ui.components.buttons.ScentPrimaryButton
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

    LaunchedEffect(Unit) {
        viewModel.error.collect { error ->
            snackbarHostState.showSnackbar(error.message)
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
                initialDisplayName = state.data.displayName,
                initialBio = state.data.bio,
                onSave = viewModel::save,
                onBack = onBack,
                modifier = modifier,
            )
    }
}

@Composable
private fun EditProfileForm(
    initialDisplayName: String,
    initialBio: String,
    onSave: (displayName: String, bio: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ScentThemeExtras.spacing
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var bio by remember { mutableStateOf(initialBio) }

    Column(
        modifier = modifier.fillMaxSize().accessiblePane("Edit profile"),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(text = "Edit Profile", style = MaterialTheme.typography.titleLarge)
        }

        ScentTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = "Display name",
        )

        ScentTextField(
            value = bio,
            onValueChange = { bio = it },
            label = "Bio",
        )

        ScentPrimaryButton(
            text = "SAVE",
            onClick = { onSave(displayName, bio) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
