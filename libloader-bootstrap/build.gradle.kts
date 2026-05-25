plugins {
    java
    `maven-publish`
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
    options.encoding = "UTF-8"
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(libs.gson)
    implementation(libs.jsoup)
}

publishing {
    publications {
        create<MavenPublication>("bootstrap") {
            groupId = "${project.group}"
            version = "${project.version}"
            artifactId = project.name
            from(components["java"])
        }
    }
}