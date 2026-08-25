# How keel is put together

## The one rule

**A component names a token. It never names a value.**

That is the whole of it. Every colour, radius, duration, font and shadow a component
reads is a CSS custom property, and the only place a literal appears is a token
definition. It is what lets one stylesheet serve a light six-palette dashboard and a
single-palette dark video app without either editing the other's rules.

`TokenContractTest` enforces it: a literal colour anywhere outside the token files
fails the build. There is exactly one allowance, black at low alpha, for the modal
scrim and the shadow values — a scrim dims whatever is behind it and a shadow is the
absence of light, so neither is a palette's business.

## The four stylesheets

Link them in this order. Nothing enforces it, and the failure is quiet: an unresolved
`var()` falls back to the property's initial value, so a page linked in the wrong
order renders unstyled rather than erroring.

| | |
|---|---|
| `tokens.css` | The contract. Shape, motion, type, shadow, and the colours that do not change with a palette. **Declares no palette**, so on its own it paints nothing. |
| `palettes.css` | The six shipped palettes, light and dark. **Optional.** |
| `base.css` | The reset, the document surface, the animations, the scrollbars. Reads tokens, defines none. |
| `components.css` | The primitives. Reads tokens, defines none. |

### Why the palettes are a separate file

A palette is the one part of a design system an app should own outright, and the
split is what makes that true rather than merely stated.

`palettes.css` declares coral on bare `:root` as well as on `[data-theme='coral']`,
so an unknown or missing `data-theme` still paints something complete. That fallback
is right for an app using the shipped palettes and actively wrong for an app with its
own: a video app on true black would find itself re-derived as somebody else's
near-black, and would have to override eleven palettes it will never paint in order
to escape one it never asked for.

So an app with its own palettes links `tokens.css` and its own sheet, and inherits
nothing from `palettes.css`.

## The token contract

Colours are **bare HSL triples** (`H S% L%`), not finished colours:

```css
--primary: 350 91% 60%;
```

```css
background-color: hsl(var(--primary));
background-color: hsl(var(--primary) / 0.9);   /* the point of the format */
```

A hex token cannot take alpha that way.

Converting an existing hex palette needs one decimal place. Measured on Dakalebi's
eleven colours: at one decimal all eleven round-trip to the same hex exactly, and at
zero decimals eight of them drift — by one channel step in seven cases and by two in
the brand red, `#e1352f` landing on `#e1332d`. A brand colour moving is noticed by
whoever chose it, so write `2 75.3% 53.3%` rather than `2 75% 53%`.

The six palettes shipped here use integer triples and lose nothing, because they were
authored as HSL in the first place. There is no hex original for them to drift from.
That is the distinction: integers are fine for a palette designed in HSL and wrong
for one converted from hex.

`KeelTokens` lists every colour in Kotlin, and the same list is used three ways: the
gallery paints from it, `TokenContractTest` holds it against the stylesheets in both
directions, and it is the documentation of what an app may rely on. A token in the
CSS but not in `KeelTokens` fails the test, and so does the reverse.

### Five relationships, written once

`--input` is always `--border`. `--ring` is always `--primary`. `--popover` is always
`--card`. `--card-foreground`, `--popover-foreground` and `--accent-foreground` are
always `--foreground`.

They are declared as `var()` references on `:root`, which resolves against whichever
palette block won — because every one of those selectors targets the same element.
Deriving them is not a shortcut: it makes a mismatch impossible rather than merely
unlikely. An input border that has quietly drifted from the panel border is the kind
of thing nobody notices for a year.

### Shape

One number sets the language, and the rest are derived, so a change moves every
corner together instead of most of them:

```css
--radius: 0.75rem;
--radius-sm:  calc(var(--radius) - 4px);
--radius-md:  calc(var(--radius) - 2px);
--radius-lg:  var(--radius);
--radius-xl:  calc(var(--radius) + 4px);
--radius-pill: 9999px;
```

Each step is also a token in its own right, so an app whose scale is not a ladder
from one root overrides the steps directly and ignores `--radius`:

```css
:root {
  --radius-sm: 0.5625rem;   /* 9px  */
  --radius-lg: 0.875rem;    /* 14px */
  --radius-pill: 999px;
}
```

