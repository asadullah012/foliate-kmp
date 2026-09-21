package io.github.asadullah012.foliate.sample

import androidx.compose.runtime.Composable

@Composable
internal actual fun SampleBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on iOS: navigation gestures are handled by UIKit/SwiftUI navigation hierarchy
}
