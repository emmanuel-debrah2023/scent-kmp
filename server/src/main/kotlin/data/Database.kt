package data

import data.schema.DecantsTable
import data.schema.FollowsTable
import data.schema.FragranceMediaTable
import data.schema.FragranceNotesTable
import data.schema.FragrancesTable
import data.schema.MediaItemsTable
import data.schema.MediaLikesTable
import data.schema.OrdersTable
import data.schema.ReviewsTable
import data.schema.UserFragranceCollectionTable
import data.schema.UsersTable
import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun initDatabase() {
    val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val dbUrl = System.getenv("DB_URL") ?: dotenv["DB_URL"] ?: "DB_URL"
    val dbUser = System.getenv("DB_USER") ?: dotenv["DB_USER"] ?: "DB_USER"
    val dbPassword = System.getenv("DB_PASSWORD") ?: dotenv["DB_PASSWORD"] ?: "DB_PASSWORD"

    Database.connect(
        url = dbUrl,
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword
    )

    transaction {
        SchemaUtils.create(
            UsersTable,
            FragrancesTable,
            DecantsTable,
            UserFragranceCollectionTable,
            MediaItemsTable,
            FragranceNotesTable,
            OrdersTable,
            ReviewsTable,
            FollowsTable,
            MediaLikesTable,
            FragranceMediaTable
        )
    }
}