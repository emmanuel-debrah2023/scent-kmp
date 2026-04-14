import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.file.DuplicatesStrategy

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "org.scent.project"
version = "1.0.0"
application {
    mainClass.set("org.scent.project.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json.server)
    
    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.postgresql)
    implementation(libs.dotenv.kotlin)
    
    // Auth Utils
    implementation(libs.jbcrypt)
    implementation(libs.java.jwt)
    implementation(libs.google.auth)
    implementation(libs.google.api.client)
    implementation(libs.google.api.client.gson)
    implementation(libs.jwks.rsa)
    
    // Utils
    implementation(libs.kotlinx.datetime)
    
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.testJunit)
}

tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
