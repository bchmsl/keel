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

## The collisions

`btn`, `spinner` and `switch` exist in both. That is not a defect — they are exactly
the three components that should unify — but both stylesheets must not be linked at
once, or keel's rules will quietly restyle Dakalebi's existing controls.

Three more arrived with Phase 3, and they behave differently: `scrub`, `scrim` and
`toast` are names keel now owns that Dakalebi also defines, and `web.css` and `tv.css`
both load *after* keel's sheet. So Dakalebi keeps its own rendering for all three
until its rules are deleted, in the same commit that adopts each component. That is
the safe direction — nothing changes until it is meant to — but it also means the
adoption is not finished when the composable is called. Checked once per name: keel's
other Phase 3 classes (`surface`, `badge`, `skeleton`, `loader`, `empty-state`,
`progress`, `segmented`, `drawer`, `dropdown`, `toast-host`) collide with nothing in
either app.

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
  and a scaled `4.625rem` on the TV. Worth a size variant rather than a fork — and now
  reachable by setting `--switch-track-w/h`, `--switch-knob-size` and
  `--switch-knob-inset`, with the knob's travel deriving from them automatically.
- **The iOS field-zoom guard** — already taken. Dakalebi's login sheet carried
  `@media (pointer: coarse) { .input { font-size: 16px } }`, which is keel's `.input`
  restated from outside for a browser bug keel's own default causes. It is now in
  `components.css` for both `.input` and `.textarea`, through
  `--input-font-size-touch`. **Dayboard inherits it**: its fields go from 14px to 16px
  on a touch device, which is the fix. The token exists because `pointer: coarse` is
  not only a phone — a remote-driven shell can match it, and there an absolute 16px is
  smaller than the interface around it.

## Known gaps

Real constraints keel does not yet meet. Each is recorded in ARCHITECTURE.md under
decisions not taken, with the reason it was deferred rather than done.

1. **Focus is a `box-shadow`, and may need to be an `outline` for the TV.** A
   box-shadow ring is clipped by an ancestor's `overflow`; an outline is not. Still
   open. The related half of this — that a focused element must not move, while keel
   scaled the slider thumb on hover — is closed by the `@media (hover: hover)` wrapper:
   with no pointer, the rule no longer matches at all.
2. **Icon size at the call site.** Partly solved: `Icon(size = null)` lets CSS own it,
   which is what a `rem`-canvas layout needs. The remaining half is that keel has no
   contextual sizing rules of its own, so the TV shell writes them.

Closed since:

- **Hover was folded into base rules.** Every `:hover` is now authored inside
  `@media (hover: hover)`, and a test fails the build if one is not.
- **Control sizes were literals.** Height, padding, control font size, and the switch
  and slider geometry are tokens, so a shell at a different viewing distance can
  resize the set from `:root` rather than re-stating keel's rules by selector. The
  switch size variant suggested above is now a matter of setting four properties.
- **The components were closed, so consumers spelled the classes by hand.** Every
  primitive takes an `attrs` slot, and `dom/ComponentClasses.kt` returns the class lists
  for markup keel does not build. `ClassNameContractTest` fails the build on a class name
  emitted by Kotlin with no rule in keel's CSS, which is the check that was missing when
  this went wrong the first time.
- **An `<a>` styled as a button had no composable.** `LinkButton` is one, and it is a
  real anchor: it can be middle-clicked, copied and opened in a new tab, none of which a
  button with a navigating `onClick` can do. It has no `enabled` parameter, because HTML
  gives an anchor no disabled state — a control that can be off is a `Button`.
- **A panel without a title had no component, so every app drew its own box.** `Surface`
  is that box, and `Card` is now built on it, so the two cannot drift apart. Four
  hand-written panels across the two apps agreed on none of their four values.
- **Empty, waiting and status had nothing.** `EmptyState`, `Skeleton`, `Spinner` and
  `Badge` cover them. `Spinner` renders `.loader`, not `.spinner` — `base.css` already
  owns that name as the icon-rotation utility, and both apps have a `.spinner` of their
  own, so the component taking it would have restyled all three.
- **Five progress bars, and no two alike.** `ProgressBar` replaces four of them —
  Dakalebi's hero, tile, countdown and thin bars — which between them carried three
  thicknesses, three track alphas, one radius and one width transition. None of those
  four differences was a decision. Adopting it changes the three odd alphas to one, gives
  the three edge-hugging bars a pill radius they are too thin to show, and retimes the
  countdown from 200ms to `--duration-fast`.
