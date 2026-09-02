// TODO(chore/remove-dead-video-screen): unreferenced — HomeFullBleedScreen.kt's
// Community feed renders its own private TextPostCard/PhotoPostCard/CommunityPostCard
// instead. Notably, the Notion ticket "Wire up post card action buttons" is marked
// Done, but this file — which looks like the one it should have wired — is the
// unreferenced one; the actions landed on HomeFullBleedScreen's private composables.
package ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.Post
import ui.components.VideoPlayer
import ui.theme.ScentThemeExtras

@Composable
fun PostCard(
    post: Post,
    onLikeClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
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
            if (post.contentFormat == ContentFormat.VIDEO && post.mediaUrls.isNotEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clickable { onVideoClick(post.mediaUrls.first()) },
                ) {
                    VideoPlayer(
                        url = post.mediaUrls.first(),
                        thumbnailUrl = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

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
