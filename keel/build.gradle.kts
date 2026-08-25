// The library. This is the only module a consumer depends on.
//
// Deliberately not an application: no `binaries.executable()`, no webpack, no
// `moduleKind`. It produces klibs and lets the consuming app decide the output
// shape - which matters, because the two consumers disagree about it (one needs
// CommonJS for the Firebase SDK's `@JsModule` externals, the other does not).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.kover)
    `maven-publish`
}

kotlin {
    explicitApi()

    js(IR) {
        // The browser sub-target exists so a consumer's webpack build can link this
        // klib. Nothing here needs a browser: the tokens, the theme model and the
        // text parser are pure, and the components are verified by the gallery.
        browser {
            testTask { enabled = false }
        }
        nodejs()
    }

    // For coverage, not for a JVM product. Kover cannot instrument Kotlin/JS, so
    // without a JVM target the pure logic in `commonMain` could not be measured at
    // all. It costs a consumer nothing: a web app links the JS klib and never sees
    // this one.
    jvm()

    sourceSets {
        commonMain.dependencies {
            // `api`, not `implementation`: Composable functions and Compose state
            // types appear in this library's own signatures, so consumers need the
            // runtime on their compile classpath.
            api(libs.compose.runtime)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jsMain.dependencies {
            // Also `api`. Every component here takes or returns Compose HTML types
            // (`AttrsScope`, `ContentBuilder`, `HTMLDivElement`), so a consumer that
            // only had this transitively could not name the arguments it passes.
            api(libs.compose.html.core)
        }
    }
}

/*
 * The stylesheet is data to the JVM tests, which is how the Kotlin half and the CSS
 * half are held against each other. Passed in rather than found by a relative path,
 * so the tests do not silently pass by reading nothing if a working directory moves.
 */
tasks.withType<Test>().configureEach {
    systemProperty(
        "keel.css.dir",
        layout.projectDirectory.dir("src/jsMain/resources/keel").asFile.absolutePath,
    )
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/bchmsl/keel")
            credentials {
                // Set by the publish workflow. Absent locally, which is fine:
                // `publishToMavenLocal` does not read this repository at all.
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
