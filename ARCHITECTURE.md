# How keel is put together

## The one rule

**A component names a token. It never names a value.**

That is the whole of it. Every colour, radius, duration, font and shadow a component
reads is a CSS custom property, and the only place a literal appears is a token
definition. It is what lets one stylesheet serve a light six-palette dashboard and a
single-palette dark video app without either editing the other's rules.

`TokenContractTest` enforces the colour half of it: a hex value, an `rgb()`/`hsl()`
functional notation, a modern `oklch()`-family function, or one of the common CSS
colour keywords, anywhere in `base.css` or `components.css`, fails the build. One
allowance, black at low alpha, for the modal scrim — a scrim dims whatever is behind
it, which is not a palette's business.

A second test enforces the size half: a `height` or `width` given a fixed length —
`px`, `rem` or a print unit — anywhere in `base.css` or `components.css` fails the
build. Two allowances, both idioms with an exact required value rather than a design
choice: `.sr-only`'s 1px clip box, and the `::-webkit-scrollbar` gutter. Relative
units are not flagged, because `%`, `em` and the viewport units already answer to
something the consumer controls.

That test exists for a failure that is easy to miss. A literal `height: 2.5rem` is
not wrong on its own — it already scales with the root font size. It goes wrong for a
consumer whose root size is spoken for by something else: a ten-foot interface sets
its root from the viewport to make its layout work, and then needs a *larger* control
at that same root. With the height written out, the only way to get one is to re-state
the rule by selector from outside, and a stylesheet that re-states another has forked
it — silently, and only for the app that did it.

Radius, duration and font are held to the same rule by review rather than by a test.
`components.css` has no literal of any of them today; `base.css` carries a few
animation durations, which are the animation's own timing rather than the interface's.

## When a consumer cannot call the composable

Two cases, both legitimate, and each has its own hatch. Before they existed a consumer
in either position wrote keel's class names out as string literals: four dozen
spellings of the button classes in one app alone, checked by nothing.

**The element is keel's, but the wiring is not.** Every primitive takes an `attrs`
slot, which runs last so the caller wins on conflict. It is where a `ref`, a `data-*`
attribute, a conditional inline `style` or a form's `name`/`autocomplete` go. Classes
accumulate rather than replace, which is deliberate: adding a marker class is the
common case and losing `btn` is never the intent.

**The element is not the one keel builds.** `dom/ComponentClasses.kt` exposes the class
lists as functions — `buttonClasses(variant, size)`, `switchClasses()`, and the rest. A
ten-foot interface renders its controls as `Div role="button"` on purpose, because a
real `<button>` brings a UA stylesheet, its own focus ring and its own activation
behaviour, all three of which fight a surface where the focus ring *is* the cursor. It
can consume keel's classes; it can never call `Button()`.

This is what lets `ButtonVariant.className` stay `internal`. The mapping is public and
the field is not, so a variant can be renamed in one place. The functions return a
space-separated list, so the result goes through `classNames(...)` — `classes(...)`
would throw on the space, which is the first trap listed below.

`ClassNameContractTest` holds the two halves together: a class name keel's Kotlin emits
with no rule in `base.css` or `components.css` fails the build. That is the alarm that
was missing when a shared sign-in screen was changed to emit keel's classes and one of
the two shells rendering it did not link keel's stylesheet. Nothing failed. A class name
is a string, an undefined class selects nothing, and an element with no rules is a
perfectly valid element, so it compiled, logged nothing, and shipped as unstyled browser
boxes. One allowance, `card--expanded`: an expanded card fills whatever the app's board
is, and keel owns components rather than layout, so `Card` hands the state over as a
class and lets the app decide what it means.

The reverse direction is deliberately not checked. A class keel defines but never emits
is not necessarily dead — some exist precisely so a consumer that cannot call the
composable can still build the markup.

## Focus and hover

Two rules, both enforced by `TokenContractTest` rather than by review, and both
stated in the header of `components.css` where the next component gets written.

