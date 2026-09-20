package io.github.galib.foliate.compose.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.galib.foliate.domain.EpubReaderStorage
import io.github.galib.foliate.domain.InMemoryEpubReaderStorage
import io.github.galib.foliate.model.EpubAnnotation
import io.github.galib.foliate.model.EpubBookmark
import io.github.galib.foliate.model.EpubReaderConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Production-ready MVI ViewModel managing the lifecycle, configuration,
 * annotations, bookmarks, and UI state of the EPUB reader.
 */
public open class ReaderModel(
    private val storage: EpubReaderStorage = InMemoryEpubReaderStorage(),
    private val clock: Clock = Clock.System
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    public val state: StateFlow<ReaderState> = _state.asStateFlow()

    private val _effects = Channel<ReaderEffect>(Channel.BUFFERED)
    public val effects: Flow<ReaderEffect> = _effects.receiveAsFlow()

    private var brightnessDismissJob: Job? = null

    public fun onEvent(event: ReaderEvent) {
        when (event) {
            is ReaderEvent.Init -> viewModelScope.launch { handleInit(event) }
            is ReaderEvent.OnNavigateBack -> viewModelScope.launch { _effects.send(ReaderEffect.OnNavigateBack) }
            is ReaderEvent.ToggleControls -> handleToggleControls()
            is ReaderEvent.UpdateProgress -> handleUpdateProgress(event.progress)
            is ReaderEvent.LocationUpdated -> handleLocationUpdated(event)
            is ReaderEvent.TocLoaded -> _state.update { it.copy(tocItems = event.items) }
            is ReaderEvent.ChangeTheme -> _state.update { it.copy(readerConfig = it.readerConfig.copy(theme = event.theme)) }
            is ReaderEvent.ChangeFontSize -> handleChangeFontSize(event.delta)
            is ReaderEvent.SetFontSize -> handleSetFontSize(event.sizePx)
            is ReaderEvent.ChangeFlow -> _state.update { it.copy(readerConfig = it.readerConfig.copy(flow = event.flow)) }
            is ReaderEvent.ChangeLineHeight -> _state.update { it.copy(readerConfig = it.readerConfig.copy(lineHeight = event.lineHeight)) }
            is ReaderEvent.ChangeMargin -> _state.update { it.copy(readerConfig = it.readerConfig.copy(margin = event.margin)) }
            is ReaderEvent.ChangeTextAlign -> _state.update { it.copy(readerConfig = it.readerConfig.copy(textAlign = event.align)) }
            is ReaderEvent.ChangeFontFamily -> _state.update { it.copy(readerConfig = it.readerConfig.copy(fontFamily = event.family)) }
            is ReaderEvent.ResetAppearance -> _state.update { it.copy(readerConfig = EpubReaderConfig()) }
            is ReaderEvent.OpenSheet -> _state.update { it.copy(activeSheet = event.sheet) }
            is ReaderEvent.CloseSheet -> _state.update { it.copy(activeSheet = null) }
            is ReaderEvent.SearchQueryChanged -> _state.update { it.copy(searchQuery = event.query) }
            is ReaderEvent.SearchResultsLoaded -> _state.update { it.copy(searchResults = event.results, isSearching = false) }
            is ReaderEvent.ToggleBookmark -> viewModelScope.launch { handleToggleBookmark() }
            is ReaderEvent.DeleteBookmark -> viewModelScope.launch { handleDeleteBookmark(event.cfi) }
            is ReaderEvent.AdjustBrightness -> handleAdjustBrightness(event.delta)
            is ReaderEvent.EndBrightness -> handleEndBrightness()
            is ReaderEvent.TextSelectionChanged -> _state.update { it.copy(textSelection = event.selection) }
            is ReaderEvent.AddHighlight -> viewModelScope.launch { handleAddHighlight(event.color, event.note) }
            is ReaderEvent.DeleteHighlight -> viewModelScope.launch { handleDeleteHighlight(event.cfi) }
            is ReaderEvent.ClearSelection -> _state.update { it.copy(textSelection = null) }
            is ReaderEvent.OnAnnotationClicked -> handleAnnotationClicked(event.cfi)
            is ReaderEvent.DismissAnnotationDetail -> _state.update { it.copy(selectedAnnotation = null) }
            is ReaderEvent.ShowFootnote -> _state.update { it.copy(activeFootnote = event.footnote) }
            is ReaderEvent.DismissFootnote -> _state.update { it.copy(activeFootnote = null) }
            is ReaderEvent.OnError -> _state.update { it.copy(errorMessage = event.message, isLoading = false) }
        }
    }

    private suspend fun handleInit(event: ReaderEvent.Init) {
        _state.update {
            it.copy(
                bookId = event.bookId,
                filePath = event.filePath,
                bookTitle = event.bookTitle,
                isLoading = true,
                errorMessage = null
            )
        }

        val bookmarks = storage.getBookmarks(event.bookId)
        val annotations = storage.getAnnotations(event.bookId)
        val savedProgress = storage.getProgress(event.bookId)

        val activeProgress = if (event.initialProgress > 0f) {
            event.initialProgress
        } else if (_state.value.progressFraction > 0f) {
            _state.value.progressFraction
        } else {
            savedProgress?.fraction ?: 0f
        }

        val activeCfi = event.initialCfi ?: _state.value.currentCfi ?: savedProgress?.cfi
        val isBookmarked = activeCfi != null && bookmarks.any { it.cfi == activeCfi }

        _state.update {
            it.copy(
                bookmarks = bookmarks,
                annotations = annotations,
                progressFraction = activeProgress,
                currentCfi = activeCfi,
                isBookmarked = isBookmarked,
                isLoading = false
            )
        }
    }

    private fun handleToggleControls() {
        _state.update { it.copy(isControlsVisible = !it.isControlsVisible) }
    }

    private fun handleUpdateProgress(progress: Float) {
        val clamped = progress.coerceIn(0f, 1f)
        _state.update { it.copy(progressFraction = clamped) }
        val cfi = _state.value.currentCfi ?: ""
        viewModelScope.launch {
            storage.saveProgress(_state.value.bookId, clamped, cfi)
        }
    }

    private fun handleLocationUpdated(event: ReaderEvent.LocationUpdated) {
        val isBookmarked = _state.value.bookmarks.any { it.cfi == event.cfi }
        _state.update {
            it.copy(
                currentCfi = event.cfi,
                progressFraction = event.fraction,
                currentChapterTitle = event.chapterTitle,
                currentSectionIndex = event.sectionIndex,
                totalSections = event.totalSections,
                isBookmarked = isBookmarked
            )
        }
    }

    private fun handleChangeFontSize(delta: Int) {
        val currentSize = _state.value.readerConfig.fontSize
        val newSize = (currentSize + delta).coerceIn(12, 36)
        _state.update { it.copy(readerConfig = it.readerConfig.copy(fontSize = newSize)) }
    }

    private fun handleSetFontSize(sizePx: Int) {
        val newSize = sizePx.coerceIn(12, 36)
        _state.update { it.copy(readerConfig = it.readerConfig.copy(fontSize = newSize)) }
    }

    private suspend fun handleToggleBookmark() {
        val cfi = _state.value.currentCfi ?: return
        val currentBookmarks = _state.value.bookmarks
        val existing = currentBookmarks.find { it.cfi == cfi }

        if (existing != null) {
            handleDeleteBookmark(cfi)
            return
        }

        val title = _state.value.currentChapterTitle ?: "Section ${_state.value.currentSectionIndex + 1}"
        val bookmark = EpubBookmark(
            cfi = cfi,
            title = title,
            timestamp = clock.now().toEpochMilliseconds(),
            progressFraction = _state.value.progressFraction
        )
        storage.saveBookmark(_state.value.bookId, bookmark)
        _state.update {
            it.copy(
                bookmarks = currentBookmarks + bookmark,
                isBookmarked = true
            )
        }
    }

    private suspend fun handleDeleteBookmark(cfi: String) {
        storage.deleteBookmark(_state.value.bookId, cfi)
        _state.update {
            val updated = it.bookmarks.filterNot { b -> b.cfi == cfi }
            it.copy(
                bookmarks = updated,
                isBookmarked = if (it.currentCfi == cfi) false else it.isBookmarked
            )
        }
    }

    private fun handleAdjustBrightness(delta: Float) {
        val current = _state.value.brightness
        val updated = (current + delta).coerceIn(0.05f, 1.0f)
        _state.update {
            it.copy(
                brightness = updated,
                isBrightnessOverlayVisible = true
            )
        }
        viewModelScope.launch {
            _effects.send(ReaderEffect.ApplyWindowBrightness(updated))
        }
        brightnessDismissJob?.cancel()
    }

    private fun handleEndBrightness() {
        brightnessDismissJob?.cancel()
        brightnessDismissJob = viewModelScope.launch {
            delay(1500)
            _state.update { it.copy(isBrightnessOverlayVisible = false) }
        }
    }

    private suspend fun handleAddHighlight(color: String, note: String?) {
        val sel = _state.value.textSelection ?: return
        if (sel.cfi.isBlank()) return

        val now = clock.now().toEpochMilliseconds()
        val annotation = EpubAnnotation(
            id = "$now",
            cfi = sel.cfi,
            text = sel.text,
            color = color,
            note = note,
            timestamp = now
        )
        storage.saveAnnotation(_state.value.bookId, annotation)
        _state.update {
            it.copy(
                annotations = it.annotations.filterNot { a -> a.cfi == annotation.cfi } + annotation,
                textSelection = null
            )
        }
    }

    private suspend fun handleDeleteHighlight(cfi: String) {
        storage.deleteAnnotation(_state.value.bookId, cfi)
        _state.update {
            it.copy(
                annotations = it.annotations.filterNot { a -> a.cfi == cfi },
                selectedAnnotation = if (it.selectedAnnotation?.cfi == cfi) null else it.selectedAnnotation
            )
        }
    }

    private fun handleAnnotationClicked(cfi: String) {
        val now = clock.now().toEpochMilliseconds()
        val clicked = _state.value.annotations.find { it.cfi == cfi }
            ?: EpubAnnotation(
                id = "$now",
                cfi = cfi,
                text = "",
                color = "#FFEB3B",
                timestamp = now
            )
        _state.update { it.copy(selectedAnnotation = clicked) }
    }
}
