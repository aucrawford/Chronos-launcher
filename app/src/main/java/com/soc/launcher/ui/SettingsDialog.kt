package com.soc.launcher.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    onBgSelected: (String) -> Unit,
    onAppSelected: (String, String) -> Unit,
    weatherApiKey: String,
    onApiKeyChanged: (String) -> Unit,
    newsApiKey: String,
    onNewsApiKeyChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val apps by produceState<List<AppInfo>>(initialValue = emptyList()) {
        withContext(Dispatchers.IO) {
            val result = getInstalledApps(context)
            Log.d("TemporalLauncher", "Loaded ${result.size} apps")
            value = result
        }
    }
    val bgPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onBgSelected(it.toString())
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(24.dp))

                Button(onClick = { bgPickerLauncher.launch(arrayOf("image/*")) }) {
                    Text("Pick Background Image")
                }

                Spacer(Modifier.height(24.dp))
                AppPicker("Search App", apps.filter { it.packageName.contains("search", true) || it.packageName.contains("browser", true) }) { onAppSelected("search", it) }
                AppPicker("AI App", apps.filter { it.packageName.contains("ai", true) || it.packageName.contains("chat", true) || it.packageName.contains("bard", true) }) { onAppSelected("ai", it) }
                AppPicker("News App", apps.filter { it.packageName.contains("news", true) || it.packageName.contains("magazines", true) }) { onAppSelected("news", it) }

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
                Text("News API (Optional)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newsApiKey,
                    onValueChange = onNewsApiKeyChanged,
                    label = { Text("NewsAPI.org Key") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray
                    )
                )

                Spacer(Modifier.height(24.dp))
                Text("Permissions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))

                PermissionLink("App Permissions") {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }

                Spacer(Modifier.height(12.dp))
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
