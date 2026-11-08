package org.scent.project.data.remote.a2ui.catalog

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import org.scent.project.data.remote.a2ui.DataBinding

const val SCENT_CATALOG_ID = "https://scent.app/a2ui/scent/v1"

// The scent/v1 component vocabulary. Props that carry facts are DataBinding (a JSON Pointer), never a
// literal; only agent-written wording (labels, headings) is a plain string. Every field is nullable
// so a malformed server payload decodes instead of throwing (ADS-STE100 null-safety).
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("component")
@Serializable
sealed interface ScentComponent {
    val id: String?

    @Serializable
    @SerialName("Column")
    data class Column(
        override val id: String? = null,
        @SchemaDescription("Ids of the child components, in display order")
        val children: List<String>? = null,
        @SchemaDescription(
            "Column style: raised draws the children in one raised card; anything else is a plain column",
        )
        val variant: String? = null,
    ) : ScentComponent

    @Serializable
    @SerialName("Text")
    data class Text(
        override val id: String? = null,
        @SchemaDescription("Heading or body wording shown as written")
        val text: String? = null,
        @SchemaDescription("A2UI text variant: h1 to h5 are announced as headings; caption and body are plain text")
        val variant: String? = null,
    ) : ScentComponent

    @Serializable
    @SerialName("Button")
    data class Button(
        override val id: String? = null,
        @SchemaDescription("Button label, e.g. Show gifts")
        val label: String? = null,
        @SchemaDescription("Event sent when pressed; the name must be a ScentAction")
        val action: A2uiAction? = null,
    ) : ScentComponent

    @Serializable
    @SerialName("ChoicePicker")
    data class ChoicePicker(
        override val id: String? = null,
        @SchemaDescription("Question wording shown above the options")
        val label: String? = null,
        @SchemaDescription("Data path of the list of options; Kotlin supplies every option")
        val options: DataBinding? = null,
        @SchemaDescription("Data path the selected option is written to")
        val value: DataBinding? = null,
    ) : ScentComponent

    @Serializable
    @SerialName("RangeSlider")
    data class RangeSlider(
        override val id: String? = null,
        @SchemaDescription("Question wording shown above the slider")
        val label: String? = null,
        @SchemaDescription("Data path of the minimum value")
        val min: DataBinding? = null,
        @SchemaDescription("Data path of the maximum value")
        val max: DataBinding? = null,
        @SchemaDescription("Data path of the step size")
        val step: DataBinding? = null,
        @SchemaDescription("Data path the chosen value is written to")
        val value: DataBinding? = null,
    ) : ScentComponent

    @Serializable
    @SerialName("PreferenceSummary")
    data class PreferenceSummary(
        override val id: String? = null,
        @SchemaDescription("Data path of the answered preferences shown in the bar")
        val items: DataBinding? = null,
        @SchemaDescription("Event sent when the user taps Change")
        val action: A2uiAction? = null,
    ) : ScentComponent

    @Serializable
    @SerialName("RecommendationCard")
    data class RecommendationCard(
        override val id: String? = null,
        @SchemaDescription("Data path of the fragrance name")
        val name: DataBinding? = null,
        @SchemaDescription("Data path of the price")
        val price: DataBinding? = null,
        @SchemaDescription("Data path of the one line reason this result fits, sent by servers older than reasons")
        val why: DataBinding? = null,
        @SchemaDescription("Event sent when the card is tapped; open_listing with a listingId binding")
        val action: A2uiAction? = null,
        @SchemaDescription("Data path of the formatted facts line, e.g. Sealed · 100 ml · Atelier Nord")
        val meta: DataBinding? = null,
        @SchemaDescription(
            "Data path of up to three reasons this result fits, in order: smell, when to wear, why it fits",
        )
        val reasons: DataBinding? = null,
    ) : ScentComponent

    // Says who wrote the wording near it. The server sends only enum values; the app owns every word,
    // so a model can never word its own attribution.
    @Serializable
    @SerialName("ProvenanceNote")
    data class ProvenanceNote(
        override val id: String? = null,
        @SchemaDescription("Who wrote the nearby wording: ai_written, ai_paused or ai_unavailable")
        val source: String? = null,
        @SchemaDescription("What the note is about: questions or reasons")
        val subject: String? = null,
        @SchemaDescription("Event sent from the note: retry_ask on ai_unavailable, rate_reasons on ai_written")
        val action: A2uiAction? = null,
        @SchemaDescription("Data path the reader's rating, up or down, is written to before rate_reasons is sent")
        val rating: DataBinding? = null,
    ) : ScentComponent

    // Decode only: a component name this app does not know. It keeps the surface decodable so the
    // renderer can skip it instead of dropping the whole message.
    @Serializable
    @SerialName("Unknown")
    data class Unknown(
        override val id: String? = null,
    ) : ScentComponent
}

/** The A2UI v0.9 text variants the scent/v1 catalog uses. */
object TextVariant {
    const val HEADING = "h2"
    const val BODY = "body"
    const val CAPTION = "caption"

    /** h1 to h5 are headings; anything else, or no variant, is plain text. */
    fun isHeading(variant: String?): Boolean =
        variant != null && variant.length == 2 && variant[0] == 'h' && variant[1] in '1'..'5'
}

/** The styles a Column can ask for. An older client ignores the prop and draws a plain column. */
object ColumnVariant {
    const val RAISED = "raised"
}
