package ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.scent.project.domain.model.User
import ui.accessibility.accessiblePane
import ui.accessibility.mergedGroup
import ui.base.UiState
import ui.components.EmptyState
import ui.components.ErrorState
import ui.components.ErrorStateVariant
import ui.theme.ScentThemeExtras

@Composable
fun ProfileFollowersScreen(
    userId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ProfileFollowersViewModel = koinViewModel(parameters = { parametersOf(userId) })
    val uiState by viewModel.uiState.collectAsState()

    ConnectionsScreenContent(
        title = "Followers",
        emptyTitle = "No followers yet",
        emptyMessage = "When people follow you, they'll show up here.",
        uiState = uiState,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Shared body for [ProfileFollowersScreen] and [ProfileFollowingScreen] — same loading /
 * error / empty / list shape, differing only in copy and which [SocialRepository]-backed
 * ViewModel supplies [uiState]. `internal` rather than `private` so both screens' files can
 * use it despite Kotlin's file-scoped `private`.
 */
@Composable
internal fun ConnectionsScreenContent(
    title: String,
    emptyTitle: String,
    emptyMessage: String,
    uiState: UiState<List<User>>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .accessiblePane(title),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        }

        when (uiState) {
            is UiState.Idle, is UiState.Loading ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

            is UiState.Error ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorState(
                        variant = ErrorStateVariant.Error,
                        title = "Something went wrong",
                        message = uiState.error.message,
                    )
                }

            is UiState.Success ->
                if (uiState.data.isEmpty()) {
                    EmptyState(
                        title = emptyTitle,
                        message = emptyMessage,
                        modifier = Modifier.padding(horizontal = ScentThemeExtras.spacing.md),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(uiState.data) { user -> ConnectionRow(user) }
                    }
                }
        }
    }
}

@Composable
private fun ConnectionRow(
    user: User,
    modifier: Modifier = Modifier,
) {
    val spacing = ScentThemeExtras.spacing

    // Display-only: there is no per-tab route yet to view another user's profile from
    // Profile (see ProfileScreen's own "isOwnProfile = true" comment), so this row is
    // non-interactive rather than a no-op click target.
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm)
                .mergedGroup(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(displayName = user.displayName, avatarUrl = user.avatarUrl, size = spacing.avatarSizeSmall)
        Column {
            Text(text = user.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
