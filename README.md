# keel

A design system for Compose HTML: themeable tokens, primitives, and a lucide icon
set. One visual language across separate apps.

Live: **[bchmsl.github.io/keel](https://bchmsl.github.io/keel)** — every primitive,
in every palette, in light and dark.

Extracted from [Dayboard](https://github.com/dayboard-app/dayboard-app.github.io),
and built to be adopted by [Dakalebi](https://github.com/dakalebi/dakalebi.github.io)
without either one having to look like the other.

## What is in it

| | |
|---|---|
| **Tokens** | Colour, shape, motion, type and shadow, as CSS custom properties. Colours are bare HSL triples, so a rule can add alpha. |
| **Palettes** | Six, each in light and dark. Optional — an app can ship its own and inherit none of these. |
| **Primitives** | Button, IconButton, Card, Dialog, Switch, Slider, TextField, TextAreaField, Pill, PillButton, FormattingField, FormattedText, FormattingToolbar, Icon. |
| **Icons** | 57 lucide glyphs, generated from a pinned tag. |
| **Theme model** | A palette is data, not an enum. Themes declare whether they support light, so a dark-only app is a first-class case. |
| **Text** | An inline-marker parser (`**bold**`, `*italic*`, `__underline__`, `` `code` ``, bare URLs) and its renderer. |

Everything that decides anything is pure Kotlin in `commonMain` and tested. The
browser is only touched in one place, `ThemeController`.

## Using it

Add the module. `keel` is a plain Gradle subproject, so a git submodule plus one
line in `settings.gradle.kts` is the whole setup — no publishing, no tokens.

```bash
git submodule add https://github.com/bchmsl/keel keel
```

```kotlin
// settings.gradle.kts
includeBuild("keel")
```

Or, if you prefer it as a project in your own build:

```kotlin
// settings.gradle.kts
include(":keel")
project(":keel").projectDir = file("keel/keel")
```

Then depend on it, and **copy its stylesheets into your own resources**:

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        jsMain.dependencies {
            implementation(project(":keel"))
        }
    }
}

// Required. A KMP library's jsMain/resources do NOT reach a consumer's
// distribution - see ARCHITECTURE.md, "How the CSS gets there".
tasks.named<Copy>("jsProcessResources") {
    from(rootProject.layout.projectDirectory.dir("keel/keel/src/jsMain/resources"))
}
```

Link the sheets in order, and inline the boot script:

```html
<link rel="stylesheet" href="keel/tokens.css" />
<link rel="stylesheet" href="keel/palettes.css" />
<link rel="stylesheet" href="keel/base.css" />
<link rel="stylesheet" href="keel/components.css" />
```

The boot script paints the stored palette before the bundle loads, so a reload does
not flash the default. `ThemeController.bootScript(defaultTheme = "...")` generates
it, and the [gallery](https://bchmsl.github.io/keel) prints the exact text for the
catalogue in use.

Then:

```kotlin
fun main() {
    val theme = ThemeController(catalog = KeelThemes.Standard)
    theme.start()

    renderComposable(rootElementId = "root") {
        Card(title = "Hello") {
            Button(label = "Press", onClick = { })
        }
    }
}
```

A single dark palette of your own instead of the six:

```kotlin
val Cinema = Theme("cinema", "Cinema", "#e1352f", supportsLight = false)
val theme = ThemeController(catalog = ThemeCatalog(listOf(Cinema), default = Cinema))
```

Link your own palette sheet in place of `palettes.css`. `Theme.supportsLight = false`
makes `ThemeController.isDark` always true and leaves `availableColorModes` with one
entry, which is how a picker knows there is no choice to offer. ARCHITECTURE.md has
the full worked example.

## Building it

Needs a JDK. 21 or newer.

```bash
./gradlew :keel:allTests
```

```bash
./gradlew :keel:koverHtmlReport
```

```bash
./gradlew :gallery:jsBrowserDistribution
```

The gallery lands in `gallery/build/dist/js/productionExecutable`. Serve it with
anything; there is no server side.

```bash
cd gallery/build/dist/js/productionExecutable && python3 -m http.server 8000
```

## Layout

| Path | What is in it |
|---|---|
| `keel/src/commonMain/` | The theme model, the token list, the swatches, the text parser. Pure, no DOM. |
| `keel/src/commonTest/` | Tests for all of it, on the JVM and on Node. |
| `keel/src/jvmTest/` | The invariants that need to read the stylesheets. |
| `keel/src/jsMain/kotlin/` | The components, the icon renderer, `ThemeController`. |
| `keel/src/jsMain/resources/keel/` | The four stylesheets. |
| `gallery/` | The showcase, deployed to Pages. A real consumer, which is what makes it a check. |
| `tools/generate_lucide.py` | Regenerates the icon catalogue. |

`:keel` carries a JVM target purely so Kover can measure it, since Kover cannot
instrument Kotlin/JS.

## Not in it yet

Both consuming apps already hand-roll these, and each is a real candidate. The list
is in rough order of how much duplication it would remove:

- **Segmented control** — written three times today (`.seg`, `.tv-seg`, `.panel__modes`).
- **Scrim** — four of them, four different values, no shared token.
- **Side drawer** — two mirrored copies, one `side` parameter apart.
- **Dropdown menu** and its click-catcher, with a documented z-index band.
- **Toast host** — one app has it, the other has no in-page transient surface at all.
- **Checkbox**, **empty state**, **skeleton loader**, **app bar**, **progress bar**.

See MIGRATION.md for what each one would take.
