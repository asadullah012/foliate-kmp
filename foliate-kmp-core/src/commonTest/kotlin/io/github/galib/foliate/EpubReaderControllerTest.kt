package io.github.galib.foliate

import io.github.galib.foliate.model.EpubReaderLocation
import io.github.galib.foliate.model.EpubReaderTheme
import io.github.galib.foliate.model.EpubTocItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EpubReaderControllerTest {

    @Test
    fun `initial state has default values`() {
        val controller = EpubReaderController()

        assertFalse(controller.isReady.value)
        assertNull(controller.errorMessage.value)
        assertEquals(0f, controller.location.value.progressFraction)
        assertTrue(controller.toc.value.isEmpty())
    }

    @Test
    fun `onRelocate updates location state`() {
        val controller = EpubReaderController()
        val loc = EpubReaderLocation(
            cfi = "epubcfi(/6/4!/4/2/1:0)",
            progressFraction = 0.42f,
            chapterTitle = "Chapter 3",
            sectionIndex = 2,
            totalSections = 10
        )

        controller.onRelocate(loc)

        assertEquals("epubcfi(/6/4!/4/2/1:0)", controller.location.value.cfi)
        assertEquals(0.42f, controller.location.value.progressFraction)
        assertEquals("Chapter 3", controller.location.value.chapterTitle)
        assertEquals(2, controller.location.value.sectionIndex)
        assertEquals(10, controller.location.value.totalSections)
    }

    @Test
    fun `onTocLoaded updates table of contents`() {
        val controller = EpubReaderController()
        val toc = listOf(
            EpubTocItem(id = "c1", label = "Chapter 1", href = "chap1.xhtml"),
            EpubTocItem(id = "c2", label = "Chapter 2", href = "chap2.xhtml")
        )

        controller.onTocLoaded(toc)

        assertEquals(2, controller.toc.value.size)
        assertEquals("Chapter 1", controller.toc.value[0].label)
    }

    @Test
    fun `ready and error transitions work correctly`() {
        val controller = EpubReaderController()

        controller.onError("Failed to read package")
        assertEquals("Failed to read package", controller.errorMessage.value)
        assertFalse(controller.isReady.value)

        controller.onReady()
        assertTrue(controller.isReady.value)
        assertNull(controller.errorMessage.value)
    }

    @Test
    fun `controller sends expected javascript to evaluator when ready`() {
        val controller = EpubReaderController()
        val executedScripts = mutableListOf<String>()
        controller.jsEvaluator = { executedScripts.add(it) }
        controller.onReady()

        controller.nextPage()
        controller.prevPage()
        controller.goToCfi("cfi/1")
        controller.setTheme(EpubReaderTheme.DARK)
        controller.setFontSize(22)

        assertEquals(5, executedScripts.size)
        assertTrue(executedScripts[0].contains("nextPage()"))
        assertTrue(executedScripts[1].contains("prevPage()"))
        assertTrue(executedScripts[2].contains("goToCfi(\"cfi/1\")"))
        assertTrue(executedScripts[3].contains("setTheme(\"#1E1E1E\""))
        assertTrue(executedScripts[4].contains("setFontSize(22)"))
    }

    @Test
    fun `commands issued before ready are queued and drained on ready`() {
        val controller = EpubReaderController()
        val executedScripts = mutableListOf<String>()
        controller.jsEvaluator = { executedScripts.add(it) }

        controller.nextPage()
        controller.goToCfi("cfi/early")

        // Not ready yet, nothing executed
        assertTrue(executedScripts.isEmpty())

        controller.onReady()

        // After ready, queued commands are executed in order
        assertEquals(2, executedScripts.size)
        assertTrue(executedScripts[0].contains("nextPage()"))
        assertTrue(executedScripts[1].contains("goToCfi(\"cfi/early\")"))
    }
}
