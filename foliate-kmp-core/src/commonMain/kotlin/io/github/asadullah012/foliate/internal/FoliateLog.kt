package io.github.asadullah012.foliate.internal

/** The tag that every log line of this library carries. */
internal const val FOLIATE_LOG_TAG: String = "Foliate"

/**
 * Tells whether the library may log the content of a publication.
 *
 * The reader runs an untrusted publication inside a web view. Console output and
 * engine messages hold text from that publication, so a release build must not write
 * them to the device log. A developer turns them on for one device:
 *
 * - Android: `adb shell setprop log.tag.Foliate DEBUG`
 * - iOS: set the environment variable `FOLIATE_DEBUG` in the Xcode scheme
 *
 * A warning or an error always reaches the log. Those lines carry no publication text.
 */
internal expect fun isVerboseLoggingEnabled(): Boolean