### Motion, type and shadow

`--ease`, `--duration-fast`, `--duration-slow` replace what would otherwise be
sixty-odd `150ms ease` literals, and make a shell that needs instant transitions —
a low-end television WebView, where a transition at D-pad frequency never finishes
before the next press — able to say so:

```css
:root { --duration-fast: 0ms; --duration-slow: 0ms; }
```

`--font-sans` and `--font-mono` are tokens rather than rules because this is the layer
a localised app replaces. A Georgian interface needs a face that covers the alphabet,
and changing that must not mean forking a stylesheet.

Shadows are overridden under `.dark`, and have to be: a 5% black shadow is invisible
against a dark surface, so one value would silently drop every elevation cue.

## The theme model

A theme is **data**, not an enum entry:

```kotlin
public data class Theme(
    val id: String,
    val label: String,
    val accentHex: String,
    val supportsLight: Boolean = true,
    val supportsDark: Boolean = true,
)

public class ThemeCatalog(val themes: List<Theme>, val default: Theme)
```

An enum would force a single-palette app to carry five palettes it will never paint,
and would make adding a palette a release of this library rather than a line in the
app that wants one.

`accentHex` is a plain hex rather than a token because a picker paints every swatch
at once while only one palette's variables are live — so five of six cannot come from
CSS.

`supportsLight` and `supportsDark` are what let one design system serve an app that
wants both and an app that wants exactly one:

```kotlin
val Cinema = Theme("cinema", "Cinema", "#e1352f", supportsLight = false)
```

```kotlin
public fun Theme.resolvesToDark(mode: ColorMode, systemPrefersDark: Boolean): Boolean
```

A single-mode theme **wins over the stored preference** rather than being overridden
by it. Someone who chose Light under one theme and then picked a dark-only theme must
not be shown an unpainted page, and their Light choice is kept for when they switch
back.

`availableColorModes` returns one entry for such a theme rather than an empty list,
so the result always names what will actually be used; a caller checks `size > 1` to
decide whether to draw a picker at all.

### Adding a palette

Two halves, and neither works alone. A `Theme` with no CSS block paints whatever was
declared last while reporting its own name; a CSS block with no `Theme` can never be
selected. `TokenContractTest` holds the two lists against each other.

```css
/* cinema.css - linked instead of palettes.css */
:root,
[data-theme='cinema'] {
  --background: 0 0% 0%;
  --foreground: 0 0% 100%;
  --card: 240 4% 5%;
  --primary: 2 66% 53%;
  --secondary: 158 58% 53%;
  --muted: 240 5% 9%;
  --muted-foreground: 240 4% 56%;
  --accent: 240 5% 9%;
  --border: 240 5% 11%;
  color-scheme: dark;
}
```

Declared on bare `:root` too, which for a single-palette app is the whole point:
there is no other palette for an unknown `data-theme` to fall back to.

## How the CSS gets there

**A Kotlin Multiplatform library's `jsMain/resources` do not reach a consumer's
distribution.** Not copied into the consumer's `jsProcessResources`, not packed into
the klib, not present in `build/dist`. Verified empirically in this repo by building
`:gallery` without the wiring: the output held `gallery.css` and no `keel/` directory
at all.

So every consumer needs one block, and there is no way to leave it out safely:

```kotlin
tasks.named<Copy>("jsProcessResources") {
    from(rootProject.layout.projectDirectory.dir("keel/src/jsMain/resources"))
}
```

It must come **after** the `kotlin { }` block, because that is what registers the
task. Placed above it, the build fails with `Task with name 'jsProcessResources' not
found`, which is at least loud.

The quiet part is why the comment on that block in `gallery/build.gradle.kts` is
long. Forget it and nothing errors: an unresolved `var()` falls back to the
property's initial value, the console stays clean, and the build stays green. The
page just renders unstyled.

`keel/` is kept as a subdirectory so a consumer's own stylesheet can never be
shadowed by one of the library's.

KGP does have a multiplatform-resources mechanism — `jsResolveResourcesFromDependencies`
and `jsZipMultiplatformResourcesForPublication` exist as tasks — which would remove
this block. It is experimental, and untried here.

