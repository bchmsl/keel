# Adopting keel

Two migrations, in this order. Dayboard first, because keel was extracted from it and
the move is mechanical; Dakalebi second, because that one is a real design decision
and benefits from the first being settled.

Neither has been performed. This is the plan, written from the code as it stands.

---

# Dayboard

Delete `tokens.css` and `components.css`, link keel's four sheets, and delete the
Kotlin that keel now owns. What is left in `screens.css`, `cards.css`, `dialogs.css`,
`panel.css` and `auth.css` is Dayboard's own layout and stays.

## Files that go away

| Delete | Replaced by |
|---|---|
| `src/jsMain/resources/tokens.css` | `keel/tokens.css` + `keel/palettes.css` + `keel/base.css` |
| `src/jsMain/resources/components.css` | `keel/components.css` |
| `ui/components/Button.kt` | `io.github.bchmsl.keel.components.Button` |
| `ui/components/Card.kt` | `…keel.components.Card` |
| `ui/components/Dialog.kt` | `…keel.components.Dialog` |
| `ui/components/Fields.kt` | `…keel.components.{Switch, Slider, TextField, TextAreaField}` |
| `ui/components/FormattedText.kt` | `…keel.components.FormattedText` |
| `ui/components/FormattingToolbar.kt` | `…keel.components.FormattingToolbar` |
| `ui/icons/Icon.kt`, `ui/icons/LucideIcons.kt` | `…keel.icons.{Icon, LucideIcon}` |
| `ui/cards/CardClasses.kt` (`sized`) | `classNames("card", "card--expanded".takeIf { expanded })` |
| `domain/text/FormattedText.kt`, `FormattingMarkers.kt` | `…keel.text.*` |
| `domain/model/Theme.kt` (`ThemeId`, `ColorMode`) | `…keel.theme.{Theme, ThemeCatalog, ColorMode}` |
| `data/ThemeController.kt` | `…keel.theme.ThemeController` |
| `dialogs/TaskFields.kt` → `FormattedField` | `…keel.components.FormattingField` |
| `tools/generate_lucide.py` | keel's copy |

Their tests move with them; keel already holds `FormattedTextTest` and
`FormattingMarkersTest` verbatim.

`domain/model/Tag.kt` stays. `Tag` itself is Dayboard's data. Only the palette part
moves: `TagShade` → `SwatchShade`, `TAG_COLORS` → `Swatches.All`, `DEFAULT_TAG_COLOR`
→ `Swatches.Default`, and `Tag.background(shade)` becomes a one-line delegate to
`swatchBackground(color, shade)`.

## Breaking changes, each needing an edit

**Renames in CSS.** Every keyframe is now prefixed, so a rule naming one has to
follow:

| Was | Now |
|---|---|
| `.font-mono-timer` | `.font-mono` |
| `.timer-pulse` | `.pulse` |
| `@keyframes spin` | `keel-spin` |
| `@keyframes fade-in` | `keel-fade-in` |
| `@keyframes dialog-in` | `keel-dialog-in` |
| `@keyframes slide-in-from-right` | `keel-slide-in-from-right` |
| `@keyframes slide-in-from-top` | `keel-slide-in-from-top` |

`.dialog__title` is **gone**, not renamed: nothing in the library ever emitted it, and
a dialog's visible heading is its caller's content. Any Dayboard rule using it keeps
its own copy.

`@keyframes task-complete` and `.task-done` are **not** in keel — a task fading when
it is finished is Dayboard's own idea. They move out of the deleted `tokens.css` and
into `cards.css`.

**`Card(draggable)` now defaults to `false`.** A general panel is not draggable. Every
board card must pass `draggable = true` explicitly. Miss one and its handle silently
disappears.

**`FormattedText(classNames = …)` is now `extraClasses = …`.** The parameter shadowed
the `classNames` helper it needed to call.

**`Icon(size)` is now `Int?`.** Every existing call still compiles. `null` means
"sized by CSS".

**Escape now closes dialogs.** This is a bug fix, and the one change to test by hand.

`ui/components/Dialog.kt` currently states that dismissing is available three ways —
the scrim, the close button and Escape. It is not: there is no window-level keydown
listener anywhere in `src/jsMain`, and the only `Escape` handlers are per-field
`onKeyDown`s that cancel an inline edit. Escape has never closed a Dayboard dialog.
The comment is wrong.

keel's `Dialog` listens for it, on by default. The interaction to check is the two
existing per-field handlers that cancel an inline edit inside a task's dialog —
`TaskEditDialog.kt:163` and `TaskEditDialog.kt:250`. Each must call
`event.stopPropagation()`, or one Escape inside an inline editor will cancel the edit
**and** close the dialog around it. A handler nearer the key wins once it stops
propagation.

`SettingsPanel.kt` has a third `"Escape" -> onCancel()` handler, in its tag editor,
but it needs no such fix: the settings panel is its own hand-built sliding overlay
(`.panel`, not `.dialog__content`) and never adopts keel's `Dialog` or
`DismissOnEscape` - it is the "side drawer" keel does not have yet, listed under
"What is not in it yet" in keel's own README. There is nothing for that Escape to
conflict with, so leave it alone.

