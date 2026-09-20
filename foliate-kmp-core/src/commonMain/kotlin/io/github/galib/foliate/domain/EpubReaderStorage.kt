package io.github.galib.foliate.domain

import io.github.galib.foliate.model.EpubAnnotation
import io.github.galib.foliate.model.EpubBookmark
import io.github.galib.foliate.model.EpubProgress
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pluggable persistence boundary for the EPUB reader.
 *
 * Implement this interface to persist reading positions, bookmarks, and annotations
 * to Room, SQLite, DataStore, or a remote cloud database.
 */
public interface EpubReaderStorage {
    public suspend fun saveProgress(bookId: String, fraction: Float, cfi: String) {
        saveProgress(bookId, EpubProgress(fraction = fraction, cfi = cfi))
    }
    public suspend fun saveProgress(bookId: String, progress: EpubProgress)
    public suspend fun getProgress(bookId: String): EpubProgress?
    public suspend fun getBookmarks(bookId: String): List<EpubBookmark>
    public suspend fun saveBookmark(bookId: String, bookmark: EpubBookmark)
    public suspend fun deleteBookmark(bookId: String, cfi: String)
    public suspend fun getAnnotations(bookId: String): List<EpubAnnotation>
    public suspend fun saveAnnotation(bookId: String, annotation: EpubAnnotation)
    public suspend fun deleteAnnotation(bookId: String, cfi: String)
}

/**
 * Default in-memory thread-safe storage implementation for [EpubReaderStorage].
 * Enables foliate-kmp to function out-of-the-box with zero configuration.
 */
public class InMemoryEpubReaderStorage : EpubReaderStorage {
    private val mutex = Mutex()
    private val progressMap = mutableMapOf<String, EpubProgress>()
    private val bookmarksMap = mutableMapOf<String, MutableList<EpubBookmark>>()
    private val annotationsMap = mutableMapOf<String, MutableList<EpubAnnotation>>()

    override suspend fun saveProgress(bookId: String, progress: EpubProgress) {
        mutex.withLock {
            progressMap[bookId] = progress
        }
    }

    override suspend fun saveProgress(bookId: String, fraction: Float, cfi: String) {
        saveProgress(bookId, EpubProgress(fraction = fraction, cfi = cfi))
    }

    override suspend fun getProgress(bookId: String): EpubProgress? = mutex.withLock {
        progressMap[bookId]
    }

    override suspend fun getBookmarks(bookId: String): List<EpubBookmark> = mutex.withLock {
        bookmarksMap[bookId]?.toList() ?: emptyList()
    }

    override suspend fun saveBookmark(bookId: String, bookmark: EpubBookmark) {
        mutex.withLock {
            val list = bookmarksMap.getOrPut(bookId) { mutableListOf() }
            list.removeAll { it.cfi == bookmark.cfi }
            list.add(bookmark)
        }
    }

    override suspend fun deleteBookmark(bookId: String, cfi: String) {
        mutex.withLock {
            bookmarksMap[bookId]?.removeAll { it.cfi == cfi }
        }
    }

    override suspend fun getAnnotations(bookId: String): List<EpubAnnotation> = mutex.withLock {
        annotationsMap[bookId]?.toList() ?: emptyList()
    }

    override suspend fun saveAnnotation(bookId: String, annotation: EpubAnnotation) {
        mutex.withLock {
            val list = annotationsMap.getOrPut(bookId) { mutableListOf() }
            list.removeAll { it.cfi == annotation.cfi }
            list.add(annotation)
        }
    }

    override suspend fun deleteAnnotation(bookId: String, cfi: String) {
        mutex.withLock {
            annotationsMap[bookId]?.removeAll { it.cfi == cfi }
        }
    }
}
