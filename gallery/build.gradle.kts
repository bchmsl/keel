// The showcase, deployed to GitHub Pages.
//
// It exists to be a real consumer. It depends on `:keel` and on nothing else - no
// Firebase, no network, no storage - so anything it cannot build is a fault in the
// library's public API or in how the library delivers its CSS, not in the gallery.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "gallery.js"
            }
            // Nothing here is testable without a browser, and everything worth
            // testing lives in `:keel` and runs on Node and the JVM.
            testTask { enabled = false }
        }

        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":keel"))
        }
    }
}

/*
 * Take keel's stylesheets into this module's own resources.
 *
 * This line is not optional and there is no way to leave it out safely. A Kotlin
 * Multiplatform library's `jsMain/resources` do NOT reach a consumer's distribution:
 * they are not copied into the consumer's `jsProcessResources`, they are not packed
 * into the klib, and they do not appear in `build/dist`. Verified by building this
 * module without this block - the output contained `gallery.css` and no `keel/`
 * directory at all.
 *
 * The failure is quiet, which is the reason for the length of this comment. An
 * unresolved `var()` falls back to the property's initial value rather than
 * erroring, so a page that is missing the tokens renders unstyled instead of
 * failing. Nothing appears in the console and the build stays green.
 *
 * `keel/` is kept as a subdirectory so a consumer's own stylesheet can never be
 * shadowed by one of the library's.
 */
val keelCss = rootProject.layout.projectDirectory.dir("keel/src/jsMain/resources")

tasks.named<Copy>("jsProcessResources") {
    from(keelCss)
}