- **The fifth bar is `Scrub`,** deliberately its own component: it needs a buffered layer,
  it is painted from a frame loop rather than from state, and it lives over picture. It
  hands the caller a `ScrubHandle` whose `setPosition` is ignored mid-drag — the guard
  both players had to learn separately — and which rewrites the range input's value and
  `aria-valuetext` alongside the visible fill, so the bar and the announcement cannot
  disagree. `.scrub` collides with Dakalebi's own player rules, and web.css loads later,
  so Dakalebi keeps rendering its own until those rules are deleted in the same commit
  that adopts the component.
- **Five pick-one-of-a-set controls, none of them a radio group.** `SegmentedControl`
  replaces `.seg`, `.tv-seg`, Dakalebi's seasons chips, Dayboard's `.panel__modes` and
  Dayboard's tag chips. All five drove appearance from a class and announced selection
  with `aria-pressed`, which is the wrong role: a toggle button says "pressed", not "two
  of five", and none of the five got arrow-key navigation, a single tab stop, or Home and
  End. The component is one real radio group per control, so the browser supplies all
  three, and the selected colour is keyed off `:checked` rather than a class, so the paint
  and the announcement cannot drift.

  It is **one** component with two treatments rather than the two the plan named. The
  planned split was `SegmentedControl` for the all-visible group and `FilterChip` for the
  scrolling rail, on the reasoning that unifying them means one grows a slot it never
  uses. Reading all five implementations first showed that is not what separates them:
  they differ only in the container — a shared track versus a horizontal scroller — and
  the "finished this one" check is one optional field on a segment rather than a slot. A
  separate `FilterChip` would have duplicated the whole radio-group mechanism to change a
  `display` and a `background-color`. `SegmentedStyle.Track` and `SegmentedStyle.Rail`
  are that difference.

  Three visible changes come with adopting it:

  | Where | Today | After |
  |---|---|---|
  | Dakalebi's settings segmented control | selected chip inverts to white on black | selected chip lifts: `--card` fill plus `--shadow-sm` |
  | Dakalebi's seasons rail | selected chip is white | selected chip is brand `--primary` |
  | Dayboard's mode switcher | rounded rectangle | pill, matching every other keel track |

  Dayboard's tag chips are the one case that may not want this component at all: their
  colour comes from the tag rather than from selection, and they dim rather than fill.
  That is a `Pill` with a selected state, and it is a Phase 4 question for Dayboard, not
  something `SegmentedControl` should grow a parameter for.
- **The overlay layers were renumbered by hand in both apps, and one of them had a bug
  in it.** `Scrim`, `Drawer`, `DropdownMenu` and `ToastHost` now settle the whole band
  against the `--z-*` tokens: scrim 20, drawer 30, catcher 39, dropdown 40, dialog 50,
  toast 60, measured strictly ascending.

  The bug is worth recording because it is the clearest case in either app of a
  component being borrowed rather than shared. Dakalebi's season menu needed a
  click-catcher, took `.scrim`, and cancelled it inline with `background:
  transparent`. That cleared the dim but not `backdrop-filter`, so opening the menu
  blurred the entire page; and at the scrim's z-index of 60 against `.menu`'s 50 the
  catcher covered the very menu it was opened for, so neither item could be clicked.
  The app later wrote `.popover-catch` to fix it, which is now `.dropdown__catch` -
  a separate class, one layer below its menu, with no dim and no blur.

  `DropdownMenu` deliberately does **not** claim `role="menu"`. See ARCHITECTURE.md.

  Five hand-written scrims carried two dim colours, three alphas and two blur radii.
  One dim now, `rgb(0 0 0 / var(--scrim-alpha))`, and `.dialog__overlay` reads the same
  token rather than keeping its own. Two visible changes come out of that:

  | Where | Today | After |
  |---|---|---|
  | keel's own `Dialog`, in both apps | 0.8 black, no blur | 0.7 black with a 4px blur |
  | Dayboard's panel scrim | `--background` at 0.6 | 0.7 black |

  The second is a fix, not a preference: `--background` at 0.6 is a wash of near-white
  over a near-white page on any light palette, which dims nothing at all.

  Two things the plan named and this does not build. `DrawerEdge` has no `Bottom`,
  because nothing wants one - the plan described Dakalebi's menu sheet as a bottom
  sheet and it is a left-anchored drawer (`.sheet`, `left: 0; top: 0; bottom: 0`), so
  a `Bottom` variant would ship as dead API. And `Drawer` has no header: Dayboard's
  panel has a sticky header over a scrolling body, Dakalebi's sheet is one padded
  scrolling stack, and guessing which shape keel should own is the Phase 4 question
  rather than this one. `Drawer` is the sliding surface; the header is content.

