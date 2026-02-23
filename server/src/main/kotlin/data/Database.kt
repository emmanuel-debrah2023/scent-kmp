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
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun initDatabase() {
    val dbUrl = System.getenv("DB_URL")
    val dbUser = System.getenv("DB_USER")
    val dbPassword = System.getenv("DB_PASSWORD")

    Database.connect(
        url = dbUrl ?: "jdbc:postgresql://db.localdburl",
        driver = "org.postgresql.Driver",
        user = dbUser ?: "postgres",
        password = dbPassword ?: "localdbpassword"
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