**Every focus rule is a two-branch selector list** — `.thing:focus-visible,
.thing[data-keel-focus]`. The second branch is for a consumer that moves focus itself
instead of letting the browser do it. A D-pad interface is the case that forced it:
focus there is a cursor the app owns, it lands on a `div` no browser would call
focusable, and `:focus-visible` heuristics do not fire in a WebView the way they do on
a desktop. Both branches are specificity (0,2,0), so adding one reorders nothing. The
attribute is namespaced so a consumer's own unrelated `data-focus` cannot be restyled
by accident.

**Every `:hover` rule is wrapped in `@media (hover: hover)`.** Without it a tap leaves
the hover state stuck on the control, because the pointer that would have left never
existed. Desktop is unaffected.

## The four stylesheets

Link them in this order.

| | |
|---|---|
| `tokens.css` | The contract. Shape, motion, type, shadow, and the colours that do not change with a palette. **Declares no palette**, so on its own it paints nothing. |
| `palettes.css` | The six shipped palettes, light and dark. **Optional.** |
| `base.css` | The reset, the document surface, the animations, the scrollbars. Reads tokens, defines none. |
| `components.css` | The primitives. Reads tokens, defines none. |

Only one of those orderings is load-bearing, and not for the reason it looks like.
A custom property is substituted from the element's *fully cascaded* value, not from
whatever stylesheet happens to have been parsed — so `base.css` and `components.css`
resolve their colours whichever order they arrive in. Served fully reversed, every
computed value is identical.

What does matter is that **`tokens.css` comes before the palette sheet**. `:root` and
`[data-theme='...']` carry the same specificity, so a token declared in both is
settled by source order alone. `--secondary-foreground` is declared in both today, and
reversing the two sheets would silently cost the two palettes that override it their
dark ink — one token in two palettes, not a broken page.
`TokenContractTest.tokensMustBeLinkedBeforeAPaletteSheet` pins exactly which tokens
are in that overlap, so the list cannot grow unnoticed.

An unresolved `var()` really does fall back to the property's initial value, and
silently — but that is the symptom of a sheet not being *delivered*, never of one
being misordered.

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

### Six derived tokens, written once

`--input` is always `--border`. `--ring` is always `--primary`. `--popover` is always
`--card`. `--card-foreground`, `--popover-foreground` and `--accent-foreground` are
always `--foreground`. Six tokens, four rules.

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
--radius-xs:  calc(var(--radius) - 8px);
--radius-sm:  calc(var(--radius) - 4px);
--radius-md:  calc(var(--radius) - 2px);
--radius-lg:  var(--radius);
--radius-xl:  calc(var(--radius) + 4px);
--radius-pill: 9999px;
```

Each step is also a token in its own right, so an app whose scale is not a ladder from
one root can override the steps directly — but it has to override **all** of them.
A step left alone keeps deriving from `--radius`, and the two that are easiest to
forget are the two most visible surfaces: `--radius-md` is what `.btn` reads and
`--radius-xl` is what `.card` reads.

```css
:root {
  --radius-xs: 0.25rem;
  --radius-sm: 0.5625rem;   /* 9px  */
  --radius-md: var(--radius-pill);
  --radius-lg: 0.875rem;    /* 14px */
  --radius-xl: 0.875rem;
  --radius-pill: 999px;
}
```

### Motion, type and shadow

`--ease`, `--duration-fast` and `--duration-slow` replace the 63 `150ms ease` and 3
`200ms ease-out` literals the source repeated.

`--ease` is the CSS keyword `ease`, not a curve of its own, and that is the point: it
is the value those rules already used, so tokenising them changed nothing about how
anything moves. A house curve belongs to an app, and an app that wants one sets the
token. It also makes a shell that needs instant transitions — a low-end television
WebView, where a transition at D-pad frequency never finishes before the next press —
able to say so:

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
  --card: 240 7.1% 5.5%;
  --primary: 2 74.8% 53.3%;
  --secondary: 153.1 60.2% 52.7%;
  --muted: 240 7% 8.4%;
  --muted-foreground: 240 4% 55.9%;
  --accent: 240 7% 8.4%;
  --border: 240 6.7% 11.8%;
  color-scheme: dark;
}
```