## `DropdownSide.Above`

Added after the fact, by the first consumer that needed it. Dakalebi's player has a
quality menu in a control bar pinned to the foot of the video: a menu hung downward
from there opens off the bottom of the screen. `.dropdown--above` is the mirror of the
default, and `keel-slide-in-from-bottom` mirrors `keel-slide-in-from-top` in base.css.

The choice stays the caller's, like `DropdownAlign`, and for the same reason: deciding
it automatically means measuring the trigger against the viewport on every open and
following it afterwards, which is the portalled dropdown this component deliberately
is not. A caller whose control bar is pinned to the bottom already knows the answer.

One trap worth recording. The rule needs `top: auto`, because `.dropdown` resolves
`top` and a box with *both* offsets resolved takes its height from the band between
them rather than from its content. For a menu hung outside its trigger that band is
negative, so the failure is a collapse rather than an overflow: measured on a 36px
trigger, 89px tall with the rule and 14px without it.

## `SwitchSize.Small`

Also added by a consumer, and for a case the web shells never hit: Dakalebi's TV shell
has *two* switches on one page. One stands at the end of a settings row; the other sits
inside a labelled pill in the player's control bar, where the default track is taller
than the pill containing it. keel's switch geometry was four `:root` tokens, so a second
size on the same page was unreachable without restating the rules from a consumer sheet.

`.switch--size-sm` rebinds those four tokens on the element rather than restating the
six declarations that read them. That is the whole trick, and it is worth stating why it
works: a custom property is resolved per element from its own cascaded value, and the
knob inherits the track's, so `translateX(track - knob - 2 * inset)` re-derives itself
for the smaller box with nothing written twice. A duplicated rule block would have had
to restate that derivation, which is the one value here that is computed rather than
given.

It carries geometry only. A small switch is the same control at a different size, so the
colours, the transition and the `aria-checked` state stay one definition.

The four `-sm` defaults are proportional to the default set and change nothing that
exists: no shipped rule referenced them before. Measured against `tv.css`'s hand-written
pair, keel reproduces all six of its numbers from the eight tokens:

| | keel derives | `tv.css` stated |
|---|---|---|
| settings track | 74x42 | 74x42 |
| settings knob, travel | 34x34, 32px | 34x34, 32px |
| player track | 60x34 | 60x34 |
| player knob, travel | 26x26, 26px | 26x26, 26px |

Both sizes keep a symmetric 4px gap at whichever edge the knob is resting against.

## `DropdownMenuItem(selected)`

The third thing a consumer needed and could not have. Dakalebi's player has a quality
menu, and a quality menu has a current value: its local `.q-item.sel` painted the
chosen rendition red and bold. keel's dropdown item had `DropdownItemTone` and nothing
else, so moving that menu onto the component would have silently dropped the one piece
of state it carries - the migration would have looked complete and lost information.

Not folded into `DropdownItemTone`. Tone is how strongly an item reads; selection is
whether it is the value already in effect. They are different facts and one item can
carry both, so an enum entry would have made "selected" and "destructive" mutually
exclusive for no reason. `.dropdown__item--selected` is therefore its own class, and it
sits *before* `--danger` in the file so that an item which is somehow both still reads
destructive - that matters more than reading as current.

One flag sets the class, `aria-current` and the check, which is the point: the previous
implementation had a class and no announcement at all, so the state existed only for
people who could see it. The check itself is `aria-hidden` - "1080p, check mark" says
nothing that "1080p, current" does not - and is a real `Icon` rather than a `::after`
glyph, because content in this stylesheet cannot be replaced by a consumer without
restating the selector.

Measured in a browser on all four combinations:

| item | computed colour | `aria-current` |
|---|---|---|
| plain | `--foreground` | absent |
| selected | `--primary` | `true` |
| danger | `--destructive` | absent |
| selected + danger | `--destructive` | `true` |

`margin-left: auto` on the check resolves against the item's own width and lands it on
the padding edge - 13px in, which is `--control-px-sm` plus the border. It is on the
check rather than `justify-content: space-between` on the item, which would also have
pushed a `leading` icon away from the label it belongs to.
