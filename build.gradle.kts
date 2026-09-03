plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.hotswan.compiler) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
            // The patterns above are matched relative to each source-set root, so they
            // miss KSP/Room output under build/generated that the KMP plugin registers
            // as a source directory. Match on the absolute path instead.
            exclude { it.file.absolutePath.contains("${File.separator}build${File.separator}") }
        }
    }

    dependencies {
        "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml", "config/detekt/detekt-idioms.yml"))
        buildUponDefaultConfig = true
        ignoredBuildTypes = listOf("release")
    }

    // Give each source-set-specific detekt task (created by the KMP plugin) its own
    // per-module baseline so that runs don't overwrite each other.
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        config.setFrom(rootProject.files("config/detekt/detekt.yml", "config/detekt/detekt-idioms.yml"))
        buildUponDefaultConfig = true
        baseline.set(project.file("config/detekt/baseline-${name}.xml"))
    }
    tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        config.setFrom(rootProject.files("config/detekt/detekt.yml", "config/detekt/detekt-idioms.yml"))
        buildUponDefaultConfig = true
        // Map baseline task name (e.g. detektBaselineMetadataCommonMain) to the matching
        // check task name (e.g. detektMetadataCommonMain) so both tasks share one baseline file.
        val checkTaskName = name.replace("detektBaseline", "detekt")
        baseline.set(project.file("config/detekt/baseline-${checkTaskName}.xml"))
    }
}
