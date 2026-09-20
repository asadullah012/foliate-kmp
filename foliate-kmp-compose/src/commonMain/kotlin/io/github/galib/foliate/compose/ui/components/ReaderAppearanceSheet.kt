package io.github.galib.foliate.compose.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.galib.foliate.model.EpubFontOption
import io.github.galib.foliate.model.EpubReaderConfig
import io.github.galib.foliate.model.EpubReaderFlow
import io.github.galib.foliate.model.EpubReaderTheme
import kotlin.math.roundToInt

private const val PREVIEW_TEXT =
    "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ReaderAppearanceSheet(
    config: EpubReaderConfig,
    onThemeChange: (EpubReaderTheme) -> Unit,
    onFlowChange: (EpubReaderFlow) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onTextAlignChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onMarginChange: (Int) -> Unit = {},
    onResetDefaults: () -> Unit = {},
    availableFonts: List<EpubFontOption> = EpubFontOption.DEFAULT_FONTS
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        AppearanceSheetContent(
            config = config,
            availableFonts = availableFonts,
            onThemeChange = onThemeChange,
            onFlowChange = onFlowChange,
            onFontSizeChange = onFontSizeChange,
            onFontFamilyChange = onFontFamilyChange,
            onLineHeightChange = onLineHeightChange,
            onMarginChange = onMarginChange,
            onTextAlignChange = onTextAlignChange,
            onResetDefaults = onResetDefaults
        )
    }
}

@Composable
private fun AppearanceSheetContent(
    config: EpubReaderConfig,
    availableFonts: List<EpubFontOption>,
    onThemeChange: (EpubReaderTheme) -> Unit,
    onFlowChange: (EpubReaderFlow) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onMarginChange: (Int) -> Unit,
    onTextAlignChange: (String) -> Unit,
    onResetDefaults: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppearanceHeader(onResetDefaults = onResetDefaults)
        AppearancePreviewCard(config = config)
        ThemePaletteSection(currentTheme = config.theme, onThemeChange = onThemeChange)
        ReadingModeSection(currentFlow = config.flow, onFlowChange = onFlowChange)
        TypographyCard(
            config = config,
            availableFonts = availableFonts,
            onFontFamilyChange = onFontFamilyChange,
            onFontSizeChange = onFontSizeChange
        )
        LayoutSpacingCard(
            config = config,
            onLineHeightChange = onLineHeightChange,
            onMarginChange = onMarginChange,
            onTextAlignChange = onTextAlignChange
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AppearanceHeader(onResetDefaults: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Reader Appearance",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            onClick = onResetDefaults,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "Reset to default settings",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AppearancePreviewCard(config: EpubReaderConfig) {
    val bg = parseHexColor(config.theme.backgroundColor)
    val fg = parseHexColor(config.theme.textColor)
    val previewSize = (config.fontSize * 0.9f).coerceIn(13f, 22f)
    val horizontalMargin = (config.margin * 0.35f).coerceIn(8f, 32f).dp

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bg),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, fg.copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = horizontalMargin, vertical = 14.dp)) {
            Text(
                text = "Aa  PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = fg.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = PREVIEW_TEXT,
                fontFamily = resolvePreviewFontFamily(config.fontFamily),
                fontSize = previewSize.sp,
                lineHeight = (previewSize * config.lineHeight).sp,
                textAlign = if (config.textAlign == "justify") TextAlign.Justify else TextAlign.Start,
                color = fg
            )
        }
    }
}

