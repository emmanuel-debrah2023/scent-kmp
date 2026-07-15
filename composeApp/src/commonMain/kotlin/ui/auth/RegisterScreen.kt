package ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.auth.components.AuthDivider
import ui.auth.components.AuthTextField
import ui.auth.components.BrandWordmark
import ui.auth.components.PrimaryButton
import ui.auth.components.SecondaryButton
import ui.theme.ScentTheme

@Composable
fun RegisterScreen(
    onCreateAccountClick: (String, String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 384.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandWordmark(modifier = Modifier.padding(bottom = 64.dp))

            Text(
                text = "Create account",
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 24.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            AuthTextField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                modifier = Modifier.padding(bottom = 32.dp),
            )

            AuthTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = "Display name",
                modifier = Modifier.padding(bottom = 32.dp),
            )

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email address",
                modifier = Modifier.padding(bottom = 32.dp),
            )

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.padding(bottom = 32.dp),
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            PrimaryButton(
                text = if (isLoading) "Creating account..." else "Create account",
                onClick = { onCreateAccountClick(username, email, displayName, password) },
                enabled = !isLoading,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            AuthDivider(
                text = "or continue with",
                modifier = Modifier.padding(bottom = 32.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SecondaryButton(
                    text = "Google",
                    onClick = { /* TODO: Phase 2 */ },
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Apple",
                    onClick = { /* TODO: Phase 3 */ },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Sign in",
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    modifier = Modifier.clickable { onNavigateToLogin() },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    ScentTheme {
        RegisterScreen(
            onCreateAccountClick = { _, _, _, _ -> },
            onNavigateToLogin = {},
        )
    }
}