**Buttons now carry `type="button"`.** Dayboard's three real submit controls are raw
elements with an explicit `type`, so nothing changes for them. Any future submit
button built from keel's `Button` passes `type = ButtonType.Submit`.

**`color-scheme` is now declared.** Native scrollbars, form controls and date pickers
follow the theme instead of always being light. An improvement, and a visible change
worth looking at once in dark mode.

**The dialog is labelled differently.** `aria-label` is gone in favour of
`aria-labelledby` / `aria-describedby` pointing at the hidden heading and paragraph,
and focus now moves into the dialog on open and back to the opener on close. Nothing
visual changes; what changes is what a screen reader says and where Tab resumes.
Worth one pass with VoiceOver.

**Theme ids are now constrained** to lower-case letters, digits and single hyphens.
All six of Dayboard's already comply, so this is only a note for a new palette.

**The scrollbar rules moved behind `@supports`.** Dayboard's own
`::-webkit-scrollbar` block never ran — the `scrollbar-width: thin` on `*` in the same
file disables those pseudo-elements in Blink and WebKit, measured at an 11px gutter
rather than the intended 6px. Nothing changes by adopting keel's version; the point is
that the 6px never existed, so do not treat its absence as a regression.

## What becomes available

New tokens Dayboard can adopt at its own pace. Counted across its seven stylesheets:

| Token | Replaces |
|---|---|
| `--radius-xs/sm/md/lg/xl/pill` | 42 `border-radius` declarations written as a literal, 15 of them the literal `9999px` |
| `--duration-fast`, `--duration-slow` | 63 `150ms ease`, 3 `200ms ease-out` |
| `--ease` | nothing yet — it *is* `ease`, so adopting it changes no timing. It exists so an app can retime everything from one line. |
| `--font-sans`, `--font-mono` | 3 font-family literals holding 2 distinct stacks: Inter once, JetBrains Mono twice |
| `--shadow-sm`, `--shadow-lg` | the repeated `0 1px 2px 0 rgb(0 0 0 / 0.05)` and the dialog's larger pair |

`--success` is **not** in that table: Dayboard has no success colour today, so it is
new API rather than a replacement.

`classNames(...)` replaces the `*listOfNotNull(…).toTypedArray()` shape, which appears
31 times across `src/jsMain/kotlin`.

## Order of work

1. Add the submodule and the `jsProcessResources` block. Link keel's four sheets
   **before** Dayboard's own, and delete `tokens.css` and `components.css`.
2. Move `task-complete` / `.task-done` into `cards.css`. Rename the seven CSS
   references above.
3. Swap the theme model and `ThemeController`. Keep the storage keys — they are
   keel's defaults precisely so no user loses their palette.
4. Swap the components, one file at a time. `Card` last; it has the most call sites
   and the `draggable` change.
5. Fix the three Escape handlers, then test by hand: open a task, edit a subtask
   inline, press Escape once. The edit should cancel and the dialog should stay.

---

# Dakalebi

A real design decision, not a move. Its own `tokens.css` argues the case for staying
as it is, and the argument is good:

> Deliberately single-theme: this is a cinema surface, and a light variant would
> fight the video it exists to frame. True black rather than charcoal so the player
> has no visible seam against the page on OLED screens.

keel does not ask it to give that up. Dakalebi becomes a **one-palette, dark-only
app** that shares the primitives, and keeps every decision that makes it itself.

## The palette

One `Theme`, and one stylesheet linked instead of `palettes.css`:

```kotlin
val Cinema = Theme("cinema", "Cinema", "#e1352f", supportsLight = false)
val theme = ThemeController(catalog = ThemeCatalog(listOf(Cinema), default = Cinema))
```

`supportsLight = false` makes `isDark` always true regardless of what is stored, and
leaves `availableColorModes` with one entry — so a settings screen draws no mode
picker rather than three buttons that do nothing.

Write the triples to **one decimal place**. At zero decimals the brand red drifts from
`#e1352f` to `#e1332d`, which is the one colour nobody should have to notice moving.

| Dakalebi | Hex | HSL triple | keel token |
|---|---|---|---|
| `--bg` | `#000000` | `0 0% 0%` | `--background` |
| `--elev` | `#0d0d0f` | `240 7.1% 5.5%` | `--card` |
| `--elev2` | `#141417` | `240 7% 8.4%` | `--muted`, `--accent` |
| `--line` | `#1c1c20` | `240 6.7% 11.8%` | `--border` |
| `--tx` | `#ffffff` | `0 0% 100%` | `--foreground` |
| `--mut` | `#8a8a93` | `240 4% 55.9%` | `--muted-foreground` |
| `--red` | `#e1352f` | `2 74.8% 53.3%` | `--primary` |
| `--ok` | `#3ecf8e` | `153.1 60.2% 52.7%` | `--secondary`, `--success` |

Four have no keel token and do not need one:

