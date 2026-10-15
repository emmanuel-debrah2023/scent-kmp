import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Guarded to match :shared — a consumer cannot declare Apple targets that its
    // dependency does not also publish, so all three modules gate on the same condition.
    if (HostManager.hostIsMac) {
        iosArm64()
        iosSimulatorArm64()
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.scent.project.contracts"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
}

// Writes the catalog JSON Schema generated from the Kotlin types (never hand-edited) to build output.
val generateCatalogSchema by tasks.registering(JavaExec::class) {
    val jvmMain =
        kotlin.targets
            .getByName("jvm")
            .compilations
            .getByName("main")
    val schemaFile = layout.buildDirectory.file("generated/a2ui/scent-v1.catalog.schema.json")
    dependsOn(jvmMain.compileTaskProvider)
    classpath(jvmMain.output.allOutputs, jvmMain.runtimeDependencyFiles)
    mainClass.set("org.scent.project.data.remote.a2ui.schema.WriteCatalogFileKt")
    args(schemaFile.get().asFile.absolutePath)
    outputs.file(schemaFile)
}

tasks.named("assemble") { dependsOn(generateCatalogSchema) }
