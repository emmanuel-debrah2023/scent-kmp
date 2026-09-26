package ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.User
import ui.accessibility.accessibleClickable
import ui.accessibility.accessiblePane
import ui.base.UiState
import ui.components.ErrorState
import ui.components.ErrorStateVariant
import ui.components.ScentConfirmDialog
import ui.components.ScentDivider
import ui.components.StackedTopBar
import ui.components.shimmerPlaceholder
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * Settings' profile-summary card loads the current user via [SettingsViewModel]; the Log out
 * row opens [ScentConfirmDialog] rather than calling [onLogout] directly. [onLogout] itself
 * is already threaded down from `App.kt`'s `sessionViewModel.logout()`.
 *
 * TODO(feature/settings-suite): Account (Sign-in & security, Notifications) and About
 * (Help & support, Terms & privacy) rows, plus the app-version footer, have no screens,
 * routes, endpoints, or commonMain version source yet — see [SettingsContent].
 */
@Composable
fun SettingsScreen(
    userId: Int,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = koinViewModel(parameters = { parametersOf(userId) })
    val uiState by viewModel.uiState.collectAsState()

    SettingsContent(
        uiState = uiState,
        onLogout = onLogout,
        onBack = onBack,
        onEditProfile = onEditProfile,
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    uiState: UiState<User>,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().accessiblePane("Settings")) {
        StackedTopBar(title = "Settings", onBack = onBack)

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = ScentThemeExtras.spacing.md)) {
            when (uiState) {
                is UiState.Idle, is UiState.Loading -> ProfileSummaryCardSkeleton()
                is UiState.Error ->
                    ErrorState(
                        variant = ErrorStateVariant.Error,
                        title = "Couldn't load your profile",
                        message = uiState.error.message,
                    )
                is UiState.Success -> ProfileSummaryCard(user = uiState.data, onClick = onEditProfile)
            }
        }

        ScentDivider(
            modifier =
                Modifier.padding(
                    horizontal = ScentThemeExtras.spacing.md,
                    vertical = ScentThemeExtras.spacing.md,
                ),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .accessibleClickable(label = "Log out", onClick = { showLogoutConfirm = true })
                    .padding(ScentThemeExtras.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "Log out",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // TODO(feature/settings-suite): Account (Sign-in & security, Notifications) and About
        // (Help & support, Terms & privacy) rows — no screens, routes, or endpoints exist yet.

        // TODO(feature/settings-suite): app version isn't exposed to commonMain yet
        // (versionName lives only in composeApp/build.gradle.kts) — needed for the
        // scent wordmark + "Version x.y.z" footer.
    }

    if (showLogoutConfirm) {
        ScentConfirmDialog(
            title = "Log out of scent?",
            message = "You'll need to sign in again to buy, sell or message sellers.",
            confirmLabel = "LOG OUT",
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            },
            onDismiss = { showLogoutConfirm = false },
            isDestructive = true,
        )
    }
}

@Composable
private fun ProfileSummaryCard(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                .accessibleClickable(label = "Edit profile", onClick = onClick)
                .padding(ScentThemeExtras.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            size = ScentThemeExtras.spacing.avatarSizeMedium,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            initialsColor = MaterialTheme.colorScheme.onPrimaryContainer,
            accentRing = true,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Edit profile",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = ScentThemeExtras.interactive,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileSummaryCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                .padding(ScentThemeExtras.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(ScentThemeExtras.spacing.avatarSizeMedium)
                    .clip(CircleShape)
                    .shimmerPlaceholder(CircleShape),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xxs),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.5f)
                        .height(ScentThemeExtras.spacing.skeletonLineMedium)
                        .shimmerPlaceholder(),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.3f)
                        .height(ScentThemeExtras.spacing.skeletonLineSmall)
                        .shimmerPlaceholder(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsLoadingPreview() {
    ScentTheme {
        SettingsContent(uiState = UiState.Loading, onLogout = {}, onBack = {}, onEditProfile = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsLoadedPreview() {
    ScentTheme {
        SettingsContent(
            uiState = UiState.Success(User(id = 1, username = "edebrah", displayName = "Emmanuel Debrah")),
            onLogout = {},
            onBack = {},
            onEditProfile = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsErrorPreview() {
    ScentTheme {
        SettingsContent(
            uiState = UiState.Error(AppError.Unknown()),
            onLogout = {},
            onBack = {},
            onEditProfile = {},
        )
    }
}
