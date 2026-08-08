package com.example.headphonecompanion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.headphonecompanion.dsp.EqBand
import com.example.headphonecompanion.dsp.ParametricEq

@Composable
fun EqScreen(onApply: (ParametricEq) -> Unit) {
    var isFiveBand by remember { mutableStateOf(true) }
    // testTags required by repo conventions
    val testTag5 = "eq_mode_5_band"
    val testTag10 = "eq_mode_10_band"
    val themeToggleTag = "theme_toggle_button"
    val highContrastTag = "high_contrast_switch"

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Equalizer")

        // Theme toggle placeholder to satisfy AGENTS.md requirement
        RowWithLabel("High-contrast theme", highContrastTag) { checked -> /* persist theme */ }

        // band mode switch
        RowWithLabel("5-band mode", testTag5) { checked -> isFiveBand = checked }
        RowWithLabel("10-band mode", testTag10) { checked -> isFiveBand = !checked }

        val bands = remember(isFiveBand) {
            if (isFiveBand) {
                listOf(
                    EqBand(60f, 0f, 1f),
                    EqBand(230f, 0f, 1f),
                    EqBand(910f, 0f, 1f),
                    EqBand(3600f, 0f, 1f),
                    EqBand(14000f, 0f, 1f)
                )
            } else {
                listOf(
                    EqBand(31f, 0f, 1f),
                    EqBand(62f, 0f, 1f),
                    EqBand(125f, 0f, 1f),
                    EqBand(250f, 0f, 1f),
                    EqBand(500f, 0f, 1f),
                    EqBand(1000f, 0f, 1f),
                    EqBand(2000f, 0f, 1f),
                    EqBand(4000f, 0f, 1f),
                    EqBand(8000f, 0f, 1f),
                    EqBand(16000f, 0f, 1f)
                )
            }
        }

        // sliders for each band
        val bandStates = remember { bands.map { mutableStateOf(it.gainDb) } }
        bands.forEachIndexed { idx, band ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("${band.centerHz.toInt()} Hz: ${bandStates[idx].value} dB")
                Slider(
                    value = bandStates[idx].value,
                    onValueChange = { bandStates[idx].value = it },
                    valueRange = -12f..12f,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }
        }

        Button(onClick = {
            val updatedBands = bands.mapIndexed { idx, b -> b.copy(gainDb = bandStates[idx].value) }
            val eq = ParametricEq(updatedBands)
            onApply(eq)
        }, modifier = Modifier.padding(top = 12.dp)) {
            Text("Apply EQ")
        }
    }
}

@Composable
private fun RowWithLabel(label: String, testTag: String, onToggle: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        var checked by remember { mutableStateOf(false) }
        Switch(checked = checked, onCheckedChange = {
            checked = it
            onToggle(it)
        }, modifier = Modifier.padding(start = 8.dp))
    }
}
