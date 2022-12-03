apply(plugin = "java")
apply(plugin = "maven-publish")

val implementation by configurations
dependencies {
    implementation("com.google.code.gson:gson:2.10")
    implementation("org.jsoup:jsoup:1.15.3")
}

(project.extensions.getByName("publishing") as PublishingExtension).apply {
    publications {
        create<MavenPublication>("bootstrap") {
            this.groupId = "net.dustrean.libloader"
            this.version = "1.0.0"
            this.artifactId = "libloader-bootstrap"
            from(components["java"])
        }
    }
}