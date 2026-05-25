plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

java {
    withSourcesJar()
    withJavadocJar()
}

val generateVersionFile by tasks.registering {
    description = "Generates PluginVersion.kt with the current project version"
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

dependencies {
    implementation(libs.kotlin.stdlib)
    compileOnly(gradleApi())
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

gradlePlugin {
    plugins {
        create("libloader") {
            id = "dev.redicloud.libloader"
            implementationClass = "dev.redicloud.libloader.plugin.LibraryLoader"
            version = "${project.version}"
        }
    }
}