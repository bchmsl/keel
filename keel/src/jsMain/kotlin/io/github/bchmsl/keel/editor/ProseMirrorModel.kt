@file:JsModule("prosemirror-model")

package io.github.bchmsl.keel.editor

/**
 * The document model: a schema, and the nodes and marks it allows.
 *
 * ------------------------------------------------------------------ the shape ----
 *
 * `@file:JsModule` makes every declaration in the file a *named* export of the
 * module, which is the shape ProseMirror ships. There is no default export and no
 * global build, and that second half is why `:keel` compiles as CommonJS rather than
 * UMD: a UMD build demands `@JsNonModule` beside `@JsModule`, and there is no global
 * to point it at. The note on `moduleKind` in this module's `build.gradle.kts` has
 * the rest.
 *
 * ------------------------------------------------------------------- internal ----
 *
 * Not tidiness. A `public` external would put a ProseMirror type into keel's own
 * signatures, and then every consumer would need the npm package on its compile
 * classpath just to name the arguments it passes. The engine is an implementation
 * detail of one component whose contract is marker text in, marker text out, and it
 * has to stay one - that property is what lets a consumer that never opens a
 * formatting field pay nothing for it.
 *
 * ------------------------------------------------------------------- dynamic ----
 *
 * Members are `dynamic` throughout. The alternative is a hand-written binding for a
 * library whose own types are large and whose surface keel touches is small, and the
 * cost of getting one of these wrong is a runtime error in a component the gallery
 * exercises on every build. Where a mistake here would be *quiet* rather than loud,
 * the arithmetic is pulled out into ordinary Kotlin and tested: see `markerCut`.
 */
internal external class Schema(spec: dynamic) {
    val nodes: dynamic
    val marks: dynamic

    /** A document from ProseMirror's own JSON, which is what the bridge produces. */
    fun nodeFromJSON(json: dynamic): dynamic
}
