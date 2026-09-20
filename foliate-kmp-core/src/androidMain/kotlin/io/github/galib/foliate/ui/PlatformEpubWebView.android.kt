package io.github.galib.foliate.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import io.github.galib.foliate.EpubReaderController
import io.github.galib.foliate.model.EpubAnnotation
import io.github.galib.foliate.model.EpubFootnote
import io.github.galib.foliate.model.EpubReaderConfig
import io.github.galib.foliate.model.EpubReaderLocation
import io.github.galib.foliate.model.EpubSearchResult
import io.github.galib.foliate.model.EpubTextSelection
import io.github.galib.foliate.model.EpubTocItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

private const val TAG = "Foliate-Android"
private const val READER_URL = "https://appassets.androidplatform.net/assets/foliate/index.html"
private const val BOOK_URL_PREFIX = "/book/"
private const val BOOK_URL = "https://appassets.androidplatform.net/book/book.epub"

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun PlatformEpubWebView(
    bookPath: String,
    initialCfi: String?,
    controller: EpubReaderController,
    config: EpubReaderConfig,
    modifier: Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val assetLoader = remember(context) {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }

    val webView = remember(context, bookPath) {
        var webViewRef: WebView? = null
        val wv = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            val bridge = AndroidEpubBridge(controller, scope, initialCfi) { webViewRef }
            addJavascriptInterface(bridge, "AndroidBridge")

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    if (consoleMessage != null) {
                        Log.d("Foliate-WebChrome", "[${consoleMessage.messageLevel()}] ${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})")
                    }
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url ?: return null

                    // 1. Intercept assets via WebViewAssetLoader
                    val assetResponse = assetLoader.shouldInterceptRequest(url)
                    if (assetResponse != null) return assetResponse

                    // 2. Intercept local book stream
                    if (url.host == "appassets.androidplatform.net" && url.path?.startsWith(BOOK_URL_PREFIX) == true) {
                        val (stream, resolvedPath) = resolveAndroidBookInputStream(context, bookPath)
                        if (stream != null) {
                            return WebResourceResponse(
                                "application/epub+zip",
                                null,
                                200,
                                "OK",
                                mapOf(
                                    "Access-Control-Allow-Origin" to "*",
                                    "Access-Control-Allow-Methods" to "GET, OPTIONS",
                                    "Access-Control-Allow-Headers" to "*"
                                ),
                                stream
                            )
                        } else {
                            Log.e(TAG, "Book file not found or unreadable: $resolvedPath")
                            scope.launch(Dispatchers.Main) {
                                controller.onError("Book file not found or unreadable: $resolvedPath")
                            }
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val cfiArg = if (!initialCfi.isNullOrBlank()) Json.encodeToString(initialCfi) else "null"
                    view?.evaluateJavascript(
                        "if (window.readerController && !window._bookOpened) { window.readerController.openBook('$BOOK_URL', $cfiArg); }",
                        null
                    )
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        controller.onError("Failed to load reader: ${error?.description}")
                    }
                }
            }

            loadUrl(READER_URL)
        }
        webViewRef = wv
        wv
    }

    LaunchedEffect(config, controller.isReady) {
        if (controller.isReady.value) {
            controller.applyConfig(config)
        }
    }

    DisposableEffect(webView) {
        controller.jsEvaluator = { script ->
            webView.post {
                webView.evaluateJavascript(script, null)
            }
        }
        onDispose {
            controller.jsEvaluator = null
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}

private fun resolveAndroidBookInputStream(context: Context, rawPath: String): Pair<InputStream?, String> {
    val clean = rawPath.removePrefix("file://")
    val direct = File(clean)
    if (direct.exists() && direct.canRead()) {
        return try {
            FileInputStream(direct) to direct.absolutePath
        } catch (e: Exception) {
            null to direct.absolutePath
        }
    }

    if (rawPath.startsWith("content://")) {
        return try {
            val uri = android.net.Uri.parse(rawPath)
            context.contentResolver.openInputStream(uri) to rawPath
        } catch (e: Exception) {
            null to rawPath
        }
    }

    val candidateFiles = File(context.filesDir, clean.trimStart('/'))
    if (candidateFiles.exists() && candidateFiles.canRead()) {
        return try {
            FileInputStream(candidateFiles) to candidateFiles.absolutePath
        } catch (e: Exception) {
            null to candidateFiles.absolutePath
        }
    }

    val candidateCache = File(context.cacheDir, clean.trimStart('/'))
    if (candidateCache.exists() && candidateCache.canRead()) {
        return try {
            FileInputStream(candidateCache) to candidateCache.absolutePath
        } catch (e: Exception) {
            null to candidateCache.absolutePath
        }
    }

    return null to direct.absolutePath
}

internal class AndroidEpubBridge(
    private val controller: EpubReaderController,
    private val scope: CoroutineScope,
    private val initialCfi: String?,
    private val getWebView: () -> WebView?
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @JavascriptInterface
    fun postMessage(message: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val element = json.parseToJsonElement(message).jsonObject
                val type = element["type"]?.jsonPrimitive?.contentOrNull
                when (type) {
                    "initialized" -> {
                        val cfiArg = if (!initialCfi.isNullOrBlank()) json.encodeToString(initialCfi) else "null"
                        val js = "window.readerController && window.readerController.openBook('$BOOK_URL', $cfiArg)"
                        getWebView()?.evaluateJavascript(js, null)
                    }
                    "log" -> {
                        val level = element["level"]?.jsonPrimitive?.contentOrNull ?: "log"
                        val msg = element["message"]?.jsonPrimitive?.contentOrNull ?: ""
                        when (level.lowercase()) {
                            "error" -> Log.e("Foliate-JS", msg)
                            "warn" -> Log.w("Foliate-JS", msg)
                            "info" -> Log.i("Foliate-JS", msg)
                            else -> Log.d("Foliate-JS", msg)
                        }
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
                        Log.d(TAG, "Unknown bridge message type: $type")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing script message: ${e.message}", e)
            }
        }
    }
}
