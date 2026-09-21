package io.github.asadullah012.foliate.sample

import androidx.compose.runtime.Composable

@Composable
internal expect fun SampleBackHandler(enabled: Boolean = true, onBack: () -> Unit)
