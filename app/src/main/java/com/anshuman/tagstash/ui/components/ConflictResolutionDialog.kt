package com.anshuman.tagstash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anshuman.tagstash.data.utils.ConflictResolution
import java.io.File

@Composable
fun ConflictResolutionDialog(
    conflictingFile: File,
    onResolve: (resolution: ConflictResolution, applyToAll: Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    var applyToAll by remember { mutableStateOf(false) }

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
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1E1E),
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFF2C2C2C))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "File Conflict",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFFA0A0A0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "A file named \"${conflictingFile.name}\" already exists in this folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Choose an action to resolve the conflict:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0A0A0)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Replace option
                        Button(
                            onClick = { onResolve(ConflictResolution.REPLACE, applyToAll) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Replace Existing File",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Keep Both option
                        Button(
                            onClick = { onResolve(ConflictResolution.KEEP_BOTH, applyToAll) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2A2A2A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Keep Both (Rename with copy)",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Skip option
                        OutlinedButton(
                            onClick = { onResolve(ConflictResolution.SKIP, applyToAll) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF3E3E3E)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFA0A0A0)
                            )
                        ) {
                            Text(
                                text = "Skip This File",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Apply to all conflicts checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyToAll = !applyToAll }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = applyToAll,
                            onCheckedChange = { applyToAll = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = Color(0xFFA0A0A0)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apply to all conflicts",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
