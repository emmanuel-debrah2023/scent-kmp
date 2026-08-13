package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras
import kotlin.math.roundToInt

@Composable
fun ListingCard(
    listing: Listing,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(width = 64.dp, height = 80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                val imageUrl = listing.fragrance.imageUrls.firstOrNull()
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = listing.fragrance.brand.uppercase(),
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = ScentThemeExtras.gray400,
                )
                Text(
                    text = listing.fragrance.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 17.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = listing.condition,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "£${listing.price.roundToInt()}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (listing.isNegotiable) "negotiable" else "firm",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = ScentThemeExtras.gray400,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListingCardPreview() {
    ScentTheme {
        ListingCard(
            listing =
                Listing(
                    id = 1,
                    fragrance = Fragrance(id = 1, name = "Aventus", brand = "Creed"),
                    sellerId = 1,
                    price = 185.0,
                    condition = "90% full",
                    isNegotiable = true,
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ListingCardFirmPreview() {
    ScentTheme {
        ListingCard(
            listing =
                Listing(
                    id = 2,
                    fragrance = Fragrance(id = 2, name = "Santal 33", brand = "Le Labo"),
                    sellerId = 2,
                    price = 120.0,
                    condition = "Unused",
                    isNegotiable = false,
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
