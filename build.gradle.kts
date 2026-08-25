// A container, not a product. Plugins are resolved here once and applied by the
// modules, so `:keel` and `:gallery` can never drift onto different Kotlin or
// Compose versions - which for a design system and its own showcase would mean the
// gallery no longer proves anything about the library.
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kover) apply false
}

group = "io.github.bchmsl"
version = "0.1.0"

subprojects {
    group = rootProject.group
    version = rootProject.version
}
