package io.github.asadullah012.foliate.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class FoliateAssetsTest {

    @Test
    fun `the entry point is the reader page`() {
        assertEquals("index.html", FoliateAssets.ENTRY_POINT)
    }

    @Test
    fun `each engine file gets the correct MIME type`() {
        assertEquals("text/html", FoliateAssets.mimeTypeFor("index.html"))
        assertEquals("text/javascript", FoliateAssets.mimeTypeFor("view.js"))
        assertEquals("text/javascript", FoliateAssets.mimeTypeFor("vendor/zip.js"))
        assertEquals("text/css", FoliateAssets.mimeTypeFor("reader.css"))
        assertEquals("application/epub+zip", FoliateAssets.mimeTypeFor("book.epub"))
    }

    @Test
    fun `the MIME type holds no charset parameter`() {
        // Android reads the MIME type and the character set through two separate
        // arguments of WebResourceResponse. A charset inside the MIME type argument
        // stops the page from rendering.
        val paths = listOf("index.html", "view.js", "reader.css", "data.json", "icon.svg")
        for (path in paths) {
            assertFalse(
                FoliateAssets.mimeTypeFor(path).contains(";"),
                "The MIME type of $path must hold no parameter"
            )
        }
    }

    @Test
    fun `a text file gets utf-8 and a binary file gets none`() {
        assertEquals("utf-8", FoliateAssets.charsetFor("index.html"))
        assertEquals("utf-8", FoliateAssets.charsetFor("view.js"))
        assertEquals("utf-8", FoliateAssets.charsetFor("reader.css"))
        assertEquals("utf-8", FoliateAssets.charsetFor("data.json"))
        assertNull(FoliateAssets.charsetFor("book.epub"))
        assertNull(FoliateAssets.charsetFor("cover.png"))
        assertNull(FoliateAssets.charsetFor("font.woff2"))
    }

    @Test
    fun `the content type joins the MIME type and the charset`() {
        assertEquals("text/html; charset=utf-8", FoliateAssets.contentTypeFor("index.html"))
        assertEquals("text/javascript; charset=utf-8", FoliateAssets.contentTypeFor("view.js"))
        assertEquals("application/epub+zip", FoliateAssets.contentTypeFor("book.epub"))
        assertEquals("image/png", FoliateAssets.contentTypeFor("cover.png"))
    }

    @Test
    fun `the MIME type ignores the letter case of the extension`() {
        assertEquals("image/png", FoliateAssets.mimeTypeFor("Cover.PNG"))
        assertEquals("image/jpeg", FoliateAssets.mimeTypeFor("photo.JPEG"))
    }

    @Test
    fun `an unknown extension gets a neutral MIME type`() {
        assertEquals("application/octet-stream", FoliateAssets.mimeTypeFor("data.bin"))
        assertEquals("application/octet-stream", FoliateAssets.mimeTypeFor("noextension"))
    }
}