## Traps this library exists to have solved once

Each of these was hit independently in both consuming apps, and each was fixed a
different way in each.

**`classes()` throws, and takes the composition with it.** Compose HTML puts every
entry through `DOMTokenList.add`, which raises `SyntaxError` on an empty token and
`InvalidCharacterError` on one containing a space. Either aborts composition of that
whole subtree. Nothing is logged where you would look, so the symptom is a row that
silently fails to appear while its data is perfectly correct. Always use
`classNames(...)`, which drops blanks and splits multi-word entries.

**An `innerHTML` written in `ref` freezes.** `ref` runs when the element is created
and never again, so swapping an icon on an existing span leaves the old drawing in
place while the `aria-label` updates — the control announces one thing and draws
another. `Icon` wraps its span in `key(icon)` so the element is discarded instead.

**Compose HTML's element builders create HTML elements, not SVG ones.** An
`<svg><circle>` built from them parses without complaint and draws nothing. SVG has
to be written as markup.

**A window listener needs `rememberUpdatedState`.** Registered once, a listener
captures the first lambda for the element's whole life. `DismissOnEscape` reads
through a `rememberUpdatedState`, so a dialog whose dismiss action changes does not
keep running the action from the step it opened on.

**Escape only reaches a focused element.** A modal opened by a button leaves focus on
the button, so a key handler on the dialog works exactly until someone opens it
without clicking into it — which is every time. It has to be a window listener.

## Decisions taken and not taken

An audit of both codebases proposed a considerably larger redesign than an
extraction. What was taken:

- **`color-scheme` per palette.** Neither app declared it, so both got light native
  scrollbars, form controls and pickers on a dark page.
- **The switch knob is `--primary-foreground`, not `#fff`.** The knob sits on
  `--primary`, and `--primary-foreground` is by definition the ink that belongs
  there. Identical output in all twelve palettes, and now a palette that needs a dark
  knob can have one without a rule changing.
- **Splitting the palettes out of the contract.** See above; driven by a real
  constraint, that a single-palette app must not inherit a fallback that fights it.
- **`Icon(size = null)`.** An interface that scales itself from one root font size has
  every length in `rem`, and an inline `px` written at the call site is the one thing
  that cannot participate.
- **`DismissOnEscape`, on by default.** One app had it and the other documented it
  without listening for it.

What was not taken, and why:

- **Generating the CSS from Kotlin.** It would remove the two-source-of-truth risk,
  which `TokenContractTest` already removes for the cost of a test rather than a code
  generator, a Gradle task and generated files in the tree. It would also cost the
  property that makes the library reusable: an app can add a palette without this
  library being rebuilt.
- **Retiring the `.dark` class for a `data-appearance` attribute.** Proposed so that
  source order stops mattering. There is exactly one correct link order, it is
  documented, and the change would break every stored preference and boot script in
  both apps to buy insurance against a mistake that fails loudly on the first look at
  the page.
- **Prefixing every class `keel-`.** Three names collide with Dakalebi today: `btn`,
  `spinner`, `switch`. Those are precisely the three components that should unify, so
  the collision is a migration instruction rather than a defect — see MIGRATION.md.
  Prefixing would rewrite every class in both apps to insure against a hazard that
  exists only while both stylesheets are linked at once.
- **Splitting hover into its own sheet**, for a shell with no pointer. Hover rules
  simply never match without a pointer, so the split buys little. The real issue it
  gestures at — hover sticking after a touch — is better answered by authoring hover
  inside `@media (hover: hover)`. Worth doing, and deliberately not done here: it
  would change how both apps look on a phone, which an extraction should not.
- **`--border-strong` and `--foreground-dim`.** Real needs in one app, and no
  component here reads either, so both would ship as dead API. `hsl(var(--foreground)
  / 0.15)` covers the first and `hsl(var(--foreground) / 0.72)` the second. They go in
  when a keel component wants them.
- **Focus as an `outline` instead of a `box-shadow`.** Correct for a shell where the
  ring is the cursor and must not be clipped by an ancestor's overflow. It changes how
  focus looks in the app the ring came from, so it belongs to a deliberate change
  rather than to a move.
