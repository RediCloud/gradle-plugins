import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.9.23"
    id("java-gradle-plugin")
    java
    id("maven-publish")
}


allprojects {
    group = "dev.redicloud.libloader"
    version = "1.7.0"
    repositories {
        mavenCentral()
        maven("https://repo.gradle.org/gradle/libs-releases/")
    }
    tasks {
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }

        withType<JavaCompile> {
            options.release.set(8)
            options.encoding = "UTF-8"
        }
    }
}
subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "java")
    repositories {
        mavenCentral()
    }
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = URI("https://maven.pkg.github.com/RediCloud/gradle-plugins")
                credentials {
                    username = findProperty("gpr.user") as String?
                        ?: System.getenv("GITHUB_ACTOR")
                    password = findProperty("gpr.key") as String?
                        ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
    java {
        withSourcesJar()
        withJavadocJar()
    }
}