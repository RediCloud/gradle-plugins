@file:JvmName("LibraryLoader")

package net.dustrean.libloader.plugin

import net.dustrean.libloader.plugin.self.SelfDependencies
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.repositories.UrlArtifactRepository
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.tasks.Jar
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

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
        val notationList: ListProperty<String>
    }

    override fun apply(target: Project) {
        val extension = target.extensions.create("config", LibraryLoaderConfig::class.java)
        extension.libraryFolder.convention(".libs/")
        extension.configurationName.convention("runtimeClasspath")

        val shade = target.configurations.create("shade")
        target.configurations.getByName("compileClasspath").extendsFrom(shade)
        extension.shadeConfiguration.set(shade)
        extension.shadeConfiguration.finalizeValue()

        target.dependencies.add("shade", "net.dustrean.libloader:libloader-bootstrap:1.1.0")
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
                    SelfDependencies.getSelfDependencies(configuration).also { it.addAll(getCustomNotationDependencies(target, extension.notationList.get())) }
                        .joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJson() }
                )
                // Repositories File
                val repoFile =
                    target.buildDir.resolve("depends//repositories.json").also { it.createNewFile() }
                repoFile.writeText(
                    target.repositories.filterNot { (it as UrlArtifactRepository).url.toString().startsWith("file:/") }
                        .joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") {
                            "\"${(it as UrlArtifactRepository).url}\""
                        }
                )
                // Configuration
                val configFile =
                    target.buildDir.resolve("depends//config.json").also { it.createNewFile() }
                configFile.writeText(
                    """
                    {
                        ${if (extension.mainClass.isPresent) """"mainClass": "${extension.mainClass.get()}",""" else ""}
                        "libraryFolder": "${extension.libraryFolder.get()}"
                    }
                    """.trimIndent(),
                )
            }
            if (extension.mainClass.isPresent)
                it.manifest {
                    it.attributes["Main-Class"] = "net.dustrean.libloader.boot.Bootstrap"
                    it.attributes["Premain-Class"] = "net.dustrean.libloader.boot.Agent"
                    it.attributes["Agent-Class"] = "net.dustrean.libloader.boot.Agent"
                    it.attributes["Launcher-Agent-Class"] = "net.dustrean.libloader.boot.Agent"
                }
        })
    }

    private fun getCustomNotationDependencies(project: Project, notationList: List<String>): Collection<SelfDependencies.SelfDependency> {
        val config = project.configurations.create("Balling")
        notationList.forEach {
            project.dependencies.add("Balling", it)
        }
        val dependencies = config.resolvedConfiguration
        return SelfDependencies.getSelfDependencies(dependencies)
    }
}