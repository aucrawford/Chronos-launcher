package com.soc.launcher.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import com.soc.launcher.*
import android.provider.Settings
import com.soc.launcher.data.model.*
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.nativeCanvas
import android.provider.MediaStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import android.widget.Toast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share

/**
 * PastScreen displays historical usage data and recent media.
 *
 * Visual Consistency:
 * - Root background alpha: 0.85
 * - Section containers: Color.White.copy(alpha = 0.03f)
 *
 * Usage Logic:
 * - Weekly Graph: Displays true Screen-On Time using UsageEvents (SCREEN_INTERACTIVE).
 * - App Activity List: Displays total running time (foreground + background).
 * - Density: High-density app list with 28dp icons and 6dp vertical padding.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PastScreen(
    apps: List<AppInfo>
) {
    val context = LocalContext.current

    val imagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasImagesPermission by remember { mutableStateOf(context.checkSelfPermission(imagesPermission) == PackageManager.PERMISSION_GRANTED) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasImagesPermission = permissions[imagesPermission] ?: hasImagesPermission
    }

    val sharedPrefs = remember { context.getSharedPreferences("launcher_prefs", android.content.Context.MODE_PRIVATE) }

    val lifecycleOwner = LocalLifecycleOwner.current
    
    // MediaStore Images Logic
    var recentImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var excludedImages by remember { 
        mutableStateOf(sharedPrefs.getStringSet("excluded_images", emptySet()) ?: emptySet()) 
    }

    LaunchedEffect(hasImagesPermission, excludedImages, lifecycleOwner) {
        if (hasImagesPermission) {
            val images = withContext(Dispatchers.IO) {
                getRecentCameraImages(context, excludedImages)
            }
            recentImages = images
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val currentExcludedImages by rememberUpdatedState(excludedImages)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasImagesPermission) {
                    coroutineScope.launch(Dispatchers.IO) {
                         val images = getRecentCameraImages(context, currentExcludedImages)
                         withContext(Dispatchers.Main) { recentImages = images }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var appUsage by remember { mutableStateOf<List<AppUsageInfo>>(emptyList()) }
    var weeklyUsage by remember { mutableStateOf<List<Long>>(emptyList()) }
    var hasUsagePermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }

    LaunchedEffect(hasUsagePermission, apps) {
        if (hasUsagePermission) {
            appUsage = getDailyAppUsage(context, apps).take(20)
            weeklyUsage = getWeeklyUsageData(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A10).copy(alpha = 0.85f)),
    ) {
        // App Usage Section (Darker Background) - Takes remaining space
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.15f))
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 0.dp)
        ) {
            AppUsageHeader(
                hasPermission = hasUsagePermission,
                onRequestPermission = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
            )

            if (!hasUsagePermission) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Grant Usage Stats Permission to see your phone usage.".uppercase(),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (appUsage.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No usage data available yet.".uppercase(),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(appUsage) { usage ->
                        AppUsageItem(usage)
                    }
                }
            }
        }

        // Screen Time Section (0.03 Background) - Height dependent on content
        if (hasUsagePermission && weeklyUsage.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
            ) {
                Text(
                    "Screen Time".uppercase(),
                    color = Color(0xFF4A90E2),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                UsageGraph(weeklyUsage)
            }
        }

        // Recent Photos Section (Darker Background) - Height dependent on content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.15f))
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.type = "image/*"
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("PastScreen", "Failed to open gallery", e)
                        }
                    }
            ) {
                Text(
                    "Recent Photos (${recentImages.size})".uppercase(),
                    color = Color(0xFF4A90E2),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            if (!hasImagesPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable { launcher.launch(arrayOf(imagesPermission)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Grant Permission to see recent photos".uppercase(),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (recentImages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No photos found".uppercase(),
                        color = Color.White.copy(alpha = 0.2f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    recentImages.chunked(3).forEach { rowImages ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowImages.forEach { uri ->
                                Box(modifier = Modifier.weight(1f)) {
                                    RecentImageItem(
                                        uri = uri,
                                        onRemove = {
                                            val newExcluded = excludedImages.toMutableSet()
                                            newExcluded.add(uri.toString())
                                            excludedImages = newExcluded
                                            sharedPrefs.edit().putStringSet("excluded_images", newExcluded).apply()
                                        }
                                    )
                                }
                            }
                            repeat(3 - rowImages.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun AppUsageHeader(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
    ) {
        Text(
            "App Usage".uppercase(),
            color = Color(0xFF4A90E2),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Default.Info,
            contentDescription = "Info",
            tint = Color(0xFF4A90E2).copy(alpha = 0.6f),
            modifier = Modifier
                .size(14.dp)
                .clickable {
                    Toast
                        .makeText(
                            context,
                            "App Activity times are individually based on total running time, not just screen time.",
                            Toast.LENGTH_LONG
                        )
                        .show()
                }
        )
    }
}

@Composable
fun AppUsageItem(usage: AppUsageInfo) {
    val context = LocalContext.current
    val hours = usage.totalTimeInForeground / (1000 * 60 * 60)
    val minutes = (usage.totalTimeInForeground / (1000 * 60)) % 60
    val timeStr = when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }

    val icon = remember(usage.packageName) {
        try {
            context.packageManager.getApplicationIcon(usage.packageName).toBitmap().asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = context.packageManager.getLaunchIntentForPackage(usage.packageName)
                if (intent != null) {
                    context.startActivity(intent)
                }
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = usage.name,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = timeStr,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentImageItem(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    
    val thumbnail by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = uri) {
        value = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(300, 300), null)
                } else {
                    MediaStore.Images.Thumbnails.getThumbnail(
                        context.contentResolver,
                        uri.lastPathSegment?.toLong() ?: 0L,
                        MediaStore.Images.Thumbnails.MINI_KIND,
                        null
                    )
                }
            } catch (e: Exception) {
                Log.e("PastScreen", "Failed to load thumbnail", e)
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .combinedClickable(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(uri, "image/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                },
                onLongClick = { showMenu = true }
            )
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(Color(0xFF1A1A1A))
        ) {
            DropdownMenuItem(
                text = { Text("Share", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.White) },
                onClick = {
                    showMenu = false
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "image/*"
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(intent, "Share Image"))
                }
            )
            DropdownMenuItem(
                text = { Text("Remove", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White) },
                onClick = {
                    showMenu = false
                    onRemove()
                }
            )
        }
    }
}

fun getRecentCameraImages(context: Context, excludedUris: Set<String>): List<Uri> {
    val images = mutableListOf<Uri>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATA
    )
    
    val selection = "${MediaStore.Images.Media.MIME_TYPE} LIKE ? AND ${MediaStore.Images.Media.DATA} LIKE ?"
    val selectionArgs = arrayOf("image/%", "%/DCIM/Camera/%")
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext() && images.size < 6) {
            val id = cursor.getLong(idColumn)
            val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
            if (!excludedUris.contains(contentUri.toString())) {
                images.add(contentUri)
            }
        }
    }
    
    // Fallback if no Camera images found
    if (images.isEmpty()) {
        val fallbackSelection = "${MediaStore.Images.Media.MIME_TYPE} LIKE ?"
        val fallbackArgs = arrayOf("image/%")
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            fallbackSelection,
            fallbackArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext() && images.size < 6) {
                val id = cursor.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                if (!excludedUris.contains(contentUri.toString())) {
                    images.add(contentUri)
                }
            }
        }
    }
    
    return images
}

@Composable
fun Section(
    title: String,
    items: List<String>,
    onItemClick: (Int) -> Unit,
    onPermissionClick: (() -> Unit)? = null,
    onClearClick: (() -> Unit)? = null,
    maxItems: Int = 3
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
            if (onClearClick != null && items.isNotEmpty()) {
                IconButton(onClick = onClearClick, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF4A90E2).copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (onPermissionClick != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPermissionClick() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Enable permissions to see $title".uppercase(),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (items.isEmpty()) {
            Text(
                "No new $title".uppercase(),
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            items.take(maxItems).forEachIndexed { index, item ->
                Text(
                    text = item,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(index) }
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun UsageGraph(weeklyUsage: List<Long>) {
    // Increased to 12-hour scale
    val maxUsageMillis = 12L * 60 * 60 * 1000f
    val days = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    
    // Reduced height from 120.dp to 100.dp
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(100.dp)
    ) {
        val hourLabels = listOf(3, 6, 9, 12)
        val todayIndex = remember {
            val cal = Calendar.getInstance()
            (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
        }

        // Grid Lines, Hours, and Bars drawn on Canvas for perfect alignment
        Canvas(modifier = Modifier.fillMaxSize()) {
            val graphBottom = size.height - 30.dp.toPx()
            val graphTop = 10.dp.toPx()
            val graphHeight = graphBottom - graphTop

            // Draw Grid Lines
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                alpha = (255 * 0.3f).toInt()
                textSize = 8.sp.toPx()
            }

            hourLabels.forEach { hour ->
                val hMillis = hour.toLong() * 60 * 60 * 1000f
                val y = graphBottom - (hMillis / maxUsageMillis * graphHeight)
                
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.5.dp.toPx()
                )
                
                drawContext.canvas.nativeCanvas.drawText("${hour}h", 0f, y - 4.dp.toPx(), textPaint)
            }

            // Draw Bars
            val barWidth = 16.dp.toPx()
            val spacing = size.width / days.size
            
            days.forEachIndexed { index, _ ->
                val usage = weeklyUsage.getOrNull(index) ?: 0L
                if (usage > 0) {
                    val barHeight = (usage.toFloat() / maxUsageMillis * graphHeight).coerceAtMost(graphHeight).coerceAtLeast(1.dp.toPx())
                    val isToday = index == todayIndex
                    
                    val x = index * spacing + (spacing / 2) - (barWidth / 2)
                    
                    // Main Bar
                    drawRoundRect(
                        color = if (isToday) Color(0xFF4A90E2).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.3f),
                        topLeft = Offset(x, graphBottom - barHeight),
                        size = ComposeSize(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    
                    // Solid bottom for the current day, or consistent baseline for others
                    val squareHeight = 2.dp.toPx().coerceAtMost(barHeight)
                    drawRect(
                        color = if (isToday) Color(0xFF4A90E2) else Color.White.copy(alpha = 0.3f),
                        topLeft = Offset(x, graphBottom - squareHeight),
                        size = ComposeSize(barWidth, squareHeight)
                    )
                }
            }
        }

        // X-axis labels and baseline
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(29.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                days.forEachIndexed { index, day ->
                    val isToday = index == todayIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(4.dp)
                                .background(if (isToday) Color(0xFF4A90E2) else Color.White.copy(alpha = 0.2f))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = day,
                            color = if (isToday) Color(0xFF4A90E2) else Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
