package ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.auth.components.*
import ui.theme.ScentTheme

@Composable
fun LoginScreen(
    onSignInClick: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    emailError: String? = null,
    passwordError: String? = null,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 384.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandWordmark(modifier = Modifier.padding(bottom = 64.dp))

            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email address",
                error = emailError,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                error = passwordError,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            PrimaryButton(
                text = "Sign in",
                onClick = { onSignInClick(email, password) },
                modifier = Modifier.padding(bottom = 32.dp)
            )

            AuthDivider(
                text = "or continue with",
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SecondaryButton(
                    text = "Google",
                    onClick = { /* TODO: Phase 2 */ },
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = "Apple",
                    onClick = { /* TODO: Phase 3 */ },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Register",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    ScentTheme {
        LoginScreen(
            onSignInClick = { _, _ -> },
            onNavigateToRegister = {}
        )
    }
}
