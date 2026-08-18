package com.anshuman.tagstash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

@Composable
fun RenameDialog(
    file: File,
    onConfirmRename: (newName: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val initialName = remember(file) { file.name }
    val isDir = remember(file) { file.isDirectory }
    val originalExt = remember(file) { if (!isDir) file.extension else "" }

    val initialSelection = remember(file) {
        if (isDir || originalExt.isEmpty()) {
            TextRange(0, initialName.length)
        } else {
            val baseNameLength = file.nameWithoutExtension.length
            TextRange(0, baseNameLength)
        }
    }

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = initialSelection
            )
        )
    }

    val currentText = textFieldValue.text
    val trimmedText = currentText.trim()
    val isSameName = trimmedText == initialName
    val isEmpty = trimmedText.isEmpty()
    val hasInvalidChars = trimmedText.contains('/') || trimmedText.contains('\u0000')

    val isCollision = remember(trimmedText, file) {
        if (isEmpty || isSameName || hasInvalidChars) false
        else {
            val parent = file.parentFile ?: File("/")
            File(parent, trimmedText).exists()
        }
    }

    val isExtensionChanged = remember(currentText, originalExt, isDir, initialName) {
        if (isDir || originalExt.isEmpty() || currentText == initialName) false
        else {
            !currentText.endsWith(".$originalExt")
        }
    }

    val isValid = !isEmpty && !isSameName && !hasInvalidChars && !isCollision

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1E1E),
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFF2C2C2C))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DriveFileRenameOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isDir) "Rename Folder" else "Rename File",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = file.parent ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA0A0A0),
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close rename dialog",
                                tint = Color(0xFFA0A0A0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Multiline Text Field
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        label = { Text("Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Rename text input" },
                        minLines = 1,
                        maxLines = 4,
                        isError = hasInvalidChars || isCollision,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Error Message
                    if (hasInvalidChars) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Name cannot contain '/' characters",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (isCollision) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "An item with this name already exists",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Extension Change Warning Banner
                    if (isExtensionChanged && isValid) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF281C1B),
                            border = BorderStroke(1.dp, Color(0xFF5A2A26)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFFF8A65),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Extension Changed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFFFFCCBC)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Changing the file extension may prevent this file from opening properly in default apps.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFE0B4AB),
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(
                                    onClick = {
                                        val base = if (currentText.contains('.')) currentText.substringBeforeLast('.') else currentText
                                        val restoredText = "$base.$originalExt"
                                        textFieldValue = TextFieldValue(
                                            text = restoredText,
                                            selection = TextRange(base.length)
                                        )
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Undo Extension (.$originalExt)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFFAB91)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text("Cancel", color = Color(0xFFCCCCCC))
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                if (isValid) {
                                    onConfirmRename(trimmedText)
                                }
                            },
                            enabled = isValid,
                            modifier = Modifier.semantics { contentDescription = "Confirm Rename" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isExtensionChanged) Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                                disabledContainerColor = Color(0xFF2C2424),
                                disabledContentColor = Color(0xFF6E5656)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isExtensionChanged) "Rename (Keep Extension)" else "Rename",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
