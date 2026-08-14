package com.anshuman.tagstash.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CapacityLimitDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Clipboard Limit Reached", color = Color.White)
        },
        text = {
            Text(
                text = "You can only have up to 1000 items in the clipboard at a time. Please paste or clear items before adding more.",
                color = Color(0xFFA0A0A0)
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}
