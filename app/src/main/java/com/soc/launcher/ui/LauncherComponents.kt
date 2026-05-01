package com.soc.launcher.ui

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.soc.launcher.MediaNotificationListener
import com.soc.launcher.data.model.AppInfo
import com.soc.launcher.isNotificationServiceEnabled
import com.soc.launcher.data.model.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.net.Uri
import android.content.Context
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager

@Composable
fun StatItem(text: String, color: Color, fontSize: TextUnit, onClick: () -> Unit) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable(onClick = onClick),
        style = TextStyle(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 1f),
                offset = Offset(0f, 0f),
                blurRadius = 6f
            )
        )
    )
}

@Composable
fun MediaControlSection() {
    val mediaInfo by MediaNotificationListener.mediaInfo.collectAsState()

    AnimatedVisibility(
        visible = mediaInfo.title.isNotEmpty() && mediaInfo.packageName.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
                .padding(16.dp)
        ) {
            Text(
                text = mediaInfo.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 1f),
                        offset = Offset(0f, 0f),
                        blurRadius = 6f
                    )
                )
            )
            Text(
                text = mediaInfo.artist,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 1f),
                        offset = Offset(0f, 0f),
                        blurRadius = 6f
                    )
                )
            )

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                IconButton(onClick = { MediaNotificationListener.sendCommand("previous") }, modifier = Modifier.size(32.dp)) {
                    Text("«", color = Color.White, fontSize = 24.sp)
                }
                IconButton(
                    onClick = {
                        if (mediaInfo.isPlaying) MediaNotificationListener.sendCommand("pause")
                        else MediaNotificationListener.sendCommand("play")
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    if (mediaInfo.isPlaying) {
                        Text("‖", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                IconButton(onClick = { MediaNotificationListener.sendCommand("next") }, modifier = Modifier.size(32.dp)) {
                    Text("»", color = Color.White, fontSize = 24.sp)
                }
            }
        }
    }

    if (mediaInfo.title.isEmpty()) {
        val context = LocalContext.current
        var notificationPermissionGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    notificationPermissionGranted = isNotificationServiceEnabled(context)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        if (!notificationPermissionGranted) {
            Button(
                onClick = {
                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                },
                modifier = Modifier.padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enable Media Controls", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

private fun launchSpecificAi(context: Context, pkg: String) {
    if (pkg.isNotEmpty()) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            try {
                context.startActivity(intent)
            } catch (e: Exception) {}
        }
    }
}

@Composable
fun Section(
    title: String,
    items: List<String>,
    onItemClick: ((Int) -> Unit)? = null,
    onPermissionClick: (() -> Unit)? = null,
    onClearClick: (() -> Unit)? = null,
    itemIcons: List<String?>? = null,
    itemSpamFlags: List<Boolean>? = null,
    maxItems: Int = Int.MAX_VALUE
) {
    val displayItems = items.take(maxItems)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title.uppercase(),
                color = Color(0xFF4A90E2),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            if (onClearClick != null && items.isNotEmpty() && items.first() != "No recent missed calls" && items.first() != "No recent messages") {
                Text(
                    "CLEAR",
                    color = Color(0xFF4A90E2).copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onClearClick() }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (onPermissionClick != null) {
            Button(
                onClick = onPermissionClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Grant Permission", color = Color.White, fontSize = 12.sp)
            }
        } else {
            displayItems.forEachIndexed { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = onItemClick != null) { onItemClick?.invoke(index) }
                        .padding(vertical = 8.dp)
                ) {
                    if (itemSpamFlags != null && index < itemSpamFlags.size && itemSpamFlags[index]) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Spam",
                            tint = Color.Red.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    } else if (itemIcons != null && index < itemIcons.size) {
                        val iconUri = itemIcons[index]
                        if (iconUri != null) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (item != "No recent missed calls" && item != "No recent messages") {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (iconUri != null || (item != "No recent missed calls" && item != "No recent messages")) {
                            Spacer(Modifier.width(16.dp))
                        }
                    }
                    Text(
                        text = item,
                        color = if (itemSpamFlags != null && index < itemSpamFlags.size && itemSpamFlags[index]) Color.White.copy(alpha = 0.6f) else Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AppIcon(app: AppInfo, modifier: Modifier = Modifier, showLabel: Boolean = true) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName).toBitmap().asImageBitmap()
        } catch (e: Exception) {
            Log.e("TemporalLauncher", "Error loading icon for ${app.packageName}", e)
            context.packageManager.defaultActivityIcon.toBitmap().asImageBitmap()
        }
    }

    Column(
        modifier = modifier.clickable {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {}
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val boxModifier = if (showLabel) {
            Modifier.size(48.dp).background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
        } else {
            Modifier.fillMaxSize()
        }
        Box(
            modifier = boxModifier,
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = icon,
                contentDescription = app.name,
                modifier = if (showLabel) Modifier.fillMaxSize(0.6f) else Modifier.fillMaxSize()
            )
        }
        if (showLabel) {
            Text(
                text = app.name,
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
