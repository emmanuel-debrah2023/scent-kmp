package ui.surface

internal const val ASK_SURFACE_ID = "ask-123e4567-e89b-42d3-a456-426614174000"

private const val CATALOG = "https://scent.app/a2ui/scent/v1"

private val answerContext =
    """{"intent":{"path":"/answers/intent"},"family":{"path":"/answers/family"},""" +
        """"budget":{"path":"/answers/budget"},"size":{"path":"/answers/size"}}"""

/**
 * The two batches the ask route sends, as the server writes them (see `AskSurfaceBuilder`), with the
 * answers filled in as an edit would show them. For previews and tests only; a shared golden fixture
 * between server and client is a follow-up of ADR-0003.
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
            """{"component":"Column","id":"root","children":["heading","family","budget","size","submit"]},""" +
            """{"component":"Text","id":"heading","text":"Three quick picks and we'll find him something",""" +
            """"variant":"h2"},""" +
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
            """"summary":["Woody","up to £80","Full bottle"],""" +
            """"results":[""" +
            """{"name":"Cedar & Smoke","brand":"Atelier Nord","price":"£76",""" +
            """"why":"Dry cedarwood and vetiver: a classic woody gift that stays close to the skin."},""" +
            """{"name":"Birch Ledger","brand":"Atelier Nord","price":"£64",""" +
            """"why":"Smoky birch with a soft musk finish, easy to wear every day."},""" +
            """{"name":"Oud Lantern","brand":"Maison Verre","price":"£79",""" +
            """"why":"Warmer and richer, with oud and amber for evenings out."}]}}},""" +
            """{"version":"v0.9","updateComponents":{"surfaceId":"$ASK_SURFACE_ID","components":[""" +
            """{"component":"Column","id":"root","children":["summary","card_1","card_2","card_3"]},""" +
            """{"component":"PreferenceSummary","id":"summary","items":{"path":"/summary"},""" +
            """"action":{"event":{"name":"edit_answers","context":$answerContext}}},""" +
            """{"component":"RecommendationCard","id":"card_1","name":{"path":"/results/0/name"},""" +
            """"price":{"path":"/results/0/price"},"why":{"path":"/results/0/why"}},""" +
            """{"component":"RecommendationCard","id":"card_2","name":{"path":"/results/1/name"},""" +
            """"price":{"path":"/results/1/price"},"why":{"path":"/results/1/why"}},""" +
            """{"component":"RecommendationCard","id":"card_3","name":{"path":"/results/2/name"},""" +
            """"price":{"path":"/results/2/price"},"why":{"path":"/results/2/why"}}]}}]}"""
}
