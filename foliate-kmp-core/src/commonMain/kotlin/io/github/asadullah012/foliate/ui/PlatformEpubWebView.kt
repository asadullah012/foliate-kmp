package io.github.asadullah012.foliate.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.asadullah012.foliate.EpubReaderController

/**
 * Platform seam for rendering the web view on Android and iOS.
 *
 * The platform view does not take the reader configuration. [EpubReader] applies the
 * configuration through the controller each time the reader becomes ready.
 */
@Composable
internal expect fun PlatformEpubWebView(
    bookPath: String,
    initialCfi: String?,
    controller: EpubReaderController,
    modifier: Modifier
)
