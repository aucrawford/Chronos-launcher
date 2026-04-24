package com.soc.launcher.ui

import android.app.AlarmManager
import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.shape.RoundedCornerShape
import android.provider.CalendarContract
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.soc.launcher.*
import com.soc.launcher.data.model.AppInfo
import com.soc.launcher.data.model.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PresentScreen(
    searchAppPackage: String,
    aiAppPackage: String,
    apps: List<AppInfo>,
    weatherApiKey: String,
    hasImportantNote: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    var currentDate by remember { mutableStateOf(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())) }

    val nextAlarm by produceState<String?>(initialValue = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        while (true) {
            val alarm = alarmManager.nextAlarmClock
            value = alarm?.let {
                val alarmTime = it.triggerTime
                val now = Calendar.getInstance()
                val alarmCal = Calendar.getInstance().apply { timeInMillis = alarmTime }

                // Only show if it's today
                val isToday = now.get(Calendar.YEAR) == alarmCal.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) == alarmCal.get(Calendar.DAY_OF_YEAR)

                if (isToday) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(alarmTime))
                } else {
                    null
                }
            }
            delay(10000) // Update every 10 seconds to keep it fresh
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

    val calendarEvents by produceState<List<String>>(initialValue = emptyList()) {
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

                val projection = arrayOf(
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.ALL_DAY
                )
                
                val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?) AND (${CalendarContract.Events.DELETED} = 0)"
                val selectionArgs = arrayOf(startOfDay.timeInMillis.toString(), endOfDay.timeInMillis.toString())
                
                context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${CalendarContract.Events.DTSTART} ASC"
                )?.use { cursor ->
                    val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                    val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                    val allDayIdx = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)
                    
                    while (cursor.moveToNext()) {
                        val title = cursor.getString(titleIdx)
                        val startTime = cursor.getLong(startIdx)
                        val allDay = cursor.getInt(allDayIdx) == 1
                        
                        val timeStr = if (allDay) "All Day" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(startTime))
                        events.add("$timeStr - $title")
                    }
                }
                value = events
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

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        while (true) {
            try {
                var lat = 40.7128
                var lon = -74.0060

                if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        val locationTask = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                        val location = withContext(Dispatchers.IO) {
                            com.google.android.gms.tasks.Tasks.await(locationTask)
                        }
                        if (location != null) {
                            lat = location.latitude
                            lon = location.longitude
                        }
                    } catch (e: Exception) {
                        Log.e("TemporalLauncher", "Location fetch failed", e)
                    }
                }

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.openweathermap.org/data/2.5/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val api = retrofit.create(WeatherApi::class.java)

                val response = api.getCurrentWeather(lat, lon, "metric", weatherApiKey)
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
                .padding(start = 16.dp, end = 16.dp, top = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            SearchBar(searchAppPackage, aiAppPackage)

            Spacer(Modifier.height(32.dp))

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
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier
                            .matchParentSize()
                            .blur(2.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
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
                    modifier = Modifier.padding(start = 32.dp, top = 4.dp),
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

            Spacer(Modifier.height(16.dp))

            weatherInfo?.let { weather ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    AsyncImage(
                        model = "https://openweathermap.org/img/wn/${weather.weather[0].icon}@2x.png",
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
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

            nextAlarm?.let { alarm ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val clockPackages = listOf(
                                    "com.google.android.deskclock",
                                    "com.android.deskclock",
                                    "com.sec.android.app.clockpackage",
                                    "com.huawei.deskclock",
                                    "com.coloros.alarmclock"
                                )
                                for (pkg in clockPackages) {
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
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier
                                .matchParentSize()
                                .blur(2.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = alarm,
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

            Spacer(Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                StatItem("RAM: $memoryUsage%", Color.White, 16.sp) {
                    val intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
                    try { context.startActivity(intent) } catch (e: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                }
                StatItem("SSD: $storageUsage%", Color.White, 16.sp) {
                    val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                    try { context.startActivity(intent) } catch (e: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                }
                StatItem("TEMP: ${batteryTemp}°C", Color.White, 16.sp) {
                    val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                    try { context.startActivity(intent) } catch (e: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                }
            }

            Spacer(Modifier.height(24.dp))

            MediaControlSection()
        }

        val googleAppsPackages = listOf(
            "com.google.android.dialer",     // Phone
            "com.google.android.apps.messaging", // Messages
            "com.google.android.apps.maps",    // Maps
            "com.android.vending",           // Play Store
            "com.google.android.apps.chromecast.app", // Home
            "com.google.android.GoogleCamera" // Camera
        )

        val bottomRowApps = googleAppsPackages.map { pkg ->
            apps.find { it.packageName == pkg } ?: apps.find { it.packageName.contains("camera", ignoreCase = true) && pkg == "com.google.android.GoogleCamera" }
        }.filterNotNull()

        val topRowApps = mostUsedApps
            .filter { app -> app.packageName !in googleAppsPackages }
            .take(6)

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            if (hasImportantNote) {
                Text(
                    text = "! IMPORTANT MESSAGE FROM YOURSELF !",
                    color = Color.Yellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 1f),
                            offset = Offset(0f, 0f),
                            blurRadius = 6f
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.30f))
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Row: Most used
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        topRowApps.forEach { app ->
                            AppIcon(app, Modifier.size(56.dp), showLabel = false)
                        }
                    }

                    // Bottom Row: Fixed Google apps
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomRowApps.forEach { app ->
                            AppIcon(app, Modifier.size(56.dp), showLabel = false)
                        }
                    }
                }
            }
        }
    }
}
