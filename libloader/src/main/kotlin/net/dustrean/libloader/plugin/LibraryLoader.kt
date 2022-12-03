@file:JvmName("LibraryLoader")

package net.dustrean.libloader.plugin

import net.dustrean.libloader.plugin.self.SelfDependencies
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.repositories.UrlArtifactRepository
import org.gradle.api.internal.artifacts.repositories.ArtifactRepositoryInternal
import org.gradle.api.provider.Property
import org.gradle.jvm.tasks.Jar

class LibraryLoader : Plugin<Project> {
    class DeclarationMissingException : Exception(
        "You forgot to declare the main class." +
                "\nYou can do that with \"the<LibraryLoaderConfig>().mainClass.set(\"yourclass\")\""
    )

    interface LibraryLoaderConfig {
        val mainClass: Property<String>
        val libraryFolder: Property<String>
        val configurationName: Property<String>
        val shadeConfiguration: Property<Configuration>
    }

    override fun apply(target: Project) {
        val extension = target.extensions.create("config", LibraryLoaderConfig::class.java)
        extension.libraryFolder.convention(".libs/")
        extension.configurationName.convention("runtimeClasspath")

        val shade = target.configurations.create("shade")
        extension.shadeConfiguration.set(shade)
        extension.shadeConfiguration.finalizeValue()

        target.dependencies.add("shade", "net.dustrean.libloader:libloader-bootstrap:1.0.0")
        @Suppress("RedundantSamConstructor")
        target.tasks.named("jar", Action { it ->
            it as Jar
            it.from("${target.buildDir}/depends") {
                it.include("**")
            }
            it.from("net//dustrean//libloader//boot//**")
            it.from({
                shade.map { if (it.isDirectory) it else target.zipTree(it) }
            })
            it.doFirst {
                val configuration =
                    target.configurations.getByName(extension.configurationName.get()).resolvedConfiguration
                // Dependencies File
                val dependsFile =
                    target.buildDir.resolve("depends").also { it.mkdirs() }.resolve("dependencies.json")
                dependsFile.writeText(
                    SelfDependencies.getSelfDependencies(configuration)
                        .joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJson() }
                )
                // Repositories File
                val repoFile =
                    target.buildDir.resolve("depends//repositories.json").also { it.createNewFile() }
                repoFile.writeText(
                    target.repositories.filterNot { (it as UrlArtifactRepository).url.toString().startsWith("file:/") }.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") {
                        "\"${(it as UrlArtifactRepository).url}\""
                    }
                )
                // Configuration
                val configFile =
                    target.buildDir.resolve("depends//config.json").also { it.createNewFile() }
                configFile.writeText(
                    """
                    {
                        "mainClass": "${if (extension.mainClass.isPresent) extension.mainClass.get() else throw DeclarationMissingException()}",
                        "libraryFolder": "${extension.libraryFolder.get()}"
                    }
                    """.trimIndent(),
                )

            }
            it.manifest {
                it.attributes["Main-Class"] = "net.dustrean.libloader.boot.Bootstrap"
                it.attributes["Premain-Class"] = "net.dustrean.libloader.boot.Agent"
                it.attributes["Agent-Class"] = "net.dustrean.libloader.boot.Agent"
                it.attributes["Launcher-Agent-Class"] = "net.dustrean.libloader.boot.Agent"
            }
        })
    }
}