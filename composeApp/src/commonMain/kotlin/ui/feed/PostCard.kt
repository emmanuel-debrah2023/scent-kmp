package ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.scent.project.domain.model.Post
import ui.theme.ScentThemeExtras

@Composable
fun PostCard(
    post: Post,
    onLikeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ScentThemeExtras.spacing
    val elevation = ScentThemeExtras.elevation

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.card),
    ) {
        Column(modifier = Modifier.padding(spacing.cardPadding)) {
            if (post.textContent.isNotEmpty()) {
                Text(
                    text = post.textContent,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (post.hashtags.isNotEmpty()) {
                Text(
                    text = post.hashtags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = spacing.xs),
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onLikeClick(post.id) },
                        modifier = Modifier.size(spacing.iconSizeLarge),
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (post.isLiked) "Unlike" else "Like",
                            tint =
                                if (post.isLiked) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                    Spacer(Modifier.width(spacing.xxs))
                    Text(
                        text = "${post.likeCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = "${post.commentCount} comments",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
