package io.github.asadullah012.foliate.internal

import io.github.asadullah012.foliate.resources.Res

/**
 * Locates the bundled foliate-js engine files.
 *
 * The engine files live in `commonMain/composeResources/files/foliate`. The Compose
 * resource system packs them into the Android AAR and into the iOS framework, and it
 * resolves the platform path at runtime. Do not build these paths by hand. A hand
 * built path breaks as soon as the Maven coordinates or the packaging change.
 */
internal object FoliateAssets {

    /** The page that hosts the reader. */
    const val ENTRY_POINT: String = "index.html"

    private const val RESOURCE_ROOT = "files/foliate"

    /**
     * Returns the platform URI of one engine file.
     *
     * @param name The file name and any sub directory, for example `vendor/zip.js`.
     * @return A `file://` URI on both Android and iOS.
     */
    fun uriFor(name: String): String = Res.getUri("$RESOURCE_ROOT/$name")

    /**
     * Returns the MIME type for one engine file or one file inside a publication.
     *
     * The result never holds a `charset` parameter. Android reads the MIME type and
     * the character set through two separate arguments of `WebResourceResponse`, and
     * it does not parse a parameter out of the MIME type. Use [charsetFor] to get the
     * character set, and compose the two only where an HTTP header needs one string.
     *
     * @param path The file path or file name.
     * @return The MIME type, or `application/octet-stream` for an unknown extension.
     */
    fun mimeTypeFor(path: String): String = when {
        path.endsWith(".html", ignoreCase = true) -> "text/html"
        path.endsWith(".xhtml", ignoreCase = true) -> "application/xhtml+xml"
        path.endsWith(".js", ignoreCase = true) -> "text/javascript"
        path.endsWith(".mjs", ignoreCase = true) -> "text/javascript"
        path.endsWith(".css", ignoreCase = true) -> "text/css"
        path.endsWith(".json", ignoreCase = true) -> "application/json"
        path.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
        path.endsWith(".md", ignoreCase = true) -> "text/plain"
        path.endsWith(".epub", ignoreCase = true) -> "application/epub+zip"
        path.endsWith(".png", ignoreCase = true) -> "image/png"
        path.endsWith(".jpg", ignoreCase = true) -> "image/jpeg"
        path.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
        path.endsWith(".gif", ignoreCase = true) -> "image/gif"
        path.endsWith(".webp", ignoreCase = true) -> "image/webp"
        path.endsWith(".woff2", ignoreCase = true) -> "font/woff2"
        path.endsWith(".woff", ignoreCase = true) -> "font/woff"
        path.endsWith(".ttf", ignoreCase = true) -> "font/ttf"
        path.endsWith(".otf", ignoreCase = true) -> "font/otf"
        else -> "application/octet-stream"
    }

    /**
     * Returns the character set for one file, or `null` for a binary file.
     *
     * @param path The file path or file name.
     * @return `"utf-8"` for a text file, `null` for every other file.
     */
    fun charsetFor(path: String): String? {
        val mimeType = mimeTypeFor(path)
        val isText = mimeType.startsWith("text/") ||
            mimeType == "application/xhtml+xml" ||
            mimeType == "application/json" ||
            mimeType == "image/svg+xml"
        return if (isText) "utf-8" else null
    }

    /**
     * Returns the value of one `Content-Type` HTTP header.
     *
     * An HTTP header holds the MIME type and the character set in one string. Use
     * this for the iOS URL scheme handler, which answers with a real HTTP response.
     *
     * @param path The file path or file name.
     * @return For example `text/html; charset=utf-8` or `image/png`.
     */
    fun contentTypeFor(path: String): String {
        val mimeType = mimeTypeFor(path)
        val charset = charsetFor(path) ?: return mimeType
        return "$mimeType; charset=$charset"
    }
}
