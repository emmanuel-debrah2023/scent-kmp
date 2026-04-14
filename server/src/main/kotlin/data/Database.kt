package data

import data.schema.*
import io.github.cdimascio.dotenv.dotenv
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun initDatabase() {
    val dotenv = dotenv {
        ignoreIfMissing = true
    }

    // Following the application.conf structure suggested by user
    val dbUrl = System.getenv("DATABASE_URL") ?: dotenv["DATABASE_URL"] ?: "jdbc:postgresql://localhost:5432/scent"
    val dbUser = System.getenv("DATABASE_USER") ?: dotenv["DATABASE_USER"] ?: "postgres"
    val dbPassword = System.getenv("DATABASE_PASSWORD") ?: dotenv["DATABASE_PASSWORD"] ?: "postgres"

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
