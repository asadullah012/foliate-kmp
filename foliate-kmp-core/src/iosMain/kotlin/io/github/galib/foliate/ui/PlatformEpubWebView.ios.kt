package io.github.galib.foliate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import io.github.galib.foliate.EpubReaderController
import io.github.galib.foliate.model.EpubAnnotation
import io.github.galib.foliate.model.EpubFootnote
import io.github.galib.foliate.model.EpubReaderConfig
import io.github.galib.foliate.model.EpubReaderLocation
import io.github.galib.foliate.model.EpubSearchResult
import io.github.galib.foliate.model.EpubTextSelection
import io.github.galib.foliate.model.EpubTocItem
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.cValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.CoreGraphics.CGRect
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKURLSchemeHandlerProtocol
import platform.WebKit.WKURLSchemeTaskProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

private const val FOLIATE_SCHEME = "foliate"
private const val READER_URL = "foliate://localhost/foliate/index.html"
private const val BOOK_URL = "foliate://localhost/book/book.epub"

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun PlatformEpubWebView(
    bookPath: String,
    initialCfi: String?,
    controller: EpubReaderController,
    config: EpubReaderConfig,
    modifier: Modifier
) {
    val scope = rememberCoroutineScope()

    val webView = remember(bookPath) {
        var webViewRef: WKWebView? = null
        val configuration = WKWebViewConfiguration()
        val contentController = WKUserContentController()
        val scriptHandler = IosEpubScriptMessageHandler(controller, scope, initialCfi) { webViewRef }
        contentController.addScriptMessageHandler(scriptHandler, "epubBridge")
        configuration.userContentController = contentController

        val schemeHandler = FoliateSchemeHandler(bookPath, controller)
        configuration.setURLSchemeHandler(schemeHandler, forURLScheme = FOLIATE_SCHEME)

        val wv = WKWebView(frame = cValue<CGRect>(), configuration = configuration)
        webViewRef = wv

        wv.apply {
            navigationDelegate = object : NSObject(), WKNavigationDelegateProtocol {
                @ObjCSignatureOverride
                override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
                    // Navigation started
                }

                @ObjCSignatureOverride
                override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                    val cfiArg = if (!initialCfi.isNullOrBlank()) "'${initialCfi.replace("'", "\\'")}'" else "null"
                    val js = "if (window.readerController && !window._bookOpened) { window.readerController.openBook('$BOOK_URL', $cfiArg); }"
                    webView.evaluateJavaScript(js, null)
                }

                @ObjCSignatureOverride
                override fun webView(
                    webView: WKWebView,
                    didFailProvisionalNavigation: WKNavigation?,
                    withError: NSError
                ) {
                    controller.onError("Failed to load reader: ${withError.localizedDescription}")
                }

                @ObjCSignatureOverride
                override fun webView(
                    webView: WKWebView,
                    didFailNavigation: WKNavigation?,
                    withError: NSError
                ) {
                    controller.onError("Navigation error: ${withError.localizedDescription}")
                }
            }
        }
    }

    LaunchedEffect(config, controller.isReady) {
        if (controller.isReady.value) {
            controller.applyConfig(config)
        }
    }

    DisposableEffect(webView) {
        controller.jsEvaluator = { script ->
            Dispatchers.Main.let {
                webView.evaluateJavaScript(script, null)
            }
        }

        val request = NSURLRequest.requestWithURL(NSURL.URLWithString(READER_URL)!!)
        webView.loadRequest(request)

        onDispose {
            controller.jsEvaluator = null
            webView.configuration.userContentController.removeScriptMessageHandlerForName("epubBridge")
            webView.stopLoading()
        }
    }

    UIKitView(
        factory = { webView },
        modifier = modifier
    )
}

