package ui.surface

internal const val ASK_SURFACE_ID = "ask-123e4567-e89b-42d3-a456-426614174000"

private const val CATALOG = "https://scent.app/a2ui/scent/v1"

private val answerContext =
    """{"intent":{"path":"/answers/intent"},"family":{"path":"/answers/family"},""" +
        """"budget":{"path":"/answers/budget"},"size":{"path":"/answers/size"}}"""

private val ratingContext =
    """{"rating":{"path":"/feedback/rating"},"intent":{"path":"/answers/intent"},""" +
        """"family":{"path":"/answers/family"},"budget":{"path":"/answers/budget"},"size":{"path":"/answers/size"}}"""

/**
 * The two batches the ask route sends, as the server writes them (see `AskSurfaceBuilder`), with the
 * answers filled in as an edit would show them. For previews and tests only; a shared golden fixture
 * between server and client is a follow-up of ADR-0003.
 *
 * They already use the ProvenanceNote and reasons shapes that `feature/ask-provenance-note-server` makes
 * the server send.
 */
internal object AskFixtures {
    /** createSurface, data model, then the question components. */
    val QUESTIONS: String =
        """{"messages":[""" +
            """{"version":"v0.9","createSurface":{"surfaceId":"$ASK_SURFACE_ID","catalogId":"$CATALOG"}},""" +
            """{"version":"v0.9","updateDataModel":{"surfaceId":"$ASK_SURFACE_ID","path":"/","value":{""" +
            """"answers":{"intent":"GIFT","family":"WOODY","budget":80.0,"size":"FULL"},""" +
            """"options":{"family":[{"value":"FRESH","label":"Fresh"},{"value":"WOODY","label":"Woody"},""" +
            """{"value":"SPICY","label":"Spicy"},{"value":"FLORAL","label":"Floral"},""" +
            """{"value":"GOURMAND","label":"Gourmand"}],""" +
            """"size":[{"value":"FULL","label":"Full bottle"},{"value":"DECANT","label":"Decant set to try"}]},""" +
            """"budget":{"min":20,"max":150,"step":5}}}},""" +
            """{"version":"v0.9","updateComponents":{"surfaceId":"$ASK_SURFACE_ID","components":[""" +
            """{"component":"Column","id":"root","children":["provenance","heading","questions","submit"]},""" +
            """{"component":"ProvenanceNote","id":"provenance","source":"ai_written","subject":"questions"},""" +
            """{"component":"Text","id":"heading","text":"Three quick picks and we'll find him something",""" +
            """"variant":"h2"},""" +
            """{"component":"Column","id":"questions","children":["family","budget","size"],"variant":"raised"},""" +
            """{"component":"ChoicePicker","id":"family","label":"Scent family",""" +
            """"options":{"path":"/options/family"},"value":{"path":"/answers/family"}},""" +
            """{"component":"RangeSlider","id":"budget","label":"Budget","min":{"path":"/budget/min"},""" +
            """"max":{"path":"/budget/max"},"step":{"path":"/budget/step"},"value":{"path":"/answers/budget"}},""" +
            """{"component":"ChoicePicker","id":"size","label":"Bottle size",""" +
            """"options":{"path":"/options/size"},"value":{"path":"/answers/size"}},""" +
            """{"component":"Button","id":"submit","label":"Show gifts","action":{"event":{""" +
            """"name":"answer_questions","context":$answerContext}}}]}}]}"""

    /** The reply to the answers: new data and components on the same surface, no createSurface. */
    val RESULTS: String =
        """{"messages":[""" +
            """{"version":"v0.9","updateDataModel":{"surfaceId":"$ASK_SURFACE_ID","path":"/","value":{""" +
            """"answers":{"intent":"GIFT","family":"WOODY","budget":80.0,"size":"FULL"},""" +
            """"feedback":{"rating":null},""" +
            """"summary":["Woody","up to £80","Full bottle"],""" +
            """"results":[""" +
            """{"listingId":7,"name":"Cedar & Smoke","brand":"Atelier Nord","price":"£76",""" +
            """"meta":"Sealed · 100 ml · Atelier Nord",""" +
            """"reasons":["Dry cedarwood and vetiver","Close to the skin, easy for the office",""" +
            """"A classic woody gift"]},""" +
            """{"listingId":8,"name":"Birch Ledger","brand":"Atelier Nord","price":"£64",""" +
            """"meta":"Sealed · 75 ml · Atelier Nord",""" +
            """"reasons":["Smoky birch with a soft musk finish","Easy to wear every day"]},""" +
            """{"listingId":9,"name":"Oud Lantern","brand":"Maison Verre","price":"£79",""" +
            """"meta":"Opened · 95% full · 100 ml · Maison Verre",""" +
            """"reasons":["Warm oud and amber","Best for evenings out","Richer than his usual woods"]}]}}},""" +
            """{"version":"v0.9","updateComponents":{"surfaceId":"$ASK_SURFACE_ID","components":[""" +
            """{"component":"Column","id":"root","children":["summary","provenance","card_1","card_2","card_3"]},""" +
            """{"component":"ProvenanceNote","id":"provenance","source":"ai_written","subject":"reasons",""" +
            """"action":{"event":{"name":"rate_reasons","context":$ratingContext}},""" +
            """"rating":{"path":"/feedback/rating"}},""" +
            """{"component":"PreferenceSummary","id":"summary","items":{"path":"/summary"},""" +
            """"action":{"event":{"name":"edit_answers","context":$answerContext}}},""" +
            """{"component":"RecommendationCard","id":"card_1","name":{"path":"/results/0/name"},""" +
            """"price":{"path":"/results/0/price"},"reasons":{"path":"/results/0/reasons"},""" +
            """"meta":{"path":"/results/0/meta"},""" +
            """"action":{"event":{"name":"open_listing",""" +
            """"context":{"listingId":{"path":"/results/0/listingId"}}}}},""" +
            """{"component":"RecommendationCard","id":"card_2","name":{"path":"/results/1/name"},""" +
            """"price":{"path":"/results/1/price"},"reasons":{"path":"/results/1/reasons"},""" +
            """"meta":{"path":"/results/1/meta"},""" +
            """"action":{"event":{"name":"open_listing",""" +
            """"context":{"listingId":{"path":"/results/1/listingId"}}}}},""" +
            """{"component":"RecommendationCard","id":"card_3","name":{"path":"/results/2/name"},""" +
            """"price":{"path":"/results/2/price"},"reasons":{"path":"/results/2/reasons"},""" +
            """"meta":{"path":"/results/2/meta"},""" +
            """"action":{"event":{"name":"open_listing",""" +
            """"context":{"listingId":{"path":"/results/2/listingId"}}}}}]}}]}"""

    /** [RESULTS] after the model failed: the rules wording, a note saying so, and Try again. */
    val RESULTS_AI_UNAVAILABLE: String =
        RESULTS
            .replace(""""source":"ai_written"""", """"source":"ai_unavailable"""")
            .replace(
                """"name":"rate_reasons","context":$ratingContext""",
                """"name":"retry_ask","context":$answerContext""",
            ).replace(""","rating":{"path":"/feedback/rating"}""", "")
}
