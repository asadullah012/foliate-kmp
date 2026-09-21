package io.github.asadullah012.foliate.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.asadullah012.foliate.EpubReaderController
import io.github.asadullah012.foliate.model.EpubReaderConfig

/**
 * Headless Compose Multiplatform view for rendering an EPUB via Foliate.js.
 * Alias for [EpubReader].
 */
@Composable
public fun FoliateReaderView(
    bookPath: String,
    controller: EpubReaderController,
    config: EpubReaderConfig = EpubReaderConfig(),
    initialCfi: String? = null,
    modifier: Modifier = Modifier
) {
    EpubReader(
        bookPath = bookPath,
        controller = controller,
        config = config,
        initialCfi = initialCfi,
        modifier = modifier
    )
}
