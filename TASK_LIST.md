# Engineering Task List & Technical Improvement Roadmap

This document outlines the detailed roadmap, actionable tasks, and architectural rationales for hardening **foliate-kmp** following the repository audit.

---

## Priority Matrix Overview

| Priority | Milestone | Focus Area | Impact |
| :--- | :--- | :--- | :--- |
| **P0 (Critical)** | [Milestone 1](#milestone-1-critical-ux--gesture-fixes) | Fix Continuous Scroll & iOS Brightness | Resolves broken reading flow and platform inconsistency |
| **P0 (Critical)** | [Milestone 2](#milestone-2-lifecycle--memory-hardening) | Android & iOS WebView Lifecycle Hardening | Eliminates potential crashes and context leaks |
| **P1 (High)** | [Milestone 3](#milestone-3-internationalization--extensibility) | Localization (i18n) & Custom String Support | Removes adoption blocker for international applications |
| **P1 (High)** | [Milestone 4](#milestone-4-testing--verification-expansion) | Automated Multiplatform & UI Test Coverage | Prevents silent regressions across Android and iOS |
| **P2 (Medium)** | [Milestone 5](#milestone-5-architecture--state-refinement) | MVI State Refinement & Compose Stability | Optimizes recomposition performance and state purity |
| **P2 (Medium)** | [Milestone 6](#milestone-6-open-source-governance--showcase) | README Showcase, Badges & GitHub Templates | Enhances developer adoption and contributor workflow |
| **P3 (Future)** | [Milestone 7](#milestone-7-platform-scope-expansion) | Desktop (JVM) and Web (Wasm/JS) Exploration | Extends Foliate's reach to full cross-platform parity |

---

## Milestone 1: Critical UX & Gesture Fixes

### Task 1.1: Fix Touch Interception & Continuous Mode Conflict in `index.html`
- **File**: `foliate-kmp-core/src/commonMain/composeResources/files/foliate/index.html`
- **Action**:
  1. Disable vertical swipe brightness gesture when reading in continuous scroll mode (`currentSettings.flow === 'scrolled'`).
  2. Reduce the touch trigger zone in paginated mode from `width * 0.35` (35%) down to an edge strip of **$\le 28\text{px}$** (or $\le 6\%$ of screen width).
  3. Support dynamic enablement via a `setBrightnessGestureEnabled(enabled)` JavaScript entry point.
- **Reasoning**:
  In continuous mode, vertical swipe is the reading scroll gesture. Currently, swiping within 35% of the screen fires `e.preventDefault()`, which halts page scrolling and hijacks the gesture to change brightness. 35% is also far too wide for paginated reading, causing accidental triggers when holding the device in one hand.

### Task 1.2: Expose Gesture Configuration in `EpubReaderConfig` & `EpubReaderController`
- **Files**:
  - `foliate-kmp-core/src/commonMain/kotlin/io/github/asadullah012/foliate/model/EpubReaderModels.kt`
  - `foliate-kmp-core/src/commonMain/kotlin/io/github/asadullah012/foliate/EpubReaderController.kt`
- **Action**:
  1. Add `enableBrightnessGesture: Boolean = true` to `EpubReaderConfig` with backward-compatible secondary constructor or default overloads.
  2. Add `setBrightnessGestureEnabled(enabled: Boolean)` to `EpubReaderController`, evaluating the corresponding JS command.
  3. Apply this setting automatically in `applyConfig(config)`.
- **Reasoning**:
  Consumers currently have zero control over gesture behavior. Adding configuration while maintaining binary backward compatibility prevents breaking existing Maven Central consumers.

### Task 1.3: Standardize Cross-Platform & iOS Brightness with In-App Dimming Scrim
- **File**: `foliate-kmp-compose/src/commonMain/kotlin/io/github/asadullah012/foliate/compose/ui/ReaderScreen.kt`
- **Action**:
  1. Implement a composable software dimming scrim over the reading surface when `state.brightness < 1.0f`:
     ```kotlin
     if (state.brightness < 1.0f) {
         val scrimAlpha = ((1.0f - state.brightness) * 0.75f).coerceIn(0f, 0.85f)
         Box(
             modifier = Modifier
                 .fillMaxSize()
                 .background(Color.Black.copy(alpha = scrimAlpha))
         )
     }
     ```
  2. Retain the `onBrightnessChange: ((Float) -> Unit)?` callback for callers who explicitly opt into manipulating hardware backlight.
- **Reasoning**:
  iOS has no window-level brightness API (only system-wide `UIScreen.main.brightness`, which alters brightness across all apps and home screens). Software scrim dimming works consistently across Android, iOS, Desktop, and Web without permissions, without side effects outside the app, and allows dimming below the hardware minimum in dark rooms.

### Task 1.4: Update Binary API Dumps & Verify ABI
- **Files**:
  - `foliate-kmp-core/api/foliate-kmp-core.klib.api`
  - `foliate-kmp-core/api/android/foliate-kmp-core.api`
- **Action**:
  Run `./gradlew apiDump` and `./gradlew apiCheck` to register the new public symbols and ensure no accidental ABI breaks.
- **Reasoning**:
  The project uses JetBrains Experimental ABI Validation. Every public API modification must be verified and tracked in git.

---

## Milestone 2: Lifecycle & Memory Hardening

### Task 2.1: Refactor Android `WebView` Lifecycle Management
- **File**: `foliate-kmp-core/src/androidMain/kotlin/io/github/asadullah012/foliate/ui/PlatformEpubWebView.android.kt`
- **Action**:
  1. Eliminate the `remember(context, bookPath, controller) { WebView(context) }` + `DisposableEffect { onDispose { webView.destroy() } }` pattern.
  2. Use `AndroidView`'s lifecycle-managed `factory` and `onRelease`:
     ```kotlin
     AndroidView(
         factory = { ctx -> createWebView(ctx, ...) },
         onRelease = { wv ->
             controller.jsEvaluator = null
             controller.onDetached()
             wv.stopLoading()
             wv.destroy()
         },
         modifier = modifier
     )
     ```
- **Reasoning**:
  Holding a destroyed `WebView` in `remember` causes crashes or blank screens if a Composable leaves and re-enters composition or when handling complex navigation backstacks.

### Task 2.2: Guard Bridge References Against Context Leaks
- **Files**:
  - `foliate-kmp-core/src/androidMain/kotlin/io/github/asadullah012/foliate/ui/PlatformEpubWebView.android.kt`
  - `foliate-kmp-core/src/iosMain/kotlin/io/github/asadullah012/foliate/ui/PlatformEpubWebView.ios.kt`
- **Action**:
  1. Ensure `AndroidBridge` uses `WeakReference<WebView>` instead of closure captures that can pin Activity contexts inside the V8 engine heap.
  2. Verify that `removeScriptMessageHandlerForName` on iOS and `removeJavascriptInterface` on Android clean up promptly upon teardown.
- **Reasoning**:
  JavaScript bridge objects registered via `addJavascriptInterface` or `WKUserContentController` create cross-boundary references between native memory and the JS runtime that easily outlive their parent views.

---

## Milestone 3: Internationalization & Extensibility

### Task 3.1: Externalize UI Strings for Multi-Language Support
- **Files**:
  - `foliate-kmp-compose/src/commonMain/kotlin/io/github/asadullah012/foliate/compose/ui/components/*.kt`
  - New contract: `foliate-kmp-compose/src/commonMain/kotlin/io/github/asadullah012/foliate/compose/model/ReaderStrings.kt`
- **Action**:
  1. Create a `ReaderStrings` data class or interface containing all user-facing labels (`readerAppearanceTitle`, `resetDefaults`, `searchPlaceholder`, `tableOfContents`, etc.) defaulting to English.
  2. Pass `strings: ReaderStrings = ReaderStrings.Default` into `ReaderScreen` and child components.
- **Reasoning**:
  All UI strings are currently hardcoded in English literals. Enterprise and international consumer apps cannot adopt `foliate-kmp-compose` without the ability to localize the interface.

### Task 3.2: Slot APIs for Reader Customization
- **File**: `foliate-kmp-compose/src/commonMain/kotlin/io/github/asadullah012/foliate/compose/ui/ReaderScreen.kt`
- **Action**:
  Provide optional composable slots (`topBarActions: @Composable RowScope.() -> Unit = {}`, `customSheets: @Composable () -> Unit = {}`) in `ReaderScreen`.
- **Reasoning**:
  Host applications frequently need to add custom actions (e.g., share text, text-to-speech narration, sync status indicators) to the reader toolbar without forking the entire screen.

---

## Milestone 4: Testing & Verification Expansion

### Task 4.1: Unit Testing Storage Persistence & Debounce Behavior
- **File**: `foliate-kmp-core/src/commonTest/kotlin/io/github/asadullah012/foliate/storage/EpubReaderStorageTest.kt`
- **Action**:
  Write automated tests using `runTest` and Turbine validating that:
  1. Rapid relocation events debounce properly to a single write after 2 seconds.
  2. Pending writes flush immediately when reader unmounts.
- **Reasoning**:
  The 2-second debounce logic in `ReaderModel` prevents database hammering; it must be verified under concurrency test conditions.

### Task 4.2: Automated Bridge Protocol Validation
- **File**: `foliate-kmp-core/src/commonTest/kotlin/io/github/asadullah012/foliate/internal/BridgeProtocolTest.kt`
- **Action**:
  Verify serialization and deserialization of all bridge message types (`relocate`, `toc`, `search_results`, `adjust_brightness`, `selection`, `footnote`, `error`).
- **Reasoning**:
  Bridge serialization bugs cause silent failures where WebView messages are dropped without stack traces.

### Task 4.3: Compose UI Tests for Turnkey Components
- **File**: `foliate-kmp-compose/src/commonTest/kotlin/io/github/asadullah012/foliate/compose/ui/ReaderScreenTest.kt`
- **Action**:
  Implement Compose UI tests verifying:
  1. Controls visibility toggling on center tap.
  2. Appearance bottom sheet presentation and font size slider interactions.
  3. Brightness HUD pill rendering when brightness adjusts.
- **Reasoning**:
  Currently, there are zero UI tests in `commonTest`, leaving all visual transitions prone to regression.

---

## Milestone 5: Architecture & State Refinement

### Task 5.1: Refactor `ReaderState` into Disciplined UDF Hierarchy
- **File**: `foliate-kmp-compose/src/commonMain/kotlin/io/github/asadullah012/foliate/compose/presentation/ReaderState.kt`
- **Action**:
  1. Model top-level lifecycle as a sealed interface (`ReaderState.Loading`, `ReaderState.Error`, `ReaderState.Ready`).
  2. Move book-reading details (CFI, progress, TOC) into the `Ready` branch.
- **Reasoning**:
  Eliminates impossible contradictory states (e.g., `isLoading = true` with a non-null `errorMessage`), adhering to Clean MVI standards.

### Task 5.2: Adopt Immutable Collections for Skippable Composables
- **File**: `foliate-kmp-compose/src/commonMain/kotlin/io/github/asadullah012/foliate/compose/presentation/ReaderState.kt`
- **Action**:
  Replace standard `List<T>` with `ImmutableList<T>` from `kotlinx.collections.immutable` for `searchResults`, `bookmarks`, `tocItems`, and `annotations`.
- **Reasoning**:
  The Compose compiler cannot infer stability for standard Kotlin `List` interfaces, triggering unnecessary recompositions of large list components (TOC drawer, search results).

---

## Milestone 6: Open-Source Governance & Showcase

### Task 6.1: Add Visual Showcase to `README.md`
- **File**: `README.md`
- **Action**:
  Embed high-quality screenshots (or animated GIF) demonstrating:
  1. Reading canvas with pagination and themes (Light, Sepia, Dark, AMOLED).
  2. Appearance sheet with typography and layout controls.
  3. Table of Contents drawer and in-book search dialog.
- **Reasoning**:
  E-reader libraries are visual products. A README without UI previews directly hurts repository adoption and developer interest.

### Task 6.2: Add Badges to `README.md`
- **File**: `README.md`
- **Action**:
  Add shields.io badges at the top of README:
  - Maven Central version: `https://img.shields.io/maven-central/v/io.github.asadullah012/foliate-kmp-core`
  - GitHub Actions CI status: `https://github.com/asadullah012/foliate-kmp/actions/workflows/build.yml/badge.svg`
  - License badge: Apache 2.0
  - Kotlin version badge
- **Reasoning**:
  Provides instant proof of active CI, release status, and license clarity.

### Task 6.3: Add Community Governance & Issue Templates
- **Files**:
  - `CODE_OF_CONDUCT.md`
  - `.github/ISSUE_TEMPLATE/bug_report.md`
  - `.github/ISSUE_TEMPLATE/feature_request.md`
  - `.github/PULL_REQUEST_TEMPLATE.md`
- **Action**:
  Create standard GitHub issue templates with environment fields (OS, Device, foliate-kmp version, EPUB sample).
- **Reasoning**:
  Standardizes incoming bug reports and pull requests, saving maintainer triage time.

---

## Milestone 7: Platform Scope Expansion (Future)

### Task 7.1: Explore Compose Desktop (CEF / JavaFX WebView) Support
- **Action**:
  Investigate embedding Foliate.js inside a desktop webview engine (e.g. JetBrains Compose Desktop WebView or JavaFX `WebView`) in a new `:foliate-kmp-desktop` module.
- **Reasoning**:
  Foliate.js was originally developed for desktop Linux. Adding Desktop support makes foliate-kmp a complete cross-platform solution.

### Task 7.2: Explore Web (Wasm/JS) Target
- **Action**:
  Evaluate compiling `foliate-kmp-core` for `wasmJs` to directly render inside the browser DOM without an enclosing native WebView bridge.
- **Reasoning**:
  Allows Kotlin Multiplatform web apps to run the exact same reader logic natively in browsers.