@OptIn(ExperimentalForeignApi::class)
private class FoliateSchemeHandler(
    private val bookPath: String,
    private val controller: EpubReaderController
) : NSObject(), WKURLSchemeHandlerProtocol {

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, startURLSchemeTask: WKURLSchemeTaskProtocol) {
        val url = startURLSchemeTask.request.URL ?: run {
            startURLSchemeTask.didFailWithError(
                NSError.errorWithDomain("FoliateScheme", 400, null)
            )
            return
        }

        val path = url.path ?: ""

        // 1. Intercept Book EPUB Request
        if (path.startsWith("/book/") || path == "/book.epub" || path.endsWith(".epub")) {
            val resolvedPath = resolveBookFilePath(bookPath)
            val fileManager = NSFileManager.defaultManager

            if (!fileManager.fileExistsAtPath(resolvedPath)) {
                val error = NSError.errorWithDomain("FoliateScheme", 404, null)
                startURLSchemeTask.didFailWithError(error)
                controller.onError("Book file not found: $resolvedPath")
                return
            }

            val data = NSData.dataWithContentsOfFile(resolvedPath)
            if (data == null) {
                val error = NSError.errorWithDomain("FoliateScheme", 500, null)
                startURLSchemeTask.didFailWithError(error)
                controller.onError("Could not read book file: $resolvedPath")
                return
            }

            val response = NSHTTPURLResponse(
                uRL = url,
                statusCode = 200,
                HTTPVersion = "HTTP/1.1",
                headerFields = mapOf(
                    "Content-Type" to "application/epub+zip",
                    "Content-Length" to "${data.length}",
                    "Access-Control-Allow-Origin" to "*",
                    "Access-Control-Allow-Methods" to "GET, HEAD, OPTIONS",
                    "Access-Control-Allow-Headers" to "*"
                )
            )
            startURLSchemeTask.didReceiveResponse(response)
            startURLSchemeTask.didReceiveData(data)
            startURLSchemeTask.didFinish()
            return
        }

        // 2. Intercept Foliate Reader Assets
        val assetData = findFoliateAsset(path)
        if (assetData != null) {
            val mimeType = mimeTypeForPath(path)
            val response = NSHTTPURLResponse(
                uRL = url,
                statusCode = 200,
                HTTPVersion = "HTTP/1.1",
                headerFields = mapOf(
                    "Content-Type" to mimeType,
                    "Content-Length" to "${assetData.length}",
                    "Access-Control-Allow-Origin" to "*",
                    "Cache-Control" to "no-cache"
                )
            )
            startURLSchemeTask.didReceiveResponse(response)
            startURLSchemeTask.didReceiveData(assetData)
            startURLSchemeTask.didFinish()
            return
        }

        val error = NSError.errorWithDomain("FoliateScheme", 404, null)
        startURLSchemeTask.didFailWithError(error)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, stopURLSchemeTask: WKURLSchemeTaskProtocol) {
        // Synchronous responses require no abort logic
    }

    private fun findFoliateAsset(path: String): NSData? {
        val cleanPath = path.removePrefix("/foliate/").removePrefix("/")
        val targetName = if (cleanPath.isEmpty()) "index.html" else cleanPath
        val fileManager = NSFileManager.defaultManager
        val bundlePath = NSBundle.mainBundle.resourcePath ?: return null

        val candidates = listOf(
            "$bundlePath/compose-resources/composeResources/foliate_kmp.foliate_kmp_core.generated.resources/files/foliate/$targetName",
            "$bundlePath/compose-resources/composeResources/foliate_kmp_core.generated.resources/files/foliate/$targetName",
            "$bundlePath/compose-resources/composeResources/e_library.epubreader.generated.resources/files/foliate/$targetName",
            "$bundlePath/compose-resources/files/foliate/$targetName",
            "$bundlePath/foliate/$targetName",
            "$bundlePath/$targetName",
            NSTemporaryDirectory() + "foliate/$targetName"
        )

        for (candidate in candidates) {
            if (fileManager.fileExistsAtPath(candidate)) {
                return NSData.dataWithContentsOfFile(candidate)
            }
        }
        return null
    }

    private fun resolveBookFilePath(rawPath: String): String {
        val clean = rawPath.removePrefix("file://")
        val decoded = NSURL.URLWithString("file://$clean")?.path ?: clean
        val fileManager = NSFileManager.defaultManager

        if (fileManager.fileExistsAtPath(decoded)) {
            return decoded
        }

        if (fileManager.fileExistsAtPath(clean)) {
            return clean
        }

        val relative = clean.trimStart('/')
        val decodedRelative = NSURL.URLWithString("file:///$relative")?.path?.removePrefix("/") ?: relative

        val searchDomains = listOf(NSApplicationSupportDirectory, NSDocumentDirectory, NSCachesDirectory)
        for (domain in searchDomains) {
            val dirPaths = NSSearchPathForDirectoriesInDomains(domain, NSUserDomainMask, true)
            val dir = dirPaths.firstOrNull() as? String ?: continue
            for (rel in listOf(decodedRelative, relative)) {
                val candidateDirect = "$dir/$rel"
                if (fileManager.fileExistsAtPath(candidateDirect)) return candidateDirect
            }
        }

        val tempCandidate = NSTemporaryDirectory() + relative
        if (fileManager.fileExistsAtPath(tempCandidate)) return tempCandidate

        return decoded
    }

    private fun mimeTypeForPath(path: String): String {
        return when {
            path.endsWith(".html", ignoreCase = true) -> "text/html; charset=utf-8"
            path.endsWith(".js", ignoreCase = true) || path.endsWith(".mjs", ignoreCase = true) -> "application/javascript; charset=utf-8"
            path.endsWith(".css", ignoreCase = true) -> "text/css; charset=utf-8"
            path.endsWith(".json", ignoreCase = true) -> "application/json; charset=utf-8"
            path.endsWith(".epub", ignoreCase = true) -> "application/epub+zip"
            path.endsWith(".png", ignoreCase = true) -> "image/png"
            path.endsWith(".jpg", ignoreCase = true) || path.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            path.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
            path.endsWith(".woff2", ignoreCase = true) -> "font/woff2"
            path.endsWith(".woff", ignoreCase = true) -> "font/woff"
            path.endsWith(".ttf", ignoreCase = true) -> "font/ttf"
            else -> "application/octet-stream"
        }
    }
}

