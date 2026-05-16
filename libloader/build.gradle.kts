import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(plugin = "kotlin")
apply(plugin = "java-gradle-plugin")
apply(plugin = "maven-publish")

val generateVersionFile by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/version")
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("dev/redicloud/libloader/plugin")
        dir.mkdirs()
        dir.resolve("PluginVersion.kt").writeText(
            """
            package dev.redicloud.libloader.plugin

            internal const val PLUGIN_VERSION = "${project.version}"
            """.trimIndent()
        )
    }
}

sourceSets["main"].java.srcDir(tasks.named("generateVersionFile").map {
    project.layout.buildDirectory.dir("generated/version").get()
})

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateVersionFile)
}

val implementation by configurations
val compileOnly by configurations
repositories {
    mavenCentral()
}
dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly(gradleApi())
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

(project.extensions.getByName("gradlePlugin") as GradlePluginDevelopmentExtension).apply {
    plugins {
        create("libloader") {
            id = "dev.redicloud.libloader"
            implementationClass = "dev.redicloud.libloader.plugin.LibraryLoader"
            version = "${project.version}"
        }
    }
}