package com.soc.launcher.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.soc.launcher.data.model.AppInfo
import com.soc.launcher.getInstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    currentAiPkg: String,
    onAppSelected: (String, String) -> Unit,
    weatherApiKey: String,
    onApiKeyChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val apps by produceState<List<AppInfo>>(initialValue = emptyList()) {
        withContext(Dispatchers.IO) {
            val result = getInstalledApps(context)
            Log.d("TemporalLauncher", "Loaded ${result.size} apps")
            value = result
        }
    }

    var permissionsExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Change Wallpaper",
                    color = Color(0xFF4A90E2),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                            context.startActivity(intent)
                        }
                        .padding(vertical = 12.dp)
                )

                Spacer(Modifier.height(24.dp))
                AppSelectionRow(
                    label = "Fallback AI App",
                    currentPkg = currentAiPkg,
                    options = apps.filter { app ->
                        val p = app.packageName.lowercase()
                        val n = app.name.lowercase()
                        val isAiKeyword = (p.contains(".ai") || p.endsWith(".ai") || p.contains("ai.")) && !p.contains("mail")
                        val isSpecificAi = p.contains("chat") ||
                                          p.contains("bard") ||
                                          p.contains("gemini") ||
                                          p.contains("openai") ||
                                          p.contains("claude") ||
                                          p.contains("perplexity") ||
                                          p.contains("lumos") ||
                                          n.contains("lumos") ||
                                          n.contains("chatgpt") ||
                                          n.contains("gemini") ||
                                          n.contains("claude")

                        isSpecificAi || isAiKeyword
                    }
                ) { onAppSelected("ai", it) }

                Spacer(Modifier.height(24.dp))
                Text("Weather", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = weatherApiKey,
                    onValueChange = onApiKeyChanged,
                    label = { Text("OpenWeatherMap API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray
                    )
                )
                Text(
                    text = "Get a free key to add local weather to your home screen",
                    color = Color(0xFF4A90E2),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://openweathermap.org/api"))
                            context.startActivity(intent)
                        }
                        .padding(top = 8.dp)
                )

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { permissionsExpanded = !permissionsExpanded }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Permissions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = if (permissionsExpanded) "▲" else "▼",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                AnimatedVisibility(visible = permissionsExpanded) {
                    Column {
                        PermissionLink("App Permissions") {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }

                        PermissionLink("Set Default Launcher") {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                Intent(Settings.ACTION_HOME_SETTINGS)
                            } else {
                                Intent(Settings.ACTION_SETTINGS)
                            }
                            context.startActivity(intent)
                        }
                        PermissionLink("Usage Access") {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }
                        PermissionLink("Notification Access") {
                            context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AppSelectionRow(label: String, currentPkg: String, options: List<AppInfo>, onSelected: (String) -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val currentApp = remember(currentPkg, options) {
        options.find { it.packageName == currentPkg }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (currentApp != null) {
                    val icon = remember(currentApp.packageName) {
                        try {
                            context.packageManager.getApplicationIcon(currentApp.packageName)
                        } catch (e: Exception) {
                            context.packageManager.defaultActivityIcon
                        }
                    }
                    Image(
                        bitmap = icon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = currentApp.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("None selected", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }

            Text(
                text = "CHANGE",
                color = Color(0xFF4A90E2),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(8.dp)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF2A2A2A))
            ) {
                options.forEach { app ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = remember(app.packageName) {
                                    try {
                                        context.packageManager.getApplicationIcon(app.packageName)
                                    } catch (e: Exception) {
                                        context.packageManager.defaultActivityIcon
                                    }
                                }
                                Image(
                                    bitmap = icon.toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(app.name, color = Color.White)
                            }
                        },
                        onClick = {
                            onSelected(app.packageName)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionLink(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color(0xFF4A90E2),
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}

@Composable
fun AppPicker(label: String, options: List<AppInfo>, onSelected: (String) -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Box(modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 8.dp)) {
            Text("Select App...", color = Color.White)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF2A2A2A))
        ) {
            options.forEach { app ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = remember(app.packageName) {
                                try {
                                    context.packageManager.getApplicationIcon(app.packageName)
                                } catch (e: Exception) {
                                    context.packageManager.defaultActivityIcon
                                }
                            }
                            Image(
                                bitmap = icon.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(app.name, color = Color.White)
                        }
                    },
                    onClick = {
                        onSelected(app.packageName)
                        expanded = false
                    }
                )
            }
        }
    }
}
