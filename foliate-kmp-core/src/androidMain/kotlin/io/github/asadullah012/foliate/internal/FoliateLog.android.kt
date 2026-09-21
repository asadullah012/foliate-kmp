package io.github.asadullah012.foliate.internal

import android.util.Log

/**
 * Reads the standard Android log switch for this tag.
 *
 * The switch is off in a release build. Turn it on for one device with
 * `adb shell setprop log.tag.Foliate DEBUG`.
 */
internal actual fun isVerboseLoggingEnabled(): Boolean =
    Log.isLoggable(FOLIATE_LOG_TAG, Log.DEBUG)
