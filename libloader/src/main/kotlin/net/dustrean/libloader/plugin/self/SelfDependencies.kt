package net.dustrean.libloader.plugin.self

import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedDependency

/**
 * Converts gradle-api dependencies to own class dependencies, preparing for json conversion
 */
object SelfDependencies {
    class SelfDependency(
        private val depend: ResolvedDependency,
        val groupId: String,
        val artifactId: String,
        val version: String,
        val dependencies: List<SelfDependency>
    ) {
        fun toJson(): String {
            return """
            {   
                "groupId": "$groupId",
                "artifactId": "$artifactId",
                "version": "$version",
                "dependencies": [
                    ${
                dependencies.filter { it.depend.configuration.lowercase().contains("runtime") }
                    .joinToString(",\n") { it.toJson() }
            }
                ]
            }
        """.trimIndent().trim('\n')
        }
    }

    fun getSelfDependencies(configuration: ResolvedConfiguration): List<SelfDependency> {
        fun toDepend(depend: ResolvedDependency): SelfDependency? {
            if (depend.configuration == "shade") {
                return null
            }
            println(depend)
            return SelfDependency(
                depend,
                depend.module.id.group,
                depend.module.id.name,
                depend.module.id.version,
                depend.children.mapNotNull(::toDepend)
            )
        }
        return configuration.firstLevelModuleDependencies.mapNotNull(
            ::toDepend
        )
    }


}