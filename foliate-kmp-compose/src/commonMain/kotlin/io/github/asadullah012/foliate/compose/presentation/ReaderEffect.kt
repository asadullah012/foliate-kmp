package io.github.asadullah012.foliate.compose.presentation

import androidx.compose.runtime.Immutable

/**
 * One-off side-effects emitted by [ReaderModel] to navigation or host platform window.
 */
@Immutable
public sealed interface ReaderEffect {
    public data object OnNavigateBack : ReaderEffect
    public data class ShowMessage(val message: String) : ReaderEffect
    public data class OpenExternalReader(val filePath: String) : ReaderEffect
    public data class ApplyWindowBrightness(val brightness: Float) : ReaderEffect
}
