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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.util.Locale

/** Shared colour chooser for profiles and future user-defined labels. */
internal val profilePresetColors = listOf(
    "#E53935", "#D81B60", "#8E24AA", "#5E35B1",
    "#3949AB", "#1E88E5", "#039BE5", "#00ACC1",
    "#00897B", "#43A047", "#7CB342", "#FDD835",
    "#FFB300", "#FB8C00", "#F4511E", "#546E7A",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerContent(
    selectedColor: String,
    onColorChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            profilePresetColors.forEach { hex ->
                val color = parseProfileColor(hex, MaterialTheme.colorScheme.primary)
                val isSelected = selectedColor.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
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
                                    width = Dimensions.Component.dividerThickness * 3,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onColorChanged(hex) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isLightColor(color)) {
                                Color.Black.copy(alpha = Dimensions.Alpha.high)
                            } else {
                                Color.White
                            },
                            modifier = Modifier.size(Dimensions.Icon.small),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))
        CustomProfileColorSection(
            selectedColor = selectedColor,
            onColorChanged = onColorChanged,
        )
    }
}

@Composable
private fun CustomProfileColorSection(
    selectedColor: String,
    onColorChanged: (String) -> Unit,
) {
    val fallback = MaterialTheme.colorScheme.primary
    val initialHsv = remember(selectedColor) {
        val argb = parseProfileColor(selectedColor, fallback).toArgb()
        FloatArray(3).also { android.graphics.Color.colorToHSV(argb, it) }
    }
    var hue by remember(selectedColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(selectedColor) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(selectedColor) { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by remember(selectedColor) {
        mutableStateOf(selectedColor.removePrefix("#"))
    }
    var hexError by remember(selectedColor) { mutableStateOf(false) }

    fun emitFromHsv() {
        val argb = android.graphics.Color.HSVToColor(
            floatArrayOf(hue, saturation, brightness),
        )
        val hex = String.format(Locale.US, "#%06X", 0xFFFFFF and argb)
        hexInput = hex.removePrefix("#")
        hexError = false
        onColorChanged(hex)
    }

    Text(
        text = stringResource(R.string.profile_color_custom),
        style = MaterialTheme.typography.titleSmall,
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    ProfileColorSlider(
        label = stringResource(R.string.profile_color_hue),
        value = hue,
        valueRange = 0f..360f,
        onValueChange = { hue = it },
        onValueChangeFinished = ::emitFromHsv,
    )
    ProfileColorSlider(
        label = stringResource(R.string.profile_color_saturation),
        value = saturation,
        valueRange = 0f..1f,
        onValueChange = { saturation = it },
        onValueChangeFinished = ::emitFromHsv,
    )
    ProfileColorSlider(
        label = stringResource(R.string.profile_color_brightness),
        value = brightness,
        valueRange = 0f..1f,
        onValueChange = { brightness = it },
        onValueChangeFinished = ::emitFromHsv,
    )

    Spacer(modifier = Modifier.height(Spacing.sm))
    OutlinedTextField(
        value = hexInput,
        onValueChange = { raw ->
            val cleaned = raw
                .removePrefix("#")
                .take(6)
                .uppercase(Locale.US)
            hexInput = cleaned
            if (cleaned.length == 6 && cleaned.all { it.isDigit() || it in 'A'..'F' }) {
                hexError = false
                onColorChanged("#$cleaned")
            } else {
                hexError = cleaned.isNotEmpty()
            }
        },
        label = { Text(stringResource(R.string.profile_color_hex)) },
        prefix = { Text("#") },
        singleLine = true,
        isError = hexError,
        supportingText = if (hexError) {
            { Text(stringResource(R.string.profile_color_hex_error)) }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProfileColorSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
        )
    }
}

internal fun parseProfileColor(hex: String, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

private fun isLightColor(color: Color): Boolean =
    (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue) > 0.6f
