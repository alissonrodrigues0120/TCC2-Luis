package com.project.ui.home

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.project.ui.components.TooltipIconButton


@Composable
fun ImagePickerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit,
    tempImageUri: Uri
) {
    if (!showDialog) return
    val context = LocalContext.current

    // Launcher for Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { onImageSelected(it) }
            onDismiss()
        }
    )

    // Launcher for Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                onImageSelected(tempImageUri)
            }
            onDismiss()
        }
    )

    // Launcher for Permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                cameraLauncher.launch(tempImageUri)
            } else {
                onDismiss()
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar Imagem") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    modifier = Modifier.clickable {
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    headlineContent = { Text("Galeria") },
                    leadingContent = { Icon(Icons.Default.List, contentDescription = null) }
                )
                ListItem(
                    modifier = Modifier.clickable {
                        val permission = android.Manifest.permission.CAMERA
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch(tempImageUri)
                        } else {
                            permissionLauncher.launch(permission)
                        }
                    },
                    headlineContent = { Text("Câmera") },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
