package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import com.pennywiseai.tracker.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.util.Locale

/**
 * The app's established category hues.
 *
 * Moved here verbatim from `CategoryEditDialog`. **Do not replace these with
 * Cashiro's list** — they are the colours every existing user's categories are
 * already using, and swapping them would recolour someone's whole taxonomy the
 * next time they opened an edit sheet.
 */
val presetColors = listOf(
    "#E53935", "#D81B60", "#8E24AA", "#5E35B1",
    "#3949AB", "#1E88E5", "#039BE5", "#00ACC1",
    "#00897B", "#43A047", "#7CB342", "#C0CA33",
    "#FDD835", "#FFB300", "#FB8C00", "#F4511E",
    "#6D4C41", "#757575", "#546E7A", "#1565C0"
)

/**
 * Colour chooser: preset swatches, plus an HSV picker with a hex field.
 *
 * @param selectedColor current value as `#RRGGBB`.
 * @param onColorChanged emits `#RRGGBB`.
 * @param showCustom when false, only the preset grid renders.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerContent(
    selectedColor: String,
    onColorChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    showCustom: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            presetColors.forEach { hex ->
                val color = parseColor(hex, MaterialTheme.colorScheme.primary)
                val isSelected = selectedColor.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        // A bare coloured Box tells a screen reader nothing at
                        // all — not which colour, not whether it is the chosen
                        // one. The hex is the only stable name a colour has here.
                        .semantics {
                            contentDescription = hex
                            selected = isSelected
                            role = Role.RadioButton
                        }
                        .size(Dimensions.Component.minTouchTarget)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    Dimensions.Component.dividerThickness * 3,
                                    MaterialTheme.colorScheme.onSurface,
                                    CircleShape
                                )
                            } else Modifier
                        )
                        .clickable { onColorChanged(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            // Contrast is decided against the swatch itself, not the
                            // theme — a check mark that disappears on yellow is the
                            // whole reason this helper exists.
                            tint = if (isLightColor(color)) {
                                Color.Black.copy(alpha = 0.87f)
                            } else {
                                Color.White
                            },
                            modifier = Modifier.size(Dimensions.Icon.small)
                        )
                    }
                }
            }
        }

        if (showCustom) {
            Spacer(modifier = Modifier.height(Spacing.md))
            CustomColorSection(selectedColor = selectedColor, onColorChanged = onColorChanged)
        }
    }
}

@Composable
private fun CustomColorSection(
    selectedColor: String,
    onColorChanged: (String) -> Unit,
) {
    val fallback = MaterialTheme.colorScheme.primary

    // Derived ONCE per incoming colour. Cashiro recomputes HSV on every
    // recomposition, so dragging a slider fights the value it just produced and
    // the handle stutters back.
    val initialHsv = remember(selectedColor) {
        val argb = parseColor(selectedColor, fallback).toArgb()
        FloatArray(3).also { android.graphics.Color.colorToHSV(argb, it) }
    }

    var hue by remember(selectedColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(selectedColor) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(selectedColor) { mutableFloatStateOf(initialHsv[2]) }

    // Separate from `selectedColor` so typing "#1E8" does not repaint anything
    // until it is a complete colour.
    var hexInput by remember(selectedColor) { mutableStateOf(selectedColor.removePrefix("#")) }
    var hexError by remember(selectedColor) { mutableStateOf(false) }

    fun emitFromHsv() {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        val hex = String.format(Locale.US, "#%06X", 0xFFFFFF and argb)
        hexInput = hex.removePrefix("#")
        hexError = false
        onColorChanged(hex)
    }

    Text(
        text = stringResource(R.string.color_custom),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.height(Spacing.sm))

    LabelledSlider(
        label = stringResource(R.string.color_hue),
        value = hue,
        valueRange = 0f..360f,
        onValueChange = { hue = it },
        onValueChangeFinished = ::emitFromHsv
    )
    LabelledSlider(
        label = stringResource(R.string.color_saturation),
        value = saturation,
        valueRange = 0f..1f,
        onValueChange = { saturation = it },
        onValueChangeFinished = ::emitFromHsv
    )
    LabelledSlider(
        label = stringResource(R.string.color_brightness),
        value = value,
        valueRange = 0f..1f,
        onValueChange = { value = it },
        onValueChangeFinished = ::emitFromHsv
    )

    Spacer(modifier = Modifier.height(Spacing.sm))

    OutlinedTextField(
        value = hexInput,
        onValueChange = { raw ->
            val cleaned = raw.removePrefix("#").take(6).uppercase(Locale.US)
            hexInput = cleaned
            // Commit only on a complete, valid value. Updating per keystroke makes
            // the swatch strobe through nonsense colours while the user types.
            if (cleaned.length == 6 && cleaned.all { it.isDigit() || it in 'A'..'F' }) {
                hexError = false
                onColorChanged("#$cleaned")
            } else {
                hexError = cleaned.isNotEmpty()
            }
        },
        label = { Text(stringResource(R.string.color_hex)) },
        prefix = { Text("#") },
        singleLine = true,
        isError = hexError,
        supportingText = if (hexError) {
            { Text(stringResource(R.string.color_hex_error)) }
        } else null,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LabelledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            // Emitting only when the drag ends keeps every intermediate colour out
            // of the caller's state, so the preview updates once per gesture.
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange
        )
    }
}