One decimal place, per the rule above — these are converted from hex, and at zero
decimals the brand red would land on `#e1332d`.

Declared on bare `:root` too, which for a single-palette app is the whole point:
there is no other palette for an unknown `data-theme` to fall back to.

A dark-only palette still gets the `dark` class on `<html>`, and needs to.
`resolvesToDark` returns true for it whatever is stored, so both `ThemeController`
and the boot script add the class - which is what lets `:root.dark`'s stronger
shadows and `color-scheme: dark` apply. A palette written to select on
`[data-theme='cinema']` alone therefore works; so does one that also qualifies with
`.dark`. What does not work is assuming the class is absent because there is no
light mode to contrast with.

## How the CSS gets there

**A Kotlin Multiplatform library's `jsMain/resources` do not reach a consumer's
distribution.** Not copied into the consumer's `jsProcessResources`, not packed into
the klib, not present in `build/dist`. Verified empirically in this repo by building
`:gallery` without the wiring: the output held `gallery.css` and no `keel/` directory
at all.

So every consumer needs one block, and there is no way to leave it out safely:

```kotlin
tasks.named<Copy>("jsProcessResources") {
    from(rootProject.layout.projectDirectory.dir("keel/keel/src/jsMain/resources"))
}
```

That is the path in a consumer with the submodule at `keel/`: the repository root,
then the library module inside it. `gallery/build.gradle.kts` writes
`keel/src/jsMain/resources` because it already *is* inside this repository. A `Copy`
whose `from` does not exist is not an error — the build stays green and nothing is
copied — so the wrong one of those two fails exactly as quietly as omitting the block.

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

The first three were hit independently in both consuming apps, and fixed a different
way in each. The last two are Dayboard's alone, and were not fixed there at all.

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

- **`color-scheme` per palette.** Dayboard declared it nowhere, so it got light native
  scrollbars, form controls and pickers on a dark page. Dakalebi sets it once,
  globally, which is right for a single-palette app and cannot express six.
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
  without listening for it. It also guards `isComposing`, so Escape backing out of an
  input method's candidate list does not take the dialog with it, and `repeat`, so a
  held key dismisses one thing rather than thirty.
- **`aria-labelledby` and `aria-describedby` on the dialog, not `aria-label`.** A label
  plus a hidden heading holding the same words gets the title announced twice, which
  is the failure the hidden heading exists to avoid. Focus now moves into the dialog
  and returns to whatever opened it; Tab is still not cycled, and the KDoc says so.
- **The `::-webkit-scrollbar` block is guarded by `@supports`.** Blink and WebKit
  ignore those pseudo-elements entirely on an element that also has a non-initial
  `scrollbar-width`, which `*` did. Measured in Chrome 148: the pseudo-elements alone
  give a 6px gutter, the standard properties alone give 11px, both together give 11px.
  Seventeen lines that read as load-bearing and never ran — inherited from the source,
  where they still do not run.

- **`Card` is built on `Surface`, not beside it.** The untitled panel is the *more*
  common shape — a login panel, a stat tile, a next-up card — and each app had
  hand-written its own box for it, four of them across two apps agreeing on none of
  their four values. `.surface` is now the box and `.card` carries only what a board
  panel adds, which is filling its column. Both classes are specificity `(0,1,0)` and
  set disjoint properties, so their order is irrelevant and `Card`'s rendered result is
  unchanged: same border, radius, colour and shadow, measured.

- **`--primary-foreground` is the ink for content laid over a picture.** A bar over a
  poster cannot take its track colour from the page: `--muted` is a pale grey in the
  light palettes and would vanish against a bright still. The honest alternative was a
  literal white at low alpha, which is the one thing a component may not name. This
  token is already declared palette-independent, as the ink that sits on a saturated
  fill of the app's own choosing — and a picture is a saturated fill whose colour
  nobody knows in advance, which is the same job. `.progress--on-media` and every
  `.scrub__*` layer read it.