@Composable
private fun ThemePaletteSection(
    currentTheme: EpubReaderTheme,
    onThemeChange: (EpubReaderTheme) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "THEME",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            EpubReaderTheme.entries.forEach { theme ->
                ThemeSwatchItem(
                    theme = theme,
                    isSelected = currentTheme == theme,
                    onClick = { onThemeChange(theme) }
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatchItem(
    theme: EpubReaderTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = parseHexColor(theme.backgroundColor)
    val fg = parseHexColor(theme.textColor)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(bg)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f),
                    shape = CircleShape
                )
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = fg,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = theme.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingModeSection(
    currentFlow: EpubReaderFlow,
    onFlowChange: (EpubReaderFlow) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "READING MODE",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = currentFlow == EpubReaderFlow.PAGINATED,
                onClick = { onFlowChange(EpubReaderFlow.PAGINATED) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                label = { Text("Paginated") }
            )
            SegmentedButton(
                selected = currentFlow == EpubReaderFlow.SCROLLED,
                onClick = { onFlowChange(EpubReaderFlow.SCROLLED) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                label = { Text("Continuous") }
            )
        }
    }
}

@Composable
private fun TypographyCard(
    config: EpubReaderConfig,
    availableFonts: List<EpubFontOption>,
    onFontFamilyChange: (String) -> Unit,
    onFontSizeChange: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FontFamilyControl(
                availableFonts = availableFonts,
                currentFamily = config.fontFamily,
                onFontFamilyChange = onFontFamilyChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            FontSizeControl(fontSize = config.fontSize, onFontSizeChange = onFontSizeChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontFamilyControl(
    availableFonts: List<EpubFontOption>,
    currentFamily: String,
    onFontFamilyChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Font Family",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            availableFonts.forEachIndexed { index, font ->
                SegmentedButton(
                    selected = currentFamily == font.fontFamilyStack,
                    onClick = { onFontFamilyChange(font.fontFamilyStack) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = availableFonts.size),
                    label = {
                        Text(
                            text = font.displayName,
                            fontFamily = resolvePreviewFontFamily(font.fontFamilyStack)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun FontSizeControl(
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Font Size",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$fontSize sp",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { onFontSizeChange((fontSize - 1).coerceAtLeast(12)) },
                modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease font size")
            }
            Text("A", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = fontSize.toFloat(),
                onValueChange = { onFontSizeChange(it.roundToInt()) },
                valueRange = 12f..36f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Text("A", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(
                onClick = { onFontSizeChange((fontSize + 1).coerceAtMost(36)) },
                modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase font size")
            }
        }
    }
}

@Composable
private fun LayoutSpacingCard(
    config: EpubReaderConfig,
    onLineHeightChange: (Float) -> Unit,
    onMarginChange: (Int) -> Unit,
    onTextAlignChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LineSpacingControl(lineHeight = config.lineHeight, onLineHeightChange = onLineHeightChange)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PageMarginControl(margin = config.margin, onMarginChange = onMarginChange)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            TextAlignControl(textAlign = config.textAlign, onTextAlignChange = onTextAlignChange)
        }
    }
}

@Composable
private fun LineSpacingControl(
    lineHeight: Float,
    onLineHeightChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Line Spacing",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(lineHeight * 10f).roundToInt() / 10f}x",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FormatLineSpacing, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = lineHeight,
                onValueChange = { onLineHeightChange((it * 10f).roundToInt() / 10f) },
                valueRange = 1.2f..2.2f,
                steps = 9,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun PageMarginControl(
    margin: Int,
    onMarginChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Page Margins",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$margin px",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = margin.toFloat(),
                onValueChange = { onMarginChange(it.roundToInt()) },
                valueRange = 12f..64f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextAlignControl(
    textAlign: String,
    onTextAlignChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Text Alignment",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = textAlign == "justify",
                onClick = { onTextAlignChange("justify") },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    Icon(Icons.Default.FormatAlignJustify, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                label = { Text("Justify") }
            )
            SegmentedButton(
                selected = textAlign == "left",
                onClick = { onTextAlignChange("left") },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                label = { Text("Left") }
            )
        }
    }
}

private fun resolvePreviewFontFamily(stack: String): FontFamily = when {
    stack.contains("serif", ignoreCase = true) -> FontFamily.Serif
    stack.contains("monospace", ignoreCase = true) -> FontFamily.Monospace
    else -> FontFamily.Default
}

private fun parseHexColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    val colorInt = clean.toLongOrNull(16) ?: 0xFFFFFFFFL
    return when (clean.length) {
        6 -> Color(0xFF000000 or colorInt)
        8 -> Color(colorInt)
        else -> Color.White
    }
}
