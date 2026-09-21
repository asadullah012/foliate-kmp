package io.github.asadullah012.foliate.internal

import platform.Foundation.NSProcessInfo

/**
 * Reads the `FOLIATE_DEBUG` environment variable.
 *
 * Add the variable to the Xcode scheme to see the engine messages on one device.
 */
internal actual fun isVerboseLoggingEnabled(): Boolean =
    NSProcessInfo.processInfo.environment["FOLIATE_DEBUG"] != null
