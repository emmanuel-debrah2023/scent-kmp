import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val projectProperties = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}

fun requireProperty(key: String, buildType: String, fallbackKey: String? = null): String {
    val value = projectProperties.getProperty(key) ?: (fallbackKey?.let { projectProperties.getProperty(it) })
    if (value.isNullOrBlank()) {
        val keyMsg = if (fallbackKey != null) "'$key' or '$fallbackKey'" else "'$key'"
        throw GradleException(
            "Missing or empty required property $keyMsg in .env or local.properties " +
            "(required for the '$buildType' build type)."
        )
    }
    return value
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            
            // Koin DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.androidx.compose)
            
            // Image loading
            implementation(libs.coil.compose)
            implementation(libs.coil.network)
            
            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            
            // Shared module
            implementation(projects.shared)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.turbine)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockk)
        }
    }
}

android {
    namespace = "org.scent.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.scent.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        getByName("debug") {
            buildConfigField("String",  "DB_URL",      "\"${requireProperty("DATABASE_URL",      "debug", "LOCAL_DATABASE_URL")}\"")
            buildConfigField("String",  "DB_USER",     "\"${requireProperty("DATABASE_USER",     "debug", "LOCAL_DATABASE_USER")}\"")
            buildConfigField("String",  "DB_PASSWORD", "\"${requireProperty("DATABASE_PASSWORD", "debug", "LOCAL_DATABASE_PASSWORD")}\"")
            buildConfigField("Boolean", "IS_SUPABASE", "false")
        }
        getByName("release") {
            isMinifyEnabled = false
            buildConfigField("String",  "DB_URL",      "\"${requireProperty("DB_URL",      "release")}\"")
            buildConfigField("String",  "DB_USER",     "\"${requireProperty("DB_USER",     "release")}\"")
            buildConfigField("String",  "DB_PASSWORD", "\"${requireProperty("DB_PASSWORD", "release")}\"")
            buildConfigField("Boolean", "IS_SUPABASE", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
