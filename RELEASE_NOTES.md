## 📖 foliate-kmp v0.1.0-beta01

The first public beta release of **foliate-kmp**, bringing high-fidelity, standards-compliant EPUB rendering powered by [foliate-js](https://github.com/johnfactotum/foliate-js) to **Kotlin Multiplatform (Android & iOS)**.

---

### ✨ Highlights

- **Modular Architecture:**
  - `io.github.asadullah012:foliate-kmp-compose` — Turnkey Material 3 reader screen with toolbars, TOC drawer, appearance sheet, and search.
  - `io.github.asadullah012:foliate-kmp-core` — Headless engine, WebView bridges (`WebViewAssetLoader` on Android, `WKURLSchemeHandler` on iOS), and reactive `EpubReaderController`.
- **Reading Modes:** Both discrete paginated page-turning and continuous vertical scrolling.
- **Exact Position Tracking (CFI):** Canonical Fragment Identifier (CFI) restoration across font changes and device rotations.
- **Customization:** 5 built-in themes (Light, Sepia, Slate, Dark, OLED Black), live font sizing, margins, and line height adjustments.
- **Search & Annotations:** In-book full-text search with excerpt previews, color-coded highlights, user notes, and footnote popovers.
- **Pluggable Persistence:** `EpubReaderStorage` interface ready for Room, SQLite, or DataStore.
- **Security & R8 Ready:** Origin-locked WebViews (`READER_ORIGIN`), external link blocking, path traversal guards, and embedded consumer ProGuard/R8 rules.
- **Binary Compatibility:** Built with `explicitApi()` and verified against Kotlin ABI validation.

---

### 📦 Installation

Published to **Maven Central**:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
}

// commonMain dependencies:
implementation("io.github.asadullah012:foliate-kmp-compose:0.1.0-beta01")
// or headless core:
implementation("io.github.asadullah012:foliate-kmp-core:0.1.0-beta01")
```

Or with Version Catalog (`libs.versions.toml`):
```toml
[versions]
foliate-kmp = "0.1.0-beta01"

[libraries]
foliate-kmp-compose = { module = "io.github.asadullah012:foliate-kmp-compose", version.ref = "foliate-kmp" }
```

---

### 📚 Documentation & Examples

For full architecture details, custom reader samples, and Room storage integration, see the [README.md](https://github.com/asadullah012/foliate-kmp#readme) and [CHANGELOG.md](https://github.com/asadullah012/foliate-kmp/blob/main/CHANGELOG.md).
