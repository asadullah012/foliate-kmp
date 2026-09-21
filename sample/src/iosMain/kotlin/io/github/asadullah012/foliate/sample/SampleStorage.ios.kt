package io.github.asadullah012.foliate.sample

import okio.FileSystem
import okio.Path.Companion.toPath

internal actual fun saveSampleEpub(bytes: ByteArray, fileName: String): String {
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
    val target = dir / fileName
    if (!FileSystem.SYSTEM.exists(target)) {
        FileSystem.SYSTEM.write(target) {
            write(bytes)
        }
    }
    return target.toString()
}
