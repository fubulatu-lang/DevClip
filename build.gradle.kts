// Versions live here rather than in a version catalog: this is a single-module
// app with a short dependency list, and a catalog would be a second file to
// keep in step for no benefit yet.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // Kotlin 2.0 moved the Compose compiler out of the Kotlin plugin and into
    // its own, versioned with Kotlin rather than separately.
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
