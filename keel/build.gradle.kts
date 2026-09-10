import org.jetbrains.kotlin.gradle.dsl.JsModuleKind

// The library. This is the only module a consumer depends on.
//
// Deliberately not an application: no `binaries.executable()` and no webpack. It
// produces klibs, and the consuming app builds the bundle.
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
        // Required, not a preference. Under the UMD default the compiler refuses a
        // `@JsModule` external unless it also declares a `@JsNonModule` global to
        // fall back to, and ProseMirror publishes no global build, so that fallback
        // cannot honestly be written. The formatting field's editor is behind seven
        // such externals.
        //
        // It constrains this compilation rather than a consumer's bundle, which is
        // still built from the klibs - and in any case both consumers already set
        // CommonJS for the Firebase SDK's own `@JsModule` externals, so the builds
        // this changes are this one and the gallery's.
        compilerOptions {
            moduleKind.set(JsModuleKind.MODULE_COMMONJS)
        }

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

            // keel's only npm dependencies, and they belong to exactly one
            // component: the editing engine behind `FormattingField`. Declared here
            // rather than asked of each consumer, which was measured to work through
            // `includeBuild` as well as through a published artifact - Kotlin writes
            // them into the imported package's own `package.json`.
            //
            // `implementation`, so they stay out of keel's API: nothing public names
            // a ProseMirror type, which is what lets Kotlin's dead-code pass drop all
            // seven from a consumer that never opens one of these fields. Measured at
            // zero bytes and zero `prosemirror` strings in Dayboard's bundle.
            //
            // One cost for a consumer, and it is not optional: a new npm dependency
            // changes the resolved yarn lock, and a Kotlin/JS build *fails* at
            // `kotlinStoreYarnLock` until the repo's own `kotlin-js-store` is
            // regenerated with `./gradlew kotlinUpgradeYarnLock` and committed.
            implementation(npm("prosemirror-model", "1.25.11"))
            implementation(npm("prosemirror-state", "1.4.4"))
            implementation(npm("prosemirror-view", "1.42.3"))
            implementation(npm("prosemirror-inputrules", "1.5.1"))
            implementation(npm("prosemirror-history", "1.5.0"))
            implementation(npm("prosemirror-keymap", "1.2.3"))
            implementation(npm("prosemirror-commands", "1.7.2"))
        }
    }
}

/*
 * The stylesheet is data to the JVM tests, which is how the Kotlin half and the CSS
 * half are held against each other. Passed in rather than found by a relative path,
 * so the tests do not silently pass by reading nothing if a working directory moves.
 *
 * The Kotlin sources are data for the same reason. A JVM test cannot call into the JS
 * target, so the one test that checks Kotlin's class names against the CSS reads them
 * as text instead. That is a narrower guarantee than calling the functions, and it is
 * the guarantee that catches the bug: a class name is a string on both sides.
 */
tasks.withType<Test>().configureEach {
    val cssDir = layout.projectDirectory.dir("src/jsMain/resources/keel")
    val jsSrcDir = layout.projectDirectory.dir("src/jsMain/kotlin")

    systemProperty("keel.css.dir", cssDir.asFile.absolutePath)
    systemProperty("keel.js.src.dir", jsSrcDir.asFile.absolutePath)

    /*
     * Declared as inputs as well as passed as properties, and the tests are wrong
     * without it. `systemProperty` tells the test where the files are; it tells
     * Gradle nothing about them. So on a CSS-only change - which is the exact change
     * this suite exists to catch - nothing in the task's fingerprint moved and the
     * whole thing stayed UP-TO-DATE, reporting a pass it had not re-checked.
     *
     * Found the honest way: a literal `height` added to components.css to prove the
     * new size guard could fail did not fail until `--rerun-tasks`.
     *
     * The Kotlin directory needs the same treatment for the same reason. Changing a
     * class-name literal in jsMain does not recompile anything the JVM test depends
     * on, so without this the class-name check would go stale in exactly the
     * situation it exists for.
     */
    inputs.dir(cssDir)
        .withPropertyName("keelStylesheets")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    inputs.dir(jsSrcDir)
        .withPropertyName("keelJsSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
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
