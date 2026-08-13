package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Review
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

@Composable
fun ReviewCard(
    review: Review,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.fragrance.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 17.sp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = review.fragrance.brand.uppercase(),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = ScentThemeExtras.gray400,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = ScentThemeExtras.accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = review.rating.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (review.content.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = review.content,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewCardWithContentPreview() {
    ScentTheme {
        ReviewCard(
            review =
                Review(
                    id = 1,
                    fragrance = Fragrance(id = 1, name = "Aventus", brand = "Creed"),
                    rating = 4,
                    content = "Incredible projection, slightly sweet but not overpowering.",
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewCardNoContentPreview() {
    ScentTheme {
        ReviewCard(
            review =
                Review(
                    id = 2,
                    fragrance = Fragrance(id = 2, name = "Santal 33", brand = "Le Labo"),
                    rating = 5,
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
