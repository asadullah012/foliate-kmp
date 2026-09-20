package io.github.galib.foliate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.galib.foliate.EpubReaderController
import io.github.galib.foliate.model.EpubReaderConfig

/**
 * Multiplatform EPUB Reader Composable embedding the foliate-js engine.
 *
 * @param bookPath Local filesystem path or asset identifier for the .epub archive.
 * @param controller Controller used to send navigation commands and observe reader state.
 * @param config Visual configuration (theme, font size, flow).
 * @param initialCfi Optional CFI location to restore reading progress.
 * @param modifier Composable layout modifier.
 */
@Composable
public fun EpubReader(
    bookPath: String,
    controller: EpubReaderController,
    config: EpubReaderConfig = EpubReaderConfig(),
    initialCfi: String? = null,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(config.theme) {
        controller.setTheme(config.theme)
    }

    LaunchedEffect(config.fontSize) {
        controller.setFontSize(config.fontSize)
    }

    LaunchedEffect(config.fontFamily) {
        controller.setFontFamily(config.fontFamily)
    }

    LaunchedEffect(config.flow) {
        controller.setLayout(config.flow)
    }

    LaunchedEffect(config.lineHeight) {
        controller.setLineHeight(config.lineHeight)
    }

    LaunchedEffect(config.textAlign) {
        controller.setTextAlign(config.textAlign)
    }

    LaunchedEffect(config.margin) {
        controller.setMargin(config.margin)
    }

    val backgroundColor = remember(config.theme) {
        val hex = config.theme.backgroundColor.removePrefix("#")
        val colorInt = hex.toLongOrNull(16) ?: 0xFFFFFFFF
        Color(0xFF000000 or colorInt)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        PlatformEpubWebView(
            bookPath = bookPath,
            initialCfi = initialCfi,
            controller = controller,
            config = config,
            modifier = Modifier.fillMaxSize()
        )
    }
}
