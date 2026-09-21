package io.github.asadullah012.foliate.compose.presentation

import androidx.compose.runtime.Immutable
import io.github.asadullah012.foliate.model.EpubAnnotation
import io.github.asadullah012.foliate.model.EpubBookmark
import io.github.asadullah012.foliate.model.EpubFootnote
import io.github.asadullah012.foliate.model.EpubReaderConfig
import io.github.asadullah012.foliate.model.EpubReaderSheet
import io.github.asadullah012.foliate.model.EpubSearchResult
import io.github.asadullah012.foliate.model.EpubTextSelection
import io.github.asadullah012.foliate.model.EpubTocItem

/**
 * Immutable UI state for the EPUB reader.
 */
@Immutable
public data class ReaderState(
    public val bookId: String = "",
    public val bookTitle: String = "",
    public val filePath: String = "",
    public val isLoading: Boolean = true,
    public val errorMessage: String? = null,
    public val isControlsVisible: Boolean = true,
    public val readerConfig: EpubReaderConfig = EpubReaderConfig(),
    public val activeSheet: EpubReaderSheet? = null,
    public val searchQuery: String = "",
    public val isSearching: Boolean = false,
    public val searchResults: List<EpubSearchResult> = emptyList(),
    public val bookmarks: List<EpubBookmark> = emptyList(),
    public val isBookmarked: Boolean = false,
    public val currentCfi: String? = null,
    public val currentChapterTitle: String? = null,
    public val currentSectionIndex: Int = 0,
    public val totalSections: Int = 1,
    public val progressFraction: Float = 0f,
    public val tocItems: List<EpubTocItem> = emptyList(),
    public val brightness: Float = 0.5f,
    public val isBrightnessOverlayVisible: Boolean = false,
    public val textSelection: EpubTextSelection? = null,
    public val annotations: List<EpubAnnotation> = emptyList(),
    public val activeFootnote: EpubFootnote? = null,
    public val selectedAnnotation: EpubAnnotation? = null
)
