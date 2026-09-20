# foliate-kmp

**foliate-kmp** is a high-fidelity Kotlin Multiplatform (Compose Multiplatform) EPUB reader library powered by [foliate-js](https://github.com/johnfactotum/foliate-js). It brings full-featured, standards-compliant EPUB rendering with discrete pagination, CFI navigation, annotations, search, and customizable themes to **Android** and **iOS**.

---

## Artifacts

`foliate-kmp` is split into two modular artifacts:

| Artifact | Description |
| :--- | :--- |
| **`io.github.galib:foliate-kmp-core`** | Headless engine, WebView platform bridges (Android `WebViewAssetLoader`, iOS `WKURLSchemeHandler`), `EpubReaderController`, data models, and storage interfaces. |
| **`io.github.galib:foliate-kmp-compose`** | Turnkey, pre-built Material 3 reading screen with top navigation bar, bottom progress scrubber, TOC drawer, appearance sheet, annotations dialog, footnote sheet, and in-book search. |

---

## Installation

Add to your `build.gradle.kts` (or Version Catalog):

```kotlin
// Option 1: Complete Turnkey UI (includes core)
commonMain.dependencies {
    implementation("io.github.galib:foliate-kmp-compose:0.1.0-beta01")
}

// Option 2: Headless Engine Only (custom UI)
commonMain.dependencies {
    implementation("io.github.galib:foliate-kmp-core:0.1.0-beta01")
}
```

---

## Quickstart

### 1. Ready-to-use Reader Screen (`foliate-kmp-compose`)

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.galib.foliate.compose.ui.ReaderScreen

@Composable
fun MyBookScreen(epubFilePath: String, onBack: () -> Unit) {
    ReaderScreen(
        filePath = epubFilePath,
        bookTitle = "Pride and Prejudice",
        onNavigateBack = onBack
    )
}
```

### 2. Headless Reader View (`foliate-kmp-core`)

If you want to design your own custom reader UI, toolbars, and gesture controls:

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.galib.foliate.EpubReaderController
import io.github.galib.foliate.model.EpubReaderConfig
import io.github.galib.foliate.model.EpubReaderTheme
import io.github.galib.foliate.ui.FoliateReaderView

@Composable
fun CustomReader(bookPath: String) {
    val controller = remember { EpubReaderController() }

    FoliateReaderView(
        bookPath = bookPath,
        controller = controller,
        config = EpubReaderConfig(theme = EpubReaderTheme.SEPIA, fontSize = 20),
        modifier = Modifier.fillMaxSize()
    )
}
```

---

## Features

- **High-Fidelity EPUB Rendering**: Powered by Foliate's battle-tested modular engine.
- **Canonical Fragment Identifiers (CFI)**: Exact reading position persistence and restoration.
- **Continuous & Paginated Flows**: Supports both classic discrete page turns and continuous vertical scrolling.
- **Built-in Themes**: Light, Sepia, Slate, Dark, and AMOLED OLED black.
- **Typography Customization**: Font family, font size, line height, margins, and text alignment.
- **In-Book Full-Text Search**: Live substring matching with excerpt previews and direct jump-to-CFI.
- **Annotations & Highlights**: Persistent highlights with color pickers and optional notes.
- **Footnotes & Popovers**: Automatic footnote detection and popup display.
- **Pluggable Persistence**: Implement `EpubReaderStorage` to persist reading progress, bookmarks, and highlights to Room, SQLite, or DataStore.

---

## License

MIT License. See [LICENSE](LICENSE) for details.  
Powered by [foliate-js](https://github.com/johnfactotum/foliate-js) (MIT License, Copyright © John Factotum).
