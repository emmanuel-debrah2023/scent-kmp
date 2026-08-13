package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.CollectionStatus
import org.scent.project.domain.model.Fragrance
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

@Composable
fun BottleItem(
    entry: CollectionEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .width(80.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Cap
        Box(
            modifier =
                Modifier
                    .size(width = 16.dp, height = 12.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(MaterialTheme.colorScheme.outline),
        )
        // Neck
        Box(
            modifier =
                Modifier
                    .size(width = 8.dp, height = 8.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
        )
        // Body
        Box(
            modifier =
                Modifier
                    .size(width = 56.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            val imageUrl = entry.fragrance.imageUrls.firstOrNull()
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = entry.fragrance.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        val sizeMeta = entry.bottleSizeMl?.let { "${it}ml" } ?: ""
        if (sizeMeta.isNotBlank()) {
            Text(
                text = sizeMeta,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = ScentThemeExtras.gray400,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottleItemPreview() {
    ScentTheme {
        BottleItem(
            entry =
                CollectionEntry(
                    fragrance = Fragrance(id = 1, name = "Aventus", brand = "Creed"),
                    status = CollectionStatus.OWNS,
                    bottleSizeMl = 100,
                ),
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BottleItemNoSizePreview() {
    ScentTheme {
        BottleItem(
            entry =
                CollectionEntry(
                    fragrance = Fragrance(id = 2, name = "Santal 33", brand = "Le Labo"),
                    status = CollectionStatus.TRIED,
                ),
            onClick = {},
        )
    }
}
