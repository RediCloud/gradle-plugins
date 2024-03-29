@file:JvmName("LibraryLoader")

package dev.redicloud.libloader.plugin

import dev.redicloud.libloader.plugin.self.SelfDependencies
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.repositories.UrlArtifactRepository
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.tasks.Jar
import java.util.*

class LibraryLoader : Plugin<Project> {
    class DeclarationMissingException : Exception(
        "You forgot to declare the main class." +
                "\nYou can do that with \"the<LibraryLoaderConfig>().mainClass.set(\"yourclass\")\""
    )

    interface LibraryLoaderConfig {
        /**
         * The path to the main class, when present.
         * When not set, the runtime has to initialize the Bootstrap itself by using:
         * <pre>
         * {@code
         * Bootstrap.apply(...) // Use your own JarLoader
         * }
         * </pre>
         */
        val mainClass: Property<String>

        /**
         * Where to download the jars to.
         * Default: ".libs/"
         */
        val libraryFolder: Property<String>

        /**
         * The name of the configuration to use.
         * Default: "runtimeClasspath"
         */
        val configurationName: Property<String>

        /**
         * The shade configuration, read-only.
         * This will add the shaded dependencies to the jar.
         */
        val shadeConfiguration: Property<Configuration>

        /**
         * The notations to add to the configuration, without using the default DependencyHandler.
         */
        val notationList: ListProperty<String>

        /**
         * The suffix name setter: dependencies-SUFFIX.json and repositories-SUFFIX.json
         * Defaults to the project name.
         */
        val configurationFileSuffix: Property<String>

        /**
         * Useful for dependencies.
         * When set to false, the Bootstrap will be excluded from the jar.
         * Default: true
         */
        val doBootstrapShade: Property<Boolean>
    }

    override fun apply(target: Project) {
        val extension = target.extensions.create("libloader-config", LibraryLoaderConfig::class.java)
        extension.libraryFolder.convention(".libs/")
        extension.configurationName.convention("runtimeClasspath")

        val shade = target.configurations.create("shade")
        target.configurations.getByName("compileClasspath").extendsFrom(shade)
        extension.shadeConfiguration.set(shade)
        extension.shadeConfiguration.finalizeValue()

        extension.configurationFileSuffix.convention("-${target.name}")

        extension.doBootstrapShade.convention(true)

        target.afterEvaluate {
            if ((it.extensions.getByName("libloader-config") as LibraryLoaderConfig).doBootstrapShade.get())
                it.dependencies.add("shade", "dev.redicloud.libloader:libloader-bootstrap:1.6.2")
        }
        @Suppress("RedundantSamConstructor")
        target.tasks.named("jar", Jar::class.java, Action { it ->
            it.from("${target.buildDir}/depends") {
                it.include("**")
                it.into("depends")
            }
            if (extension.doBootstrapShade.get())
                it.from("dev//redicloud//libloader//boot//**")

            it.from({
                shade.map { if (it.isDirectory) it else target.zipTree(it) }
            })
            it.doFirst {
                val configuration =
                    target.configurations.getByName(extension.configurationName.get()).resolvedConfiguration
                // Dependencies File
                val dependsFile =
                    target.buildDir.resolve("depends").also { it.mkdirs() }
                        .resolve("dependencies${extension.configurationFileSuffix.get()}.json")
                dependsFile.writeText(
                    SelfDependencies.getSelfDependencies(configuration)
                        .also { it.addAll(getCustomNotationDependencies(target, extension.notationList.get())) }
                        .joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJson() }
                )
                // Repositories File
                val repoFile =
                    target.buildDir.resolve("depends//repositories${extension.configurationFileSuffix.get()}.json")
                        .also { it.createNewFile() }
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
                    it.attributes["Main-Class"] = "dev.redicloud.libloader.boot.Bootstrap"
                    it.attributes["Premain-Class"] = "dev.redicloud.libloader.boot.Agent"
                    it.attributes["Agent-Class"] = "dev.redicloud.libloader.boot.Agent"
                    it.attributes["Launcher-Agent-Class"] = "dev.redicloud.libloader.boot.Agent"
                }
        })
    }

    private fun getCustomNotationDependencies(
        project: Project,
        notationList: List<String>
    ): Collection<SelfDependencies.SelfDependency> {
        val config = project.configurations.create("Balling")
        notationList.forEach {
            project.dependencies.add("Balling", it)
        }
        val dependencies = config.resolvedConfiguration
        return SelfDependencies.getSelfDependencies(dependencies)
    }
}
