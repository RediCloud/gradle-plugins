import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.7.22"
    id("java-gradle-plugin")
    java
    id("maven-publish")
}


allprojects {
    group = "net.dustrean.libloader"
    version = "1.3.2"
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
                name = "dustrean"
                url = URI("https://repo.dustrean.net/releases/")
                credentials(PasswordCredentials::class.java) {
                    username = System.getenv("DUSTREAN_REPO_USERNAME")
                    password = System.getenv("DUSTREAN_REPO_PASSWORD")
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

