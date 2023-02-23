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
    version = "1.6.2"
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
                /*
                credentials(PasswordCredentials::class.java) {
                    username = System.getenv("REDI_CLOUD_REPO_USERNAME")
                    password = System.getenv("REDI_CLOUD_REPO_PASSWORD")
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
                 */
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

