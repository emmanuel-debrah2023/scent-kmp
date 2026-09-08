package org.scent.project.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import org.scent.project.data.local.dao.CollectionDao
import org.scent.project.data.local.dao.FollowDao
import org.scent.project.data.local.dao.ListingDao
import org.scent.project.data.local.dao.PostDao
import org.scent.project.data.local.dao.ReviewDao
import org.scent.project.data.local.dao.UserDao
import org.scent.project.data.local.entity.CollectionEntryEntity
import org.scent.project.data.local.entity.FollowEntity
import org.scent.project.data.local.entity.FragranceEntity
import org.scent.project.data.local.entity.FragranceNoteEntity
import org.scent.project.data.local.entity.ListingEntity
import org.scent.project.data.local.entity.PostEntity
import org.scent.project.data.local.entity.PostListingEntity
import org.scent.project.data.local.entity.ReviewEntity
import org.scent.project.data.local.entity.UserEntity

/**
 * Local single source of truth for the Flow-backed read paths (ADR-0001).
 *
 * Entities and DAOs are added per migration phase; each addition needs a schema
 * version bump and a migration once the app ships with a populated database.
 */
@Database(
    entities = [
        PostEntity::class,
        PostListingEntity::class,
        ListingEntity::class,
        FragranceEntity::class,
        FragranceNoteEntity::class,
        CollectionEntryEntity::class,
        ReviewEntity::class,
        UserEntity::class,
        FollowEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
@ConstructedBy(ScentDatabaseConstructor::class)
abstract class ScentDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao

    abstract fun listingDao(): ListingDao

    abstract fun collectionDao(): CollectionDao

    abstract fun reviewDao(): ReviewDao

    abstract fun userDao(): UserDao

    abstract fun followDao(): FollowDao

    companion object {
        const val FILE_NAME: String = "scent.db"
    }
}

/**
 * Room's KSP processor generates the `actual` for each target at compile time.
 */
expect object ScentDatabaseConstructor : RoomDatabaseConstructor<ScentDatabase> {
    override fun initialize(): ScentDatabase
}
