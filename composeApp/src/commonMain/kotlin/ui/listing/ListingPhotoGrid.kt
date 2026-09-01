package ui.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import org.scent.project.domain.error.AppError
import ui.accessibility.accessibleLabel
import ui.accessibility.accessibleLiveRegion
import ui.accessibility.collectionContainer
import ui.accessibility.collectionItem
import ui.accessibility.withCustomActions
import ui.media.PickedImage
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

private const val GRID_COLUMNS = 3

/**
 * Grid of picked/uploading listing photos. Move-left/move-right buttons rather than
 * drag-and-drop for reorder — deterministic, and the visual and screen-reader paths are
 * the same code instead of drag needing a parallel [withCustomActions] implementation.
 *
 * Plain chunked [Row]s, not [androidx.compose.foundation.lazy.grid.LazyVerticalGrid]: the
 * parent form already scrolls, [org.scent.project.domain.usecase.MAX_LISTING_PHOTOS] (6) never
 * needs virtualization, and a lazy grid nested in a scrollable Column throws at measure time
 * (infinite height constraint) — same reasoning as `FragranceSuggestionList` in
 * `CreateListingScreen.kt`.
 */
@Composable
fun ListingPhotoGrid(
    photos: List<ListingPhoto>,
    onRemove: (Int) -> Unit,
    onMove: (id: Int, delta: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = photos.chunked(GRID_COLUMNS)
    Column(
        modifier = modifier.collectionContainer(rowCount = rows.size, columnCount = GRID_COLUMNS),
        verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.sm),
    ) {
        rows.forEachIndexed { rowIndex, rowPhotos ->
            Row(horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.sm)) {
                rowPhotos.forEachIndexed { columnIndex, photo ->
                    val index = rowIndex * GRID_COLUMNS + columnIndex
                    ListingPhotoTile(
                        photo = photo,
                        index = index,
                        count = photos.size,
                        onRemove = { onRemove(photo.id) },
                        onMoveLeft = { onMove(photo.id, -1) },
                        onMoveRight = { onMove(photo.id, 1) },
                        modifier =
                            Modifier
                                .weight(1f)
                                .collectionItem(rowIndex = rowIndex, columnIndex = columnIndex),
                    )
                }
                repeat(GRID_COLUMNS - rowPhotos.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ListingPhotoTile(
    photo: ListingPhoto,
    index: Int,
    count: Int,
    onRemove: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUploading = photo.status is PhotoUploadStatus.Uploading
    val actions =
        buildList {
            if (index > 0) {
                add(
                    CustomAccessibilityAction("Move photo ${index + 1} earlier") {
                        onMoveLeft()
                        true
                    },
                )
            }
            if (index <
                count - 1
            ) {
                add(
                    CustomAccessibilityAction("Move photo ${index + 1} later") {
                        onMoveRight()
                        true
                    },
                )
            }
            if (!isUploading) {
                add(
                    CustomAccessibilityAction("Remove photo ${index + 1}") {
                        onRemove()
                        true
                    },
                )
            }
        }.toTypedArray()

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .withCustomActions(*actions),
    ) {
        AsyncImage(
            model = photo.imageModel,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            contentScale = ContentScale.Crop,
        )

        when (val status = photo.status) {
            is PhotoUploadStatus.Uploading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                            .accessibleLiveRegion(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeMedium))
                }
            }
            is PhotoUploadStatus.Failed -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f))
                            .accessibleLiveRegion()
                            .padding(ScentThemeExtras.spacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Upload failed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            is PhotoUploadStatus.Uploaded -> Unit
        }

        if (!isUploading) {
            IconButton(
                onClick = onRemove,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .accessibleLabel("Remove photo ${index + 1}"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Box(modifier = Modifier.align(Alignment.BottomStart)) {
                if (index > 0) {
                    IconButton(
                        onClick = onMoveLeft,
                        modifier = Modifier.accessibleLabel("Move photo ${index + 1} earlier"),
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            }
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                if (index < count - 1) {
                    IconButton(
                        onClick = onMoveRight,
                        modifier = Modifier.accessibleLabel("Move photo ${index + 1} later"),
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListingPhotoGridPreview() {
    ScentTheme {
        ListingPhotoGrid(
            photos =
                listOf(
                    ListingPhoto(
                        id = 1,
                        source = PhotoSource.Remote("https://example.com/photo-1.jpg"),
                        status = PhotoUploadStatus.Uploaded(mediaId = 1),
                    ),
                    ListingPhoto(
                        id = 2,
                        source = PhotoSource.Local(PickedImage(bytes = ByteArray(0), contentType = "image/jpeg")),
                        status = PhotoUploadStatus.Uploading,
                    ),
                    ListingPhoto(
                        id = 3,
                        source = PhotoSource.Local(PickedImage(bytes = ByteArray(0), contentType = "image/jpeg")),
                        status = PhotoUploadStatus.Failed(error = AppError.ContentError.UploadFailed()),
                    ),
                ),
            onRemove = {},
            onMove = { _, _ -> },
        )
    }
}
