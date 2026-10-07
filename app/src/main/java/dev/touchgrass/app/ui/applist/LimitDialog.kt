package dev.touchgrass.app.ui.applist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.touchgrass.app.R
import dev.touchgrass.app.limits.LimitStore
import dev.touchgrass.app.ui.theme.TouchGrassTheme

private val PresetMinutes = listOf(15, 30, 45, 60, 90)

@Composable
fun LimitDialog(
    appLabel: String,
    currentLimit: Int?,
    onSave: (minutes: Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(currentLimit?.toString().orEmpty()) }
    val minutes = input.toIntOrNull()?.takeIf { it in LimitStore.VALID_MINUTES }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetMinutes.forEach { preset ->
                        FilterChip(
                            selected = minutes == preset,
                            onClick = { input = preset.toString() },
                            label = { Text(stringResource(R.string.minutes, preset)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.limit_custom)) },
                    suffix = { Text(stringResource(R.string.minutes_suffix)) },
                    isError = input.isNotEmpty() && minutes == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { minutes?.let(onSave) }, enabled = minutes != null) {
                Text(stringResource(R.string.limit_save))
            }
        },
        dismissButton = {
            if (currentLimit != null) {
                TextButton(onClick = onRemove) { Text(stringResource(R.string.limit_remove)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Preview
@Composable
private fun LimitDialogPreview() {
    TouchGrassTheme {
        LimitDialog(appLabel = "Instagram", currentLimit = 30, onSave = {}, onRemove = {}, onDismiss = {})
    }
}
