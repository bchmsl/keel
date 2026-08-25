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

version = "0.1.0"

// `group` is set on the subprojects and deliberately NOT on the root.
//
// The root is an empty container, and giving it a group would publish a coordinate
// for it too. In a composite build that is fatal rather than untidy: the root is
// named `keel` and so is the library subproject, so both would answer to
// `io.github.bchmsl:keel` and a consumer's `includeBuild("keel")` would fail with
// "Module version 'io.github.bchmsl:keel' is not unique in composite: can be
// provided by [project ':keel', project ':keel:keel']". Leaving the root without a
// group is what makes the coordinate unambiguous, and composite inclusion the
// simplest way to consume this.
subprojects {
    group = "io.github.bchmsl"
    version = rootProject.version
}