private class IosEpubScriptMessageHandler(
    private val controller: EpubReaderController,
    private val scope: CoroutineScope,
    private val initialCfi: String?,
    private val getWebView: () -> WKWebView?
) : NSObject(), WKScriptMessageHandlerProtocol {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage
    ) {
        val body = didReceiveScriptMessage.body as? String ?: return
        scope.launch(Dispatchers.Main) {
            try {
                val element = json.parseToJsonElement(body).jsonObject
                val type = element["type"]?.jsonPrimitive?.contentOrNull
                when (type) {
                    "initialized" -> {
                        val cfiArg = if (!initialCfi.isNullOrBlank()) "'${initialCfi.replace("'", "\\'")}'" else "null"
                        val js = "window.readerController && window.readerController.openBook('$BOOK_URL', $cfiArg)"
                        getWebView()?.evaluateJavaScript(js, null)
                    }
                    "log" -> {
                        val level = element["level"]?.jsonPrimitive?.contentOrNull ?: "log"
                        val msg = element["message"]?.jsonPrimitive?.contentOrNull ?: ""
                        println("[Foliate-JS][$level] $msg")
                    }
                    "relocate" -> {
                        val cfi = element["cfi"]?.jsonPrimitive?.contentOrNull
                        val fraction = element["fraction"]?.jsonPrimitive?.floatOrNull ?: 0f
                        val chapterTitle = element["chapterTitle"]?.jsonPrimitive?.contentOrNull
                        val sectionIndex = element["sectionIndex"]?.jsonPrimitive?.intOrNull ?: 0
                        val totalSections = element["totalSections"]?.jsonPrimitive?.intOrNull ?: 1
                        controller.onRelocate(
                            EpubReaderLocation(
                                cfi = cfi,
                                progressFraction = fraction,
                                chapterTitle = chapterTitle,
                                sectionIndex = sectionIndex,
                                totalSections = totalSections
                            )
                        )
                    }
                    "toc" -> {
                        val items = element["items"]?.jsonArray
                        if (items != null) {
                            val tocList = json.decodeFromJsonElement<List<EpubTocItem>>(items)
                            controller.onTocLoaded(tocList)
                        }
                    }
                    "ready" -> {
                        controller.onReady()
                    }
                    "error" -> {
                        val msg = element["message"]?.jsonPrimitive?.contentOrNull ?: "Error reading EPUB"
                        controller.onError(msg)
                    }
                    "toggle_controls" -> {
                        controller.onToggleControls()
                    }
                    "search_results" -> {
                        val items = element["items"]?.jsonArray
                        if (items != null) {
                            val results = json.decodeFromJsonElement<List<EpubSearchResult>>(items)
                            controller.onSearchResults(results)
                        } else {
                            controller.onSearchResults(emptyList())
                        }
                    }
                    "adjust_brightness" -> {
                        val delta = element["delta"]?.jsonPrimitive?.floatOrNull ?: 0f
                        controller.onAdjustBrightness(delta)
                    }
                    "end_brightness" -> {
                        controller.onEndBrightness()
                    }
                    "selection" -> {
                        val text = element["text"]?.jsonPrimitive?.contentOrNull
                        val cfi = element["cfi"]?.jsonPrimitive?.contentOrNull
                        val sectionIndex = element["sectionIndex"]?.jsonPrimitive?.intOrNull ?: 0
                        if (!text.isNullOrBlank() && !cfi.isNullOrBlank()) {
                            controller.onSelection(EpubTextSelection(text = text, cfi = cfi, sectionIndex = sectionIndex))
                        } else {
                            controller.onSelection(null)
                        }
                    }
                    "footnote" -> {
                        val text = element["text"]?.jsonPrimitive?.contentOrNull ?: ""
                        val href = element["href"]?.jsonPrimitive?.contentOrNull ?: ""
                        val title = element["title"]?.jsonPrimitive?.contentOrNull
                        if (text.isNotBlank()) {
                            controller.onFootnote(EpubFootnote(text = text, href = href, title = title))
                        }
                    }
                    "annotation_click" -> {
                        val cfi = element["cfi"]?.jsonPrimitive?.contentOrNull
                        if (!cfi.isNullOrBlank()) {
                            controller.onAnnotationClick(cfi)
                        }
                    }
                    else -> {
                        println("[Foliate-iOS] Unknown bridge message type: $type")
                    }
                }
            } catch (e: Exception) {
                println("[Foliate-iOS] Error parsing script message: ${e.message}")
            }
        }
    }
}
