package com.soc.launcher.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.text.format.DateFormat
import android.view.inputmethod.InputMethodManager
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.soc.launcher.*
import com.soc.launcher.data.model.AppInfo
import com.soc.launcher.data.model.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PresentScreen(
    aiAppPackage: String,
    apps: List<AppInfo>,
    weatherApiKey: String,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val use24HourFormat = DateFormat.is24HourFormat(context)

    var hasUsagePermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val mostUsedApps by produceState<List<AppInfo>>(initialValue = emptyList(), apps, hasUsagePermission) {
        if (apps.isNotEmpty()) {
            value = withContext(Dispatchers.IO) { getMostUsedApps(context, apps) }
        }
    }

    val totalUsageTime by produceState(initialValue = "0m", hasUsagePermission) {
        while (true) {
            if (hasUsagePermission) {
                val totalMs = withContext(Dispatchers.IO) { getScreenOnTime(context) }
                val hours = totalMs / (1000 * 60 * 60)
                val minutes = (totalMs / (1000 * 60)) % 60
                value = when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    else -> "${minutes}m"
                }
            }
            delay(60000) // Update every minute
        }
    }

    var currentDate by remember { mutableStateOf(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())) }

    val allAlarms by produceState<List<String>>(initialValue = emptyList(), use24HourFormat) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        while (true) {
            val next = alarmManager.nextAlarmClock
            val alarms = mutableListOf<String>()
            next?.let {
                val alarmTime = it.triggerTime
                val now = Calendar.getInstance()
                val alarmCal = Calendar.getInstance().apply { timeInMillis = alarmTime }

                // Only show if it's today
                val isToday = now.get(Calendar.YEAR) == alarmCal.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) == alarmCal.get(Calendar.DAY_OF_YEAR)

                if (isToday) {
                    val pattern = if (use24HourFormat) "HH:mm" else "h:mm a"
                    alarms.add(SimpleDateFormat(pattern, Locale.getDefault()).format(Date(alarmTime)))
                }
            }
            value = alarms
            delay(10000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    val storageUsage by produceState(initialValue = 0) {
        while (true) {
            value = withContext(Dispatchers.IO) { getStorageInfo() }
            delay(10000)
        }
    }

    val memoryUsage by produceState(initialValue = 0) {
        while (true) {
            value = withContext(Dispatchers.IO) { getMemoryInfo(context) }
            delay(5000)
        }
    }

    val batteryTemp by produceState(initialValue = 0f) {
        while (true) {
            value = getBatteryTemperature(context)
            delay(10000)
        }
    }

    val calendarEvents by produceState<List<String>>(initialValue = emptyList(), use24HourFormat) {
        while (true) {
            if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                val events = mutableListOf<String>()
                val now = Calendar.getInstance()
                val startOfDay = now.clone() as Calendar
                startOfDay.set(Calendar.HOUR_OF_DAY, 0)
                startOfDay.set(Calendar.MINUTE, 0)
                startOfDay.set(Calendar.SECOND, 0)
                
                val endOfDay = now.clone() as Calendar
                endOfDay.set(Calendar.HOUR_OF_DAY, 23)
                endOfDay.set(Calendar.MINUTE, 59)
                endOfDay.set(Calendar.SECOND, 59)

                // Use Instances to correctly handle recurring events
                val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                android.content.ContentUris.appendId(builder, startOfDay.timeInMillis)
                android.content.ContentUris.appendId(builder, endOfDay.timeInMillis)

                val projection = arrayOf(
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.ALL_DAY
                )
                
                context.contentResolver.query(
                    builder.build(),
                    projection,
                    null,
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC"
                )?.use { cursor ->
                    val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                    val startIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                    val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)

                    while (cursor.moveToNext()) {
                        val title = cursor.getString(titleIdx)
                        val startTime = cursor.getLong(startIdx)
                        val allDay = cursor.getInt(allDayIdx) == 1

                        val pattern = if (use24HourFormat) "HH:mm" else "h:mm a"
                        val timeStr = if (allDay) "All Day" else SimpleDateFormat(pattern, Locale.getDefault()).format(Date(startTime))
                        events.add("$timeStr - $title")
                    }
                }
                value = events.distinct() // Remove duplicates if any
            }
            delay(300000) // Update every 5 minutes
        }
    }

    var weatherInfo by remember { mutableStateOf<WeatherResponse?>(null) }

    LaunchedEffect(weatherApiKey) {
        if (weatherApiKey.isBlank()) {
            weatherInfo = null
            return@LaunchedEffect
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        while (true) {
            try {
                var lat = 40.7128
                var lon = -74.0060

                if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER) 
                        ?: locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    
                    if (location != null) {
                        lat = location.latitude
                        lon = location.longitude
                    }
                }

                val response = WeatherService.getCurrentWeather(lat, lon, "metric", weatherApiKey)
                weatherInfo = response
            } catch (e: Exception) {
                Log.e("TemporalLauncher", "Weather fetch failed", e)
            }
            delay(1800000) // Update every 30 mins
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 140.dp),
            horizontalAlignment = Alignment.Start
        ) {
            var searchQuery by remember { mutableStateOf("") }
            PresentSearchBar(
                aiPkg = aiAppPackage,
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSettingsClick = onSettingsClick
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                weatherInfo?.let { weather ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Replaced Coil AsyncImage with a simple Text or placeholder since we removed the library
                        Text(
                            text = "☁", // Placeholder for weather icon
                            fontSize = 24.sp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${weather.main.temp.toInt()}°C - ${weather.weather[0].description}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 1f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 6f
                                )
                            )
                        )
                    }
                }

                // Stats Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    StatItem("RAM: $memoryUsage%", Color.White, 16.sp) {
                        val intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
                        try { context.startActivity(intent) } catch (e: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    }
                    StatItem("STORAGE: $storageUsage%", Color.White, 16.sp) {
                        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                        try { context.startActivity(intent) } catch (e: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    }
                    StatItem("TEMP: ${batteryTemp}°C", Color.White, 16.sp) {
                        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                        try { context.startActivity(intent) } catch (e: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    }
                }

                // Time & Usage Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (hasUsagePermission) {
                        Text(
                            text = "Screen Time: $totalUsageTime".uppercase(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 1f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 6f
                                )
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.clickable {
                            val uri = Uri.parse("content://com.android.calendar/time/")
                            val intent = Intent(Intent.ACTION_VIEW).setData(uri)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val calendarPackages = listOf(
                                    "com.google.android.calendar",
                                    "com.android.calendar",
                                    "com.samsung.android.calendar"
                                )
                                for (pkg in calendarPackages) {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                        return@clickable
                                    }
                                }
                            }
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier
                                    .matchParentSize()
                                    .blur(2.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = currentDate.uppercase(),
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 1f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 6f
                                )
                            )
                        )
                    }

                    if (calendarEvents.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(start = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            calendarEvents.forEach { event ->
                                Text(
                                    text = event,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 1f),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 6f
                                        )
                                    )
                                )
                            }
                        }
                    }

                    if (allAlarms.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(start = 32.dp, top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            allAlarms.forEach { alarm ->
                                Text(
                                    text = alarm,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 1f),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 6f
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

                MediaControlSection()
            }
        }

        val googleAppsPackages = listOf(
            "com.google.android.dialer",     // Phone
            "com.google.android.apps.messaging", // Messages
            "com.google.android.apps.maps",    // Maps
            "com.android.vending",           // Play Store
            "com.google.android.apps.chromecast.app", // Home
            "com.google.android.GoogleCamera" // Camera
        )

        val bottomRowApps = (googleAppsPackages.mapNotNull { pkg ->
            apps.find { it.packageName == pkg } ?: apps.find { it.packageName.contains("camera", ignoreCase = true) && pkg == "com.google.android.GoogleCamera" }
        } + apps.filter { it.packageName !in googleAppsPackages }).distinctBy { it.packageName }.take(6)

        val topRowApps = (mostUsedApps.filter { it !in bottomRowApps } +
                apps.filter { it !in bottomRowApps })
            .distinctBy { it.packageName }
            .take(6)

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.40f))
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    // Top Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        topRowApps.forEach { app ->
                            AppIcon(app, Modifier.size(43.dp), showLabel = false)
                        }
                    }

                    // Bottom Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomRowApps.forEach { app ->
                            AppIcon(app, Modifier.size(43.dp), showLabel = false)
                        }
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentSearchBar(
    aiPkg: String,
    query: String = "",
    onQueryChange: ((String) -> Unit)? = null,
    placeholder: String = "Search...",
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.40f))
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.clickable { onSettingsClick() }
        )
        TextField(
            value = query,
            onValueChange = { onQueryChange?.invoke(it) },
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.6f)) },
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.White,
                focusedTextColor = Color.White
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                    context.startActivity(intent)
                    onQueryChange?.invoke("")
                    // Hide keyboard
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(null, 0)
                }
            })
        )
        IconButton(onClick = {
            if (aiPkg.isNotEmpty()) {
                val intent = context.packageManager.getLaunchIntentForPackage(aiPkg)
                if (intent != null) {
                    try {
                        context.startActivity(intent)
                        return@IconButton
                    } catch (e: Exception) {}
                }
            }

            // Fallback to Google Assistant specific package if known, otherwise general intent
            val googleAssistantPkg = "com.google.android.apps.googleassistant"
            val gaIntent = context.packageManager.getLaunchIntentForPackage(googleAssistantPkg)
            if (gaIntent != null) {
                try {
                    context.startActivity(gaIntent)
                    return@IconButton
                } catch (e: Exception) {}
            }

            // Fallback to system assistant if specific packages fail
            val assistantIntent = Intent(Intent.ACTION_ASSIST)
            try {
                context.startActivity(assistantIntent)
            } catch (e: Exception) {
                // Last ditch: Open Google App search
                try {
                    val searchIntent = Intent(Intent.ACTION_MAIN)
                    searchIntent.setPackage("com.google.android.googlequicksearchbox")
                    context.startActivity(searchIntent)
                } catch (e2: Exception) {
                    Log.e("ChronosLauncher", "Failed to launch any assistant", e)
                }
            }
        }) {
            Icon(Icons.Default.Star, contentDescription = "AI", tint = Color(0xFF4A90E2))
        }
    }
}
