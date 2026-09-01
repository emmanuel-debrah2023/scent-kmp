package ui.listing

import org.scent.project.domain.model.ListingKind

/** [ListingKind] is a raw pass-through of the server's enum name (`SEALED`, `OPENED`,
 *  `DECANT`, `TESTER`) — never `.name`'d directly in a composable, always mapped to a
 *  display string. Shared by [CreateListingScreen] and [EditListingScreen]'s kind chips. */
fun ListingKind.displayLabel(): String =
    when (this) {
        ListingKind.SEALED -> "Sealed"
        ListingKind.OPENED -> "Opened"
        ListingKind.DECANT -> "Decant"
        ListingKind.TESTER -> "Tester"
    }
