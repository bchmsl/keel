rootProject.name = "keel"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

// `:keel` is the library and the only thing consumers depend on. It is a subproject
// rather than the root so the root can stay a container: a design system that is
// also a Gradle root project cannot be added to a composite build without its
// settings file fighting the host's.
include(":keel")

// `:gallery` is the showcase, deployed to GitHub Pages. It is a real consumer of
// `:keel` and nothing else, which is what makes it a test and not a demo: if the
// library's public API or its CSS delivery breaks, the gallery stops building.
include(":gallery")
