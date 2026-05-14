package com.soc.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.soc.launcher.data.model.AppInfo
import com.soc.launcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PresentSearchBar(
    aiPkg: String,
    aiApps: List<AppInfo>,
    mapApps: List<AppInfo>,
    currentMapsPkg: String,
    query: String = "",
    onQueryChange: ((String) -> Unit)? = null,
    placeholder: String = "Search...",
    onAiAppSelected: (String) -> Unit,
    onMapAppSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val installedAiApps = remember(aiApps) {
        aiApps.distinctBy { it.packageName }
            .sortedBy { it.name }
    }
    val effectiveAiPkg = remember(aiPkg, installedAiApps) {
        val current = installedAiApps.find { it.packageName == aiPkg }
        if (current != null) {
            aiPkg
        } else {
            val priority = listOf(
                "com.google.android.apps.gemini",
                "com.google.android.apps.googleassistant"
            )
            val fallback = priority.firstOrNull { p -> installedAiApps.any { it.packageName == p } }
            fallback ?: installedAiApps.firstOrNull()?.packageName ?: ""
        }
    }

    val aiIcon = remember(effectiveAiPkg) {
        try {
            if (effectiveAiPkg.isNotEmpty()) {
                context.packageManager.getApplicationIcon(effectiveAiPkg).toBitmap().asImageBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    val mapsIcon = remember(currentMapsPkg) {
        try {
            context.packageManager.getApplicationIcon(currentMapsPkg).toBitmap().asImageBitmap()
        } catch (e: Exception) { null }
    }

    var showMapsMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SecondaryBackground)
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { onQueryChange?.invoke(it) },
            modifier = Modifier
                .weight(1f)
                .height(80.dp)
                .padding(16.dp),
            placeholder = {
                Text(
                    placeholder,
                    color = FoltrainWhite.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = FoltrainMain,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange?.invoke("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = FoltrainMain, modifier = Modifier.size(20.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FoltrainWhite.copy(alpha = 0.5f),
                unfocusedBorderColor = FoltrainMain,
                focusedTextColor = FoltrainWhite,
                unfocusedTextColor = FoltrainWhite,
                cursorColor = FoltrainMain,
                focusedContainerColor = FoltrainWhite.copy(alpha = 0.03f),
                unfocusedContainerColor = FoltrainWhite.copy(alpha = 0.03f)
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                    context.startActivity(intent)
                    onQueryChange?.invoke("")
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(null, 0)
                }
            })
        )

        // Maps Icon
        Box(modifier = Modifier
            .size(44.dp)
            .combinedClickable(
                onClick = {
                    val intent = context.packageManager.getLaunchIntentForPackage(currentMapsPkg)
                    if (intent != null) context.startActivity(intent)
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMapsMenu = true
                }
            )
        ) {
            if (mapsIcon != null) {
                Image(bitmap = mapsIcon, contentDescription = "Maps", modifier = Modifier.size(44.dp))
            }

            DropdownMenu(
                expanded = showMapsMenu,
                onDismissRequest = { showMapsMenu = false },
                modifier = Modifier.background(Color(0xFF2A2A2A))
            ) {
                mapApps.forEach { app ->
                    val appIcon = remember(app.packageName) {
                        try {
                            context.packageManager.getApplicationIcon(app.packageName).toBitmap().asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (appIcon != null) {
                                    Image(
                                        bitmap = appIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(app.name, color = FoltrainWhite)
                            }
                        },
                        onClick = {
                            onMapAppSelected(app.packageName)
                            showMapsMenu = false
                        }
                    )
                }
            }
        }

        // AI Icon
        if (effectiveAiPkg.isNotEmpty()) {
            if (mapsIcon != null) {
                Spacer(Modifier.width(16.dp))
            }
            var showAiMenu by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .combinedClickable(
                        onClick = {
                            val intent = context.packageManager.getLaunchIntentForPackage(effectiveAiPkg)
                            if (intent != null) {
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAiMenu = true
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (aiIcon != null) {
                    Image(bitmap = aiIcon, contentDescription = "AI Assistant", modifier = Modifier.size(44.dp))
                } else {
                    Icon(Icons.Default.Star, contentDescription = "AI Assistant", tint = FoltrainMain, modifier = Modifier.size(44.dp))
                }

                DropdownMenu(
                    expanded = showAiMenu,
                    onDismissRequest = { showAiMenu = false },
                    modifier = Modifier.background(Color(0xFF2A2A2A))
                ) {
                    installedAiApps.filter { it.packageName != effectiveAiPkg }.forEach { app ->
                        val appIcon = remember(app.packageName) {
                            try {
                                context.packageManager.getApplicationIcon(app.packageName).toBitmap().asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (appIcon != null) {
                                        Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(text = app.name, color = FoltrainWhite)
                                }
                            },
                            onClick = {
                                onAiAppSelected(app.packageName)
                                showAiMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}