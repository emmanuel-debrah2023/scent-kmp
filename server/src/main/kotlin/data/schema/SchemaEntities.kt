package data.schema

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UserEntity(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<UserEntity>(UsersTable)

    var username by UsersTable.username
    var email by UsersTable.email
    var displayName by UsersTable.displayName
    var avatarUrl by UsersTable.avatarUrl
    var bio by UsersTable.bio
    var isSeller by UsersTable.isSeller
    var followerCount by UsersTable.followerCount
    var createdAt by UsersTable.createdAt

    // Fragrances this user is SELLING
    val listings by FragranceEntity referrersOn FragrancesTable.sellerId

    // Decants this user is SELLING
    val decantListings by DecantEntity referrersOn DecantsTable.sellerId

    // Fragrances in this user's COLLECTION
    val collection by FragranceEntity via UserFragranceCollectionTable
}

class FragranceEntity(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<FragranceEntity>(FragrancesTable)

    var seller by UserEntity referencedOn FragrancesTable.sellerId
    var name by FragrancesTable.name
    var brand by FragrancesTable.brand
    var description by FragrancesTable.description
    var price by FragrancesTable.price
    var volume by FragrancesTable.volume
    var concentration by FragrancesTable.concentration
    var condition by FragrancesTable.condition
    var stockQuantity by FragrancesTable.stockQuantity
    var isActive by FragrancesTable.isActive
    var viewCount by FragrancesTable.viewCount
    var createdAt by FragrancesTable.createdAt

    // Decants split from this fragrance
    val decants by DecantEntity referrersOn DecantsTable.fragranceId

    // Users who have collected this fragrance
    val collectors by UserEntity via UserFragranceCollectionTable

    // Media items associated with this fragrance
    val media by MediaItemEntity via FragranceMediaTable

    // Reviews of this fragrance
    val reviews by ReviewEntity referrersOn ReviewsTable.fragranceId
}

class DecantEntity(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<DecantEntity>(DecantsTable)

    var fragrance by FragranceEntity referencedOn DecantsTable.fragranceId
    var seller by UserEntity referencedOn DecantsTable.sellerId
    var size by DecantsTable.size
    var unit by DecantsTable.unit
    var price by DecantsTable.price
    var stockQuantity by DecantsTable.stockQuantity
    var description by DecantsTable.description
    var isActive by DecantsTable.isActive
    var createdAt by DecantsTable.createdAt
}

class MediaItemEntity(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<MediaItemEntity>(MediaItemsTable)

    var uploader by UserEntity referencedOn MediaItemsTable.uploaderId
    var type by MediaItemsTable.type
    var url by MediaItemsTable.url
    var thumbnailUrl by MediaItemsTable.thumbnailUrl
    var duration by MediaItemsTable.duration
    var caption by MediaItemsTable.caption
    var likeCount by MediaItemsTable.likeCount
    var commentCount by MediaItemsTable.commentCount
    var isReview by MediaItemsTable.isReview
    var createdAt by MediaItemsTable.createdAt

    // Fragrances associated with this media item
    val fragrances by FragranceEntity via FragranceMediaTable
}

class OrderEntity(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<OrderEntity>(OrdersTable)

    var buyer by UserEntity referencedOn OrdersTable.buyerId
    var seller by UserEntity referencedOn OrdersTable.sellerId
    var fragrance by FragranceEntity optionalReferencedOn OrdersTable.fragranceId
    var decant by DecantEntity optionalReferencedOn OrdersTable.decantId
    var quantity by OrdersTable.quantity
    var totalAmount by OrdersTable.totalAmount
    var status by OrdersTable.status
    var shippingAddress by OrdersTable.shippingAddress
    var createdAt by OrdersTable.createdAt
    var updatedAt by OrdersTable.updatedAt
}

class ReviewEntity(
    id: EntityID<Int>,
) : IntEntity(id) {
    companion object : IntEntityClass<ReviewEntity>(ReviewsTable)

    var reviewer by UserEntity referencedOn ReviewsTable.reviewerId
    var fragrance by FragranceEntity referencedOn ReviewsTable.fragranceId
    var order by OrderEntity optionalReferencedOn ReviewsTable.orderId
    var rating by ReviewsTable.rating
    var content by ReviewsTable.content
    var mediaItem by MediaItemEntity optionalReferencedOn ReviewsTable.mediaItemId
    var createdAt by ReviewsTable.createdAt
}