- **A pick-one control is a radio group, not a row of buttons with `aria-pressed`.**
  All five hand-written versions across the two apps were buttons: appearance from a
  class, selection announced as "pressed". A toggle button says pressed; it cannot say
  "two of five", and none of the five had arrow-key navigation, a single tab stop, or
  Home and End, because getting those from buttons means reimplementing roving
  tabindex. `SegmentedControl` is `label > input[type=radio] + span`, so the browser
  supplies all of it, and the selected colour is keyed off `:checked` rather than a
  class keel adds — the same argument as the switch's `aria-checked`. A class can
  disagree with what is announced, and it fails in the direction that matters: looking
  right while announcing wrong.

- **The segmented focus rule carries its own container class, purely for specificity.**
  `.segmented--rail .segmented__input:checked ~ .segmented__label` is `(0,4,0)` and
  sets `box-shadow: none`, because the rail's selected chip is a flat primary fill with
  no lift. Written the obvious way, the focus rule would be `(0,3,0)` and lose — so a
  focused *selected* rail chip would have shown no ring at all, the one state a keyboard
  user most needs to see. Both focus branches are written as
  `.segmented .segmented__input:focus-visible ~ .segmented__label`, which reaches
  `(0,4,0)` and then wins on source order. The redundant-looking ancestor is the whole
  point of the rule.

- **The hidden radio gets its own visually-hidden rule rather than `.sr-only`.**
  `.sr-only` is `position: absolute`, which resolves against the nearest positioned
  ancestor. On the rail that ancestor would be the scroller, so focusing a chip would
  scroll a 1px box somewhere else into view instead of the chip. `.segmented__item` is
  `position: relative` and `.segmented__input` is hidden against it, measured: the
  input's `offsetParent` is its own item.

- **The "finished this one" check takes `color: inherit` on the chosen chip.** It is
  `--success` normally, and the first version named `--primary-foreground` for the
  selected state, which is right for the rail's primary fill and wrong for the track's,
  where the chip is `--card` — a white check on a light card in half the palettes.
  `inherit` lands on whatever that chip's text colour already is, correct in both
  treatments without naming either.

What was not taken, and why:

- **`overflow: hidden` on `.surface`.** It looks obviously right: an unpadded surface
  holding a poster has square corners inside a rounded box, so the radius is
  decorative. But clipping also removes what is *meant* to leave the box, and keel's
  focus ring is a `box-shadow` drawn outside its control — so a clipped card would lose
  the ring on the button in its header entirely, and lose it invisibly. Which kind a
  surface is depends on what is inside it, so it is `clipped = false` by default and
  the caller says. This is the same clipping hazard as the open focus-ring gap below,
  arriving from the other direction.

- **A separate `FilterChip` beside `SegmentedControl`.** The plan called for two
  components, on the reasoning that unifying them means one grows a slot it never uses.
  Reading all five hand-written versions first showed that is not what separates them:
  they differ only in the container — a shared track versus a horizontal scroller — and
  the "finished this one" check is one optional field on a segment, not a slot. A second
  component would have duplicated the whole radio-group mechanism to change a `display`
  and a `background-color`. `SegmentedStyle` is that difference.

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
  gestures at — hover sticking after a touch — was answered instead by authoring every
  hover inside `@media (hover: hover)`, which is now the rule and is tested. That was
  held back from the extraction itself, on the grounds that a move should not change
  how either app looks on a phone; it landed as a deliberate change afterwards.
- **`--border-strong` and `--foreground-dim`.** Real needs in one app, and no
  component here reads either, so both would ship as dead API. `hsl(var(--foreground)
  / 0.15)` covers the first and `hsl(var(--foreground) / 0.72)` the second. They go in
  when a keel component wants them.
- **Focus as an `outline` instead of a `box-shadow`.** Correct for a shell where the
  ring is the cursor and must not be clipped by an ancestor's overflow. It changes how
  focus looks in the app the ring came from, so it belongs to a deliberate change
  rather than to a move.
