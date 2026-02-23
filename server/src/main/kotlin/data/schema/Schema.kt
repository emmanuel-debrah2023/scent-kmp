package data.schema

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.datetime

// ─────────────────────────────────────────────
// ENUMS
// ─────────────────────────────────────────────

enum class FragranceCondition {
    NEW,
    USED,
    DECANT,
    SAMPLE
}

enum class Concentration {
    EAU_DE_COLOGNE,   // EDC   ~2-4%
    EAU_DE_TOILETTE,  // EDT   ~5-15%
    EAU_DE_PARFUM,    // EDP   ~15-20%
    PARFUM,           // Extrait ~20-30%
    EAU_SOLID,        // Solid perfume
    MIST              // Body mist ~1-3%
}

enum class CollectionStatus {
    OWNS,
    WISHLIST,
    TRIED,
    DESTASHED
}

enum class MediaType {
    VIDEO,
    IMAGE
}

enum class NoteType {
    TOP,
    MIDDLE,
    BASE
}

enum class OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}

enum class DecantUnit {
    ML,
    SPRAY
}

// ─────────────────────────────────────────────
// TABLES
// ─────────────────────────────────────────────

object UsersTable : IntIdTable("users") {
    val username        = varchar("username", 50).uniqueIndex()
    val email           = varchar("email", 100).uniqueIndex()
    val passwordHash    = varchar("password_hash", 255)
    val displayName     = varchar("display_name", 100)
    val avatarUrl       = varchar("avatar_url", 255).nullable()
    val bio             = text("bio").nullable()
    val isSeller        = bool("is_seller").default(false)
    val followerCount   = integer("follower_count").default(0)
    val createdAt       = datetime("created_at")
}

object FragrancesTable : IntIdTable("fragrances") {
    val sellerId        = reference("seller_id", UsersTable)
    val name            = varchar("name", 150)
    val brand           = varchar("brand", 100)
    val description     = text("description").nullable()
    val price           = decimal("price", 10, 2)
    val volume          = integer("volume_ml").nullable()
    val concentration   = enumerationByName("concentration", 30, Concentration::class).nullable()
    val condition       = enumerationByName("condition", 20, FragranceCondition::class)
    val stockQuantity   = integer("stock_quantity").default(1)
    val isActive        = bool("is_active").default(true)
    val viewCount       = integer("view_count").default(0)
    val createdAt       = datetime("created_at")
}

// Sellers can split a bottle into smaller decant listings
object DecantsTable : IntIdTable("decants") {
    val fragranceId     = reference("fragrance_id", FragrancesTable)  // parent fragrance listing
    val sellerId        = reference("seller_id", UsersTable)
    val size            = decimal("size", 6, 2)                  // e.g. 5.0, 10.0
    val unit            = enumerationByName("unit", 10, DecantUnit::class).default(DecantUnit.ML)
    val price           = decimal("price", 10, 2)
    val stockQuantity   = integer("stock_quantity").default(1)
    val description     = text("description").nullable()         // e.g. "sprayed ~5 times"
    val isActive        = bool("is_active").default(true)
    val createdAt       = datetime("created_at")
}

// Many-to-many: users collecting fragrances
object UserFragranceCollectionTable : Table("user_fragrance_collection") {
    val userId          = reference("user_id", UsersTable)
    val fragranceId     = reference("fragrance_id", FragrancesTable)
    val addedAt         = datetime("added_at")
    val personalNotes   = text("personal_notes").nullable()
    val bottleSizeMl    = integer("bottle_size_ml").nullable()
    val status          = enumerationByName("status", 20, CollectionStatus::class)
    override val primaryKey = PrimaryKey(userId, fragranceId)
}

object MediaItemsTable : IntIdTable("media_items") {
    val uploaderId      = reference("uploader_id", UsersTable)
    val type            = enumerationByName("type", 10, MediaType::class)
    val url             = varchar("url", 500)
    val thumbnailUrl    = varchar("thumbnail_url", 500).nullable()
    val duration        = integer("duration_seconds").nullable()
    val caption         = text("caption").nullable()
    val likeCount       = integer("like_count").default(0)
    val commentCount    = integer("comment_count").default(0)
    val isReview        = bool("is_review").default(false)
    val createdAt       = datetime("created_at")
}

object FragranceMediaTable : Table("fragrance_media") {
    val fragranceId = reference("fragrance_id", FragrancesTable)
    val mediaItemId = reference("media_item_id", MediaItemsTable)
    override val primaryKey = PrimaryKey(fragranceId, mediaItemId)
}

object FragranceNotesTable : Table("fragrance_notes") {
    val fragranceId     = reference("fragrance_id", FragrancesTable)
    val note            = varchar("note", 100)
    val noteType        = enumerationByName("note_type", 10, NoteType::class)
}

object OrdersTable : IntIdTable("orders") {
    val buyerId         = reference("buyer_id", UsersTable)
    val sellerId        = reference("seller_id", UsersTable)
    val fragranceId     = reference("fragrance_id", FragrancesTable).nullable()
    val decantId        = reference("decant_id", DecantsTable).nullable() // either fragrance OR decant
    val quantity        = integer("quantity").default(1)
    val totalAmount     = decimal("total_amount", 10, 2)
    val status          = enumerationByName("status", 20, OrderStatus::class)
    val shippingAddress = text("shipping_address")
    val createdAt       = datetime("created_at")
    val updatedAt       = datetime("updated_at")
}

object ReviewsTable : IntIdTable("reviews") {
    val reviewerId      = reference("reviewer_id", UsersTable)
    val fragranceId     = reference("fragrance_id", FragrancesTable)
    val orderId         = reference("order_id", OrdersTable).nullable()
    val rating          = integer("rating") // 1–5
    val content         = text("content").nullable()
    val mediaItemId     = reference("media_item_id", MediaItemsTable).nullable() // video review
    val createdAt       = datetime("created_at")
}

object FollowsTable : Table("follows") {
    val followerId      = reference("follower_id", UsersTable)
    val followingId     = reference("following_id", UsersTable)
    val createdAt       = datetime("created_at")
    override val primaryKey = PrimaryKey(followerId, followingId)
}

object MediaLikesTable : Table("media_likes") {
    val userId          = reference("user_id", UsersTable)
    val mediaItemId     = reference("media_item_id", MediaItemsTable)
    val createdAt       = datetime("created_at")
    override val primaryKey = PrimaryKey(userId, mediaItemId)
}
