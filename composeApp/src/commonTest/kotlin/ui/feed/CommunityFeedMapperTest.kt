package ui.feed

import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.Post
import kotlin.test.Test
import kotlin.test.assertEquals

class CommunityFeedMapperTest {
    private val now = 1_000_000_000_000L
    private val second = 1_000L
    private val minute = 60 * second
    private val hour = 60 * minute
    private val day = 24 * hour

    private fun post(
        createdAt: Long,
        username: String = "amara",
        userId: String = "42",
    ) = Post(
        id = "post_1",
        userId = userId,
        username = username,
        contentFormat = ContentFormat.TEXT,
        fragranceIds = emptyList(),
        createdAt = createdAt,
    )

    private fun itemFor(ageMillis: Long): CommunityFeedItem =
        listOf(post(createdAt = now - ageMillis)).toCommunityFeedItems(now).first()

    private fun assertTimeAgo(
        ageMillis: Long,
        compact: String,
        description: String,
    ) {
        val item = itemFor(ageMillis)
        assertEquals(compact, item.timeAgo)
        assertEquals("amara, $description", item.headerDescription)
    }

    @Test
    fun `username comes from the post username and not the user id`() {
        val item = listOf(post(createdAt = now, username = "amara", userId = "42")).toCommunityFeedItems(now).first()
        assertEquals("amara", item.username)
    }

    @Test
    fun `a blank username leaves only the time in the header description`() {
        val item = listOf(post(createdAt = now - 2 * hour, username = "")).toCommunityFeedItems(now).first()
        assertEquals("2 hours ago", item.headerDescription)
    }

    @Test
    fun `thirty seconds ago reads as now`() = assertTimeAgo(30 * second, "now", "just now")

    @Test
    fun `a post dated in the future reads as now`() = assertTimeAgo(-5 * minute, "now", "just now")

    @Test
    fun `one minute ago is singular`() = assertTimeAgo(minute, "1m", "1 minute ago")

    @Test
    fun `fifty nine minutes stays in minutes`() = assertTimeAgo(59 * minute, "59m", "59 minutes ago")

    @Test
    fun `two hours ago is plural`() = assertTimeAgo(2 * hour, "2h", "2 hours ago")

    @Test
    fun `twenty three hours stays in hours`() = assertTimeAgo(23 * hour, "23h", "23 hours ago")

    @Test
    fun `one day ago is singular`() = assertTimeAgo(day, "1d", "1 day ago")

    @Test
    fun `three days ago is plural`() = assertTimeAgo(3 * day, "3d", "3 days ago")

    @Test
    fun `an empty list still returns the mock items`() {
        val items = emptyList<Post>().toCommunityFeedItems(now)
        assertEquals(5, items.size)
        assertEquals("mock_1", items.first().id)
    }
}
