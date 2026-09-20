package io.github.galib.foliate.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.galib.foliate.EpubReaderController
import io.github.galib.foliate.model.EpubReaderConfig

/**
 * Platform seam for rendering the web view on Android and iOS.
 */
@Composable
internal expect fun PlatformEpubWebView(
    bookPath: String,
    initialCfi: String?,
    controller: EpubReaderController,
    config: EpubReaderConfig,
    modifier: Modifier
)
