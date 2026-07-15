import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import utils.ImageLoader

/**
 * Simple navigation example using Compose state
 */
sealed class Screen {
    object Home : Screen()

    data class FragranceDetail(
        val fragranceId: String,
        val fragranceName: String,
    ) : Screen()
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            FragranceListScreen(
                onFragranceClick = { id, name ->
                    currentScreen = Screen.FragranceDetail(id, name)
                },
            )
        }

        is Screen.FragranceDetail -> {
            FragranceDetailScreen(
                fragranceId = screen.fragranceId,
                fragranceName = screen.fragranceName,
                onBackClick = { currentScreen = Screen.Home },
            )
        }
    }
}

@Composable
fun FragranceListScreen(onFragranceClick: (String, String) -> Unit) {
    val sampleFragrances =
        listOf(
            Triple("1", "Chanel No. 5", "https://example.com/chanel5.jpg"),
            Triple("2", "Dior Sauvage", "https://example.com/sauvage.jpg"),
            Triple("3", "Tom Ford Black Orchid", null),
        )

    Column {
        Text(
            text = "Fragrances",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sampleFragrances) { (id, name, imageUrl) ->
                FragranceCard(
                    name = name,
                    imageUrl = imageUrl,
                    onClick = { onFragranceClick(id, name) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FragranceDetailScreen(
    fragranceId: String,
    fragranceName: String,
    onBackClick: () -> Unit,
) {
    Column {
        TopAppBar(
            title = { Text(fragranceName) },
            navigationIcon = {
                TextButton(onClick = onBackClick) {
                    Text("← Back")
                }
            },
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = ImageLoader.getImageUrl(null),
                contentDescription = fragranceName,
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = fragranceName,
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Fragrance ID: $fragranceId",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text =
                    "This is a detailed view of the fragrance. In a real app, you would load " +
                        "fragrance details, reviews, notes, and other information here.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
fun FragranceCard(
    name: String,
    imageUrl: String?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Using Coil for image loading with placeholder
            AsyncImage(
                model = ImageLoader.getImageUrl(imageUrl),
                contentDescription = name,
                modifier = Modifier.size(60.dp),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
