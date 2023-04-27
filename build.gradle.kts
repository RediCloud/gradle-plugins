import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.7.22"
    id("java-gradle-plugin")
    java
    id("maven-publish")
}


allprojects {
    group = "dev.redicloud.libloader"
    version = "1.6.4"
    repositories {
        mavenCentral()
        maven("https://repo.gradle.org/gradle/libs-releases/")
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
                name = "redicloud"
                url = URI("https://repo.redicloud.dev/releases/")
                credentials(PasswordCredentials::class.java) {
                    username = findProperty("REDI_CLOUD_REPO_USERNAME") as String?
                        ?: System.getenv("REDI_CLOUD_REPO_USERNAME")
                    password = findProperty("REDI_CLOUD_REPO_PASSWORD") as String?
                        ?: System.getenv("REDI_CLOUD_REPO_PASSWORD")
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }


    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
    java {
        withSourcesJar()
        withJavadocJar()
    }
}

