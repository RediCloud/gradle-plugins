# Gradle Plugins

This Project contains all Gradle plugins made for redicloud.

# Table of Contents

1. [Libloader](#libloader)
   1. [Libloader Gradle Plugin](#libloader-gradle-plugin)
      1. [Configuration](#configuration)
      2. [Shading](#shading)
   2. [Libloader Bootstrap](#libloader-bootstrap)
      1. [Standalone Jars](#standalone-jars)
      2. [Non-Standalone Jars (Plugins for Velocity or Spigot)](#non-standalone-jars)

## Libloader

The Library Loader's purpose is to reduce the .jar size.
This is made possible by splitting the process in two.

### Libloader Gradle Plugin

The Gradle plugin configures everything and grabs the dependencies from there.
It works by overriding the main class and inserting agents, which will be able by using the `instrumentation` to insert Jars.
You can add the plugin by using this code:

```kotlin
// build.gradle.kts
plugins {
    id("dev.redicloud.libloader") version ("1.6.3")
}
repositories {
   maven("https://repo.redicloud.dev/releases/")
}
```

##### Configuration

```kotlin
// build.gradle.kts
import dev.redicloud.libloader.plugin.LibraryLoader.LibraryLoaderConfig
//...
the(LibraryLoaderConfig::class).apply {
    this.mainClass.set("dev.redicloud.testing.Main") // Not mandatory when using non-standalone Jars, see chapter Bootloader
    this.libraryFolder.set(".libs/") // Default Value
    this.configurationName.set("runtimeClasspath") // Default Value, configuration to use for dependencies
}
```

##### Shading

```kotlin
// build.gradle.kts
dependencies {
    shade("x.y.z:a:b.c.d") // Instead of dynamically loading, it will be put in the Jar 
}
```

### Libloader Bootstrap

The Bootloader is shaded into the Jar and gets the the dependencies using Gson, JSoup and internal JSONs.

##### Standalone Jars

After initializing, when used in a standalone Jar, the main method given will be executed.
No further action other than adding it to the configuration of the Plugin needed.

##### Non-Standalone Jars

Using the `URLClassLoaderJarLoader` you are able to load dependencies for your plugins.

Example Spigot:

```kotlin
class Plugin: JavaPlugin() {
    override fun onLoad() {
        Bootstrap().apply(URLClassLoaderJarLoader(this::class.java.classLoader as URLClassLoader))
    }
}
```

Example Velocity:

```kotlin
@Plugin(...)
class Plugin @Inject constructor(private val proxy: ProxyServer) : PluginContainer {
    @Subscribe
    fun init(e: ProxyInitializeEvent) {
        Bootstrap().apply(URLClassLoaderJarLoader(this::class.java.classLoader as URLClassLoader))
    }
}
```