| Dakalebi | Write as |
|---|---|
| `--line-strong` | `hsl(var(--foreground) / 0.15)` |
| `--tx-dim` | `hsl(var(--foreground) / 0.72)` |
| `--red-dim` | `hsl(var(--primary) / 0.5)` |
| `--pad`, `--rail-gap` | Keep. Page layout, not design system. |

**Do not also adopt keel's `--destructive`.** Dakalebi's brand red *is* its danger
colour — `.btn-danger` derives from `--red` at alpha — and inheriting a separate
`--destructive: 0 84% 60%` would give one brand two reds a few degrees apart. Override
it: `--destructive: var(--primary)`.

## The three collisions

`btn`, `spinner` and `switch` exist in both. That is not a defect — they are exactly
the three components that should unify — but both stylesheets must not be linked at
once, or keel's rules will quietly restyle Dakalebi's existing controls.

Delete Dakalebi's own rules for all three **in the same commit** that links keel's
sheet:

- `.btn` and its `-primary` / `-ghost` / `-quiet` / `-danger` variants map onto
  `ButtonVariant.{Default, Ghost, Outline, Destructive}`.
- `.switch` / `.switch > div` becomes keel's `Switch`. Two gains: the state is keyed
  off `aria-checked` rather than an `.on` class written from Kotlin, so the colour and
  the announcement cannot disagree; and it stops being three near-identical controls
  (`.switch`, `.tv-switch`, `.tv-ctl-switch`) sized by hand.
- `.spinner` is the awkward one. In Dakalebi it is a 38px bordered ring positioned
  inside the player; in keel it is a bare `animation` applied to any icon. Same name,
  same keyframe, incompatible meaning. Rename Dakalebi's to `.player-spinner` — its
  own keyframe is already `keel-spin`'s twin and can go.

## What each shell overrides

The two pages already link different sheets, which is the mechanism keel needs. Web
links `tokens.css` then its own; TV links `tokens.css` then `tv.css` and deliberately
never loads the web sheet.

**Both:**

```css
:root {
  --font-sans: 'Noto Sans Georgian', 'BPG Arial', Sylfaen, ui-sans-serif, system-ui, sans-serif;
  --radius-xs: 0.25rem;
  --radius-sm: 0.5625rem;   /* 9px, small chrome over media */
  /* `.btn` reads --radius-md, and every Dakalebi button is a capsule. Leave this
     out and the buttons quietly become 10px rounded rectangles, because the step
     keeps deriving from keel's --radius. */
  --radius-md: var(--radius-pill);
  --radius-lg: 0.875rem;    /* 14px, surfaces */
  --radius-xl: 0.875rem;
  --radius-pill: 999px;
  --ease: cubic-bezier(0.32, 0.72, 0, 1);
}
```

Every step, not a subset. A step left alone keeps deriving from keel's `--radius`.

**TV only** — the reason `--duration-*` are tokens at all:

```css
:root {
  --duration-fast: 0ms;
  --duration-slow: 0ms;
}
```

Its own comment records why: a low-end built-in-TV WebView repaints every
transitioning element per frame for the whole duration, and at D-pad frequency those
frames never finish before the next press. That was previously unreachable from a
consumer sheet, because the durations were 66 literals.

## What Dakalebi keeps

Everything that is the product: the player and its chrome, tiles, rails, the hero, the
TV navigation rail, the spatial-navigation engine, thumbnails and fallback gradients,
`TvConfirmDialog` with its real focus trap, and `.auth-field`'s `display: contents`
trick that lets one sign-in screen serve a browser and a television.

Two icons stay too. `back10` and `forward10` draw a literal "10" inside the arc and
lucide has no equivalent, so they remain Dakalebi's. Every other glyph it uses is in
keel's catalogue: play, pause, volume, cast, fullscreen, menu, more, link, download,
check, back, home, grid, gear.

## What Dakalebi gives keel

Two things it already has and Dayboard does not, both worth pulling up:

- **`DismissOnEscape`** — already taken. It is in keel, on by default in `Dialog`, and
  it fixes the Dayboard defect described above.
- **iOS switch proportions.** keel's switch is 36×20; Dakalebi's is 51×31 on the web
  and a scaled `4.625rem` on the TV. Worth a size variant rather than a fork.

## Three known gaps

Real constraints keel does not yet meet. Each is recorded in ARCHITECTURE.md under
decisions not taken, with the reason it was deferred rather than done.

1. **Focus is a `box-shadow`, and needs to be an `outline` for the TV.** A box-shadow
   ring is clipped by an ancestor's `overflow`; an outline is not. Dakalebi also
   measured that a focused element must not move, while keel scales the slider thumb
   on hover.
2. **Hover is folded into base rules.** Harmless without a pointer, but the honest fix
   is to author hover inside `@media (hover: hover)`.
3. **Icon size at the call site.** Partly solved: `Icon(size = null)` lets CSS own it,
   which is what a `rem`-canvas layout needs. The remaining half is that keel has no
   contextual sizing rules of its own, so the TV shell writes them.
