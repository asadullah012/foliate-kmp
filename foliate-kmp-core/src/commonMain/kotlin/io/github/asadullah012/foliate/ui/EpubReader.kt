package io.github.asadullah012.foliate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.asadullah012.foliate.EpubReaderController
import io.github.asadullah012.foliate.model.EpubReaderConfig

/**
 * Multiplatform EPUB Reader Composable embedding the foliate-js engine.
 *
 * @param bookPath Local filesystem path or asset identifier for the .epub archive.
 * @param controller Controller used to send navigation commands and observe reader state.
 * @param config Visual configuration (theme, font size, flow).
 * @param initialCfi Optional CFI location to restore reading progress. The reader reads
 *   this value once, when it opens the publication. A later change has no effect.
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
    val isReady by controller.isReady.collectAsState()

    // One effect applies the whole configuration. It runs when the configuration
    // changes and again each time the reader becomes ready, which also covers a new
    // web view after a change of publication.
    LaunchedEffect(config, isReady) {
        if (isReady) {
            controller.applyConfig(config)
        }
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
            modifier = Modifier.fillMaxSize()
        )
    }
}
