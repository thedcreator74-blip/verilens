package com.example.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PasteTextVerifyDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Verify Text Statement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Paste any suspicious headline, forwarded message, or news claim to evaluate its credibility against trusted records.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                        textInput = it
                        if (error != null) error = null
                    },
                    label = { Text("Enter or paste claim text...") },
                    minLines = 3,
                    maxLines = 6,
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(text = "Think Before You Share")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_paste_text_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = textInput.trim()
                    if (trimmed.length < 5) {
                        error = "Please enter at least 5 characters to analyze."
                    } else {
                        onVerify(trimmed)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_paste_text_confirm")
            ) {
                Text("Analyze")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun PasteLinkVerifyDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Verify Web Link",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Paste an article or announcement URL to inspect domain authority, author history, and verifiable facts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        if (error != null) error = null
                    },
                    label = { Text("https://example.com/news-story") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(text = "Domain reputation & fact-check cross-reference")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_paste_link_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = urlInput.trim()
                    if (trimmed.length < 8 || (!trimmed.startsWith("http://") && !trimmed.startsWith("https://"))) {
                        error = "Please enter a valid URL starting with http:// or https://"
                    } else {
                        onVerify(trimmed)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_paste_link_confirm")
            ) {
                Text("Inspect Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
