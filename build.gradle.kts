plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "dev.redicloud.libloader"
    version = "1.8.0"
    repositories {
        mavenCentral()
        maven("https://repo.gradle.org/gradle/libs-releases/")
    }
}

subprojects {
    pluginManager.withPlugin("maven-publish") {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/RediCloud/gradle-plugins")
                    credentials {
                        username = findProperty("gpr.user") as String?
                            ?: System.getenv("GITHUB_ACTOR")
                        password = findProperty("gpr.key") as String?
                            ?: System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
    }
}
