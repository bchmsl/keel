package io.github.bchmsl.keel.editor

/**
 * The global `RegExp`.
 *
 * Declared here because `kotlin.js.RegExp` does not resolve on this Kotlin version,
 * and `js("new RegExp(pattern)")` cannot stand in: `js` takes a compile-time
 * constant, so it cannot see a pattern passed in as a parameter.
 */
@JsName("RegExp")
internal external class JsRegExp(pattern: String, flags: String = definedExternally)
