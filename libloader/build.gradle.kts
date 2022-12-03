import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(plugin = "kotlin")
apply(plugin = "java-gradle-plugin")
apply(plugin = "maven-publish")


val implementation by configurations
val compileOnly by configurations
repositories {
    mavenCentral()
}
dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly(gradleApi())
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

(project.extensions.getByName("gradlePlugin") as GradlePluginDevelopmentExtension).apply {
    plugins {
        create("libloader") {
            id = "net.dustrean.libloader"
            implementationClass = "net.dustrean.libloader.plugin.LibraryLoader"
            version = "1.0.0"
        }
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "17"
}

