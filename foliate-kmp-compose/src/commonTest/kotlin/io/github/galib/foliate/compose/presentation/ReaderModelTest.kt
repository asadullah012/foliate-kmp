package io.github.galib.foliate.compose.presentation

import io.github.galib.foliate.domain.InMemoryEpubReaderStorage
import io.github.galib.foliate.model.EpubAnnotation
import io.github.galib.foliate.model.EpubBookmark
import io.github.galib.foliate.model.EpubReaderFlow
import io.github.galib.foliate.model.EpubReaderSheet
import io.github.galib.foliate.model.EpubReaderTheme
import io.github.galib.foliate.model.EpubSearchResult
import io.github.galib.foliate.model.EpubTextSelection
import io.github.galib.foliate.model.EpubTocItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testClock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1700000000000L)
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has expected default values`() {
        val model = ReaderModel(InMemoryEpubReaderStorage(), testClock)
        val state = model.state.value

        assertTrue(state.isLoading)
        assertTrue(state.isControlsVisible)
        assertEquals(18, state.readerConfig.fontSize)
        assertEquals(EpubReaderTheme.LIGHT, state.readerConfig.theme)
        assertEquals(EpubReaderFlow.PAGINATED, state.readerConfig.flow)
        assertTrue(state.bookmarks.isEmpty())
        assertTrue(state.annotations.isEmpty())
        assertNull(state.activeSheet)
    }

    @Test
    fun `init loads saved bookmarks annotations and progress from storage`() = runTest(testDispatcher) {
        val storage = InMemoryEpubReaderStorage()
        val bookId = "book-123"
        val savedBookmark = EpubBookmark(cfi = "cfi/1", title = "Chapter 1", timestamp = 1000L, progressFraction = 0.2f)
        val savedAnnotation = EpubAnnotation(id = "ann-1", cfi = "cfi/2", text = "Hello", color = "#FFEB3B")

        storage.saveBookmark(bookId, savedBookmark)
        storage.saveAnnotation(bookId, savedAnnotation)
        storage.saveProgress(bookId, 0.45f, "cfi/3")

        val model = ReaderModel(storage, testClock)
        model.onEvent(ReaderEvent.Init(bookId = bookId, filePath = "/path/book.epub", bookTitle = "Test Book"))
        advanceUntilIdle()

        val state = model.state.value
        assertFalse(state.isLoading)
        assertEquals("Test Book", state.bookTitle)
        assertEquals("/path/book.epub", state.filePath)
        assertEquals(1, state.bookmarks.size)
        assertEquals(savedBookmark, state.bookmarks[0])
        assertEquals(1, state.annotations.size)
        assertEquals(savedAnnotation, state.annotations[0])
        assertEquals(0.45f, state.progressFraction)
        assertEquals("cfi/3", state.currentCfi)
    }

    @Test
    fun `toggleControls flips visibility state`() = runTest(testDispatcher) {
        val model = ReaderModel(InMemoryEpubReaderStorage(), testClock)
        assertTrue(model.state.value.isControlsVisible)

        model.onEvent(ReaderEvent.ToggleControls)
        advanceUntilIdle()
        assertFalse(model.state.value.isControlsVisible)

        model.onEvent(ReaderEvent.ToggleControls)
        advanceUntilIdle()
        assertTrue(model.state.value.isControlsVisible)
    }

    @Test
    fun `updateProgress clamps progress and persists to storage`() = runTest(testDispatcher) {
        val storage = InMemoryEpubReaderStorage()
        val model = ReaderModel(storage, testClock)
        model.onEvent(ReaderEvent.Init(bookId = "book-1", filePath = "test.epub"))
        advanceUntilIdle()

        model.onEvent(ReaderEvent.UpdateProgress(0.75f))
        advanceUntilIdle()

        assertEquals(0.75f, model.state.value.progressFraction)
        val persisted = storage.getProgress("book-1")
        assertNotNull(persisted)
        assertEquals(0.75f, persisted.fraction)
    }

    @Test
    fun `appearance configuration events update readerConfig`() = runTest(testDispatcher) {
        val model = ReaderModel(InMemoryEpubReaderStorage(), testClock)

        model.onEvent(ReaderEvent.ChangeTheme(EpubReaderTheme.DARK))
        model.onEvent(ReaderEvent.SetFontSize(24))
        model.onEvent(ReaderEvent.ChangeFlow(EpubReaderFlow.SCROLLED))
        model.onEvent(ReaderEvent.ChangeLineHeight(1.8f))
        model.onEvent(ReaderEvent.ChangeTextAlign("left"))
        model.onEvent(ReaderEvent.ChangeFontFamily("serif"))
        model.onEvent(ReaderEvent.ChangeMargin(48))
        advanceUntilIdle()

        val config = model.state.value.readerConfig
        assertEquals(EpubReaderTheme.DARK, config.theme)
        assertEquals(24, config.fontSize)
        assertEquals(EpubReaderFlow.SCROLLED, config.flow)
        assertEquals(1.8f, config.lineHeight)
        assertEquals("left", config.textAlign)
        assertEquals("serif", config.fontFamily)
        assertEquals(48, config.margin)
    }

    @Test
    fun `resetAppearance restores default readerConfig`() = runTest(testDispatcher) {
        val model = ReaderModel(InMemoryEpubReaderStorage(), testClock)
        model.onEvent(ReaderEvent.ChangeTheme(EpubReaderTheme.DARK))
        model.onEvent(ReaderEvent.SetFontSize(30))
        model.onEvent(ReaderEvent.ChangeMargin(64))
        advanceUntilIdle()

        assertEquals(EpubReaderTheme.DARK, model.state.value.readerConfig.theme)
        assertEquals(30, model.state.value.readerConfig.fontSize)
        assertEquals(64, model.state.value.readerConfig.margin)

        model.onEvent(ReaderEvent.ResetAppearance)
        advanceUntilIdle()

        val defaultConfig = io.github.galib.foliate.model.EpubReaderConfig()
        assertEquals(defaultConfig, model.state.value.readerConfig)
    }

    @Test
    fun `toggleBookmark adds new bookmark and removes existing bookmark`() = runTest(testDispatcher) {
        val storage = InMemoryEpubReaderStorage()
        val model = ReaderModel(storage, testClock)
        model.onEvent(ReaderEvent.Init(bookId = "book-1", filePath = "test.epub"))
        advanceUntilIdle()
        model.onEvent(ReaderEvent.LocationUpdated("cfi/chapter1", 0.1f, "Chapter 1", 0, 5))
        advanceUntilIdle()

        // Add
        model.onEvent(ReaderEvent.ToggleBookmark)
        advanceUntilIdle()

        assertTrue(model.state.value.isBookmarked)
        assertEquals(1, model.state.value.bookmarks.size)
        assertEquals("cfi/chapter1", model.state.value.bookmarks[0].cfi)
        assertEquals(1, storage.getBookmarks("book-1").size)

        // Toggle again -> Remove
        model.onEvent(ReaderEvent.ToggleBookmark)
        advanceUntilIdle()

        assertFalse(model.state.value.isBookmarked)
        assertTrue(model.state.value.bookmarks.isEmpty())
        assertTrue(storage.getBookmarks("book-1").isEmpty())
    }

    @Test
    fun `addHighlight and deleteHighlight update state and storage`() = runTest(testDispatcher) {
        val storage = InMemoryEpubReaderStorage()
        val model = ReaderModel(storage, testClock)
        model.onEvent(ReaderEvent.Init(bookId = "book-1", filePath = "test.epub"))
        model.onEvent(ReaderEvent.TextSelectionChanged(EpubTextSelection("highlighted text", "cfi/sel", 0)))
        advanceUntilIdle()

        model.onEvent(ReaderEvent.AddHighlight("#A5D6A7", "Note on passage"))
        advanceUntilIdle()

        assertEquals(1, model.state.value.annotations.size)
        val annotation = model.state.value.annotations[0]
        assertEquals("cfi/sel", annotation.cfi)
        assertEquals("#A5D6A7", annotation.color)
        assertEquals("Note on passage", annotation.note)
        assertNull(model.state.value.textSelection)
        assertEquals(1, storage.getAnnotations("book-1").size)

        // Delete
        model.onEvent(ReaderEvent.DeleteHighlight("cfi/sel"))
        advanceUntilIdle()

        assertTrue(model.state.value.annotations.isEmpty())
        assertTrue(storage.getAnnotations("book-1").isEmpty())
    }

    @Test
    fun `sheet management opens and closes sheets`() = runTest(testDispatcher) {
        val model = ReaderModel(InMemoryEpubReaderStorage(), testClock)

        model.onEvent(ReaderEvent.OpenSheet(EpubReaderSheet.APPEARANCE))
        advanceUntilIdle()
        assertEquals(EpubReaderSheet.APPEARANCE, model.state.value.activeSheet)

        model.onEvent(ReaderEvent.OpenSheet(EpubReaderSheet.TOC))
        advanceUntilIdle()
        assertEquals(EpubReaderSheet.TOC, model.state.value.activeSheet)

        model.onEvent(ReaderEvent.CloseSheet)
        advanceUntilIdle()
        assertNull(model.state.value.activeSheet)
    }

    @Test
    fun `onNavigateBack sends effect`() = runTest(testDispatcher) {
        val model = ReaderModel(InMemoryEpubReaderStorage(), testClock)
        model.onEvent(ReaderEvent.OnNavigateBack)
        advanceUntilIdle()

        val effect = model.effects.first()
        assertEquals(ReaderEffect.OnNavigateBack, effect)
    }
}
