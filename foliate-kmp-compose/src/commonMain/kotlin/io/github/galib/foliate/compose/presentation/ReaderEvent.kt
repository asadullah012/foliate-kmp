package io.github.galib.foliate.compose.presentation

import androidx.compose.runtime.Immutable
import io.github.galib.foliate.model.EpubFootnote
import io.github.galib.foliate.model.EpubReaderFlow
import io.github.galib.foliate.model.EpubReaderSheet
import io.github.galib.foliate.model.EpubReaderTheme
import io.github.galib.foliate.model.EpubSearchResult
import io.github.galib.foliate.model.EpubTextSelection
import io.github.galib.foliate.model.EpubTocItem

/**
 * Sealed interface representing all user, engine, and lifecycle actions in the reader.
 */
@Immutable
public sealed interface ReaderEvent {
    public data class Init(
        val bookId: String,
        val filePath: String,
        val bookTitle: String = "",
        val initialCfi: String? = null,
        val initialProgress: Float = 0f
    ) : ReaderEvent

    public data object OnNavigateBack : ReaderEvent
    public data object ToggleControls : ReaderEvent
    public data class UpdateProgress(val progress: Float) : ReaderEvent

    public data class ChangeTheme(val theme: EpubReaderTheme) : ReaderEvent
    public data class ChangeFontSize(val delta: Int) : ReaderEvent
    public data class SetFontSize(val sizePx: Int) : ReaderEvent
    public data class ChangeFlow(val flow: EpubReaderFlow) : ReaderEvent
    public data class ChangeLineHeight(val lineHeight: Float) : ReaderEvent
    public data class ChangeMargin(val margin: Int) : ReaderEvent
    public data class ChangeTextAlign(val align: String) : ReaderEvent
    public data class ChangeFontFamily(val family: String) : ReaderEvent
    public data object ResetAppearance : ReaderEvent

    public data class OpenSheet(val sheet: EpubReaderSheet) : ReaderEvent
    public data object CloseSheet : ReaderEvent

    public data class SearchQueryChanged(val query: String) : ReaderEvent
    public data class SearchResultsLoaded(val results: List<EpubSearchResult>) : ReaderEvent

    public data class LocationUpdated(
        val cfi: String,
        val fraction: Float,
        val chapterTitle: String?,
        val sectionIndex: Int,
        val totalSections: Int
    ) : ReaderEvent

    public data class TocLoaded(val items: List<EpubTocItem>) : ReaderEvent

    public data object ToggleBookmark : ReaderEvent
    public data class DeleteBookmark(val cfi: String) : ReaderEvent

    public data class AdjustBrightness(val delta: Float) : ReaderEvent
    public data object EndBrightness : ReaderEvent

    public data class TextSelectionChanged(val selection: EpubTextSelection?) : ReaderEvent
    public data class AddHighlight(val color: String, val note: String? = null) : ReaderEvent
    public data class DeleteHighlight(val cfi: String) : ReaderEvent
    public data object ClearSelection : ReaderEvent

    public data class OnAnnotationClicked(val cfi: String) : ReaderEvent
    public data object DismissAnnotationDetail : ReaderEvent

    public data class ShowFootnote(val footnote: EpubFootnote) : ReaderEvent
    public data object DismissFootnote : ReaderEvent

    public data class OnError(val message: String) : ReaderEvent
}
