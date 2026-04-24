package com.soc.launcher

import com.soc.launcher.data.model.*
import com.soc.launcher.util.AppCategoryHelper
import com.soc.launcher.util.NewsParser
import com.soc.launcher.util.SpamDetector
import com.soc.launcher.ui.*
import android.Manifest
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.WallpaperManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.StatFs
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.*
import androidx.core.app.NotificationManagerCompat
import android.content.ComponentName
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.ui.text.TextStyle
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

interface WeatherApi {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String,
        @Query("appid") apiKey: String
    ): WeatherResponse
}

interface NewsApi {
    @GET("api/v1/cors/news-feed")
    suspend fun getTopHeadlines(): OkSurfResponse
}

interface NewsOrgApi {
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String,
        @Query("apiKey") apiKey: String
    ): NewsOrgResponse

    @GET("everything")
    suspend fun getEverything(
        @Query("q") query: String,
        @Query("sortBy") sortBy: String,
        @Query("apiKey") apiKey: String
    ): NewsOrgResponse
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { _ -> }

            LaunchedEffect(Unit) {
                val permissions = mutableListOf(
                    Manifest.permission.READ_CALL_LOG,
                    "android.permission.WRITE_CALL_LOG",
                    Manifest.permission.READ_SMS,
                    "android.permission.WRITE_SMS",
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                launcher.launch(permissions.toTypedArray())
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    TemporalLauncher()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemporalLauncher() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPrefs = remember { context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }

    val wallpaperManager = remember { WallpaperManager.getInstance(context) }

    val wallpaperPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    var hasWallpaperPermission by remember {
        mutableStateOf(context.checkSelfPermission(wallpaperPermission) == PackageManager.PERMISSION_GRANTED)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasWallpaperPermission = context.checkSelfPermission(wallpaperPermission) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val defaultWallpaper by produceState<Bitmap?>(initialValue = null, hasWallpaperPermission) {
        if (hasWallpaperPermission) {
            withContext(Dispatchers.IO) {
                try {
                    value = wallpaperManager.drawable?.toBitmap()
                } catch (e: SecurityException) {
                    Log.e("TemporalLauncher", "SecurityException reading wallpaper", e)
                } catch (e: Exception) {
                    Log.e("TemporalLauncher", "Error reading wallpaper", e)
                }
            }
        }
    }
    
    val apps by produceState<List<AppInfo>>(initialValue = emptyList()) {
        withContext(Dispatchers.IO) {
            val result = getInstalledApps(context)
            Log.d("TemporalLauncher", "Loaded ${result.size} apps")
            value = result
        }
    }

    var backgroundImageUri by remember { mutableStateOf(sharedPrefs.getString("bg_uri", null)) }
    var searchAppPackage by remember { mutableStateOf(sharedPrefs.getString("search_pkg", "com.google.android.googlequicksearchbox") ?: "com.google.android.googlequicksearchbox") }
    var aiAppPackage by remember { mutableStateOf(sharedPrefs.getString("ai_pkg", "com.google.android.apps.bard") ?: "com.google.android.apps.bard") }
    var newsAppPackage by remember { mutableStateOf(sharedPrefs.getString("news_pkg", "com.google.android.apps.magazines") ?: "com.google.android.apps.magazines") }
    var weatherApiKey by remember { mutableStateOf(sharedPrefs.getString("weather_api_key", "") ?: "") }
    var newsApiKey by remember { mutableStateOf(sharedPrefs.getString("news_api_key", "") ?: "") }

    var reminders by remember {
        val json = sharedPrefs.getString("reminders", null)
        mutableStateOf<List<Reminder>>(
            try {
                if (json != null) {
                    val type = object : TypeToken<List<Reminder>>() {}.type
                    gson.fromJson<List<Reminder>>(json, type)
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    val hasImportantNote = reminders.any { it.isImportant }

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (backgroundImageUri != null) {
            AsyncImage(
                model = backgroundImageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (defaultWallpaper != null) {
            Image(
                bitmap = defaultWallpaper!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Default background if no wallpaper is found
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showSettings = true }
                    )
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> PastScreen(
                        newsAppPackage = newsAppPackage,
                        newsApiKey = newsApiKey,
                        reminders = reminders,
                        onRemindersChanged = { updatedReminders ->
                            reminders = updatedReminders
                            sharedPrefs.edit().putString("reminders", gson.toJson(updatedReminders)).apply()
                        }
                    )
                    1 -> PresentScreen(searchAppPackage, aiAppPackage, apps, weatherApiKey, hasImportantNote)
                    2 -> FutureScreen(apps)
                }
            }
        }

        if (showSettings) {
            SettingsDialog(
                onDismiss = { showSettings = false },
                onBgSelected = { uri ->
                    backgroundImageUri = uri
                    sharedPrefs.edit().putString("bg_uri", uri).apply()
                },
                onAppSelected = { type, pkg ->
                    when (type) {
                        "search" -> { searchAppPackage = pkg; sharedPrefs.edit().putString("search_pkg", pkg).apply() }
                        "ai" -> { aiAppPackage = pkg; sharedPrefs.edit().putString("ai_pkg", pkg).apply() }
                        "news" -> { newsAppPackage = pkg; sharedPrefs.edit().putString("news_pkg", pkg).apply() }
                    }
                },
                newsApiKey = newsApiKey,
                onNewsApiKeyChanged = { key ->
                    newsApiKey = key
                    sharedPrefs.edit().putString("news_api_key", key).apply()
                },
                weatherApiKey = weatherApiKey,
                onApiKeyChanged = { key ->
                    weatherApiKey = key
                    sharedPrefs.edit().putString("weather_api_key", key).apply()
                }
            )
        }
    }
}

fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val appList = mutableListOf<AppInfo>()
    
    try {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager.MATCH_ALL
        } else {
            0
        }
        val resolved = pm.queryIntentActivities(intent, flags)
        Log.d("TemporalLauncher", "queryIntentActivities found ${resolved.size} apps")

        if (resolved.isEmpty()) {
            Log.w("TemporalLauncher", "No apps found with queryIntentActivities")
        }

        resolved.forEach { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()
            val appInfo = resolveInfo.activityInfo.applicationInfo
            
            val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (appInfo.category) {
                    android.content.pm.ApplicationInfo.CATEGORY_GAME -> "Games"
                    android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
                    android.content.pm.ApplicationInfo.CATEGORY_VIDEO,
                    android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "Media"
                    android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                    android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "News"
                    android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Work"
                    else -> AppCategoryHelper.determineCategory(pkg)
                }
            } else {
                AppCategoryHelper.determineCategory(pkg)
            }
            appList.add(AppInfo(label, pkg, category))
        }
    } catch (e: Exception) {
        Log.e("TemporalLauncher", "Error in getInstalledApps", e)
    }

    if (appList.isEmpty()) {
        Log.d("TemporalLauncher", "Falling back to getInstalledApplications")
        try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA).forEach { appInfo ->
                if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                    appList.add(AppInfo(appInfo.loadLabel(pm).toString(), appInfo.packageName, "Other"))
                }
            }
        } catch (e: Exception) {
            Log.e("TemporalLauncher", "Fallback failed", e)
        }
    }
    
    return appList.distinctBy { it.packageName }.sortedBy { it.name }
}

fun getStorageInfo(): Int {
    val path = Environment.getDataDirectory()
    val stat = StatFs(path.path)
    val blockSize = stat.blockSizeLong
    val totalBlocks = stat.blockCountLong
    val availableBlocks = stat.availableBlocksLong
    val totalSpace = totalBlocks * blockSize
    val availableSpace = availableBlocks * blockSize
    return (100 - (availableSpace.toDouble() / totalSpace.toDouble() * 100)).toInt()
}

fun getMemoryInfo(context: Context): Int {
    val mi = ActivityManager.MemoryInfo()
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    activityManager.getMemoryInfo(mi)
    val percentUsed = 100 - (mi.availMem.toDouble() / mi.totalMem.toDouble() * 100).toInt()
    return percentUsed
}

fun getBatteryTemperature(context: Context): Float {
    val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val temp = intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
    return temp / 10f
}

fun getMissedCalls(context: Context): List<MissedCallInfo> {
    val calls = mutableListOf<MissedCallInfo>()
    if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
    }
    try {
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME),
            "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE} AND ${CallLog.Calls.IS_READ} = 0",
            null,
            "${CallLog.Calls.DATE} DESC"
        )
        cursor?.use {
            val idIndex = it.getColumnIndex(CallLog.Calls._ID)
            val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            
            val counts = mutableMapOf<String, Int>()
            val latestCalls = mutableMapOf<String, Triple<String, String, String>>() // number -> (id, name, number)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = if (nameIndex != -1) it.getString(nameIndex) else null
                val number = if (numberIndex != -1) it.getString(numberIndex) else "Unknown"
                val displayName = name ?: number
                
                counts[displayName] = (counts[displayName] ?: 0) + 1
                if (!latestCalls.containsKey(displayName)) {
                    latestCalls[displayName] = Triple(id, displayName, number)
                }
            }
            
            latestCalls.forEach { (displayName, info) ->
                calls.add(MissedCallInfo(info.first, info.second, info.third, counts[displayName] ?: 1))
            }
        }
    } catch (e: Exception) {
        Log.e("TemporalLauncher", "Error querying missed calls", e)
    }
    return calls
}

fun getRecentMessages(context: Context): List<MessageInfo> {
    val messages = mutableListOf<MessageInfo>()
    if (context.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
    }
    try {
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.READ, Telephony.Sms.TYPE),
            "read = 0 AND type = ${Telephony.Sms.MESSAGE_TYPE_INBOX}",
            null,
            "date DESC"
        )
        cursor?.use {
            val idIndex = it.getColumnIndex(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val readIndex = it.getColumnIndex(Telephony.Sms.READ)
            val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)
            
            while (it.moveToNext()) {
                val type = if (typeIndex != -1) it.getInt(typeIndex) else -1
                if (type != Telephony.Sms.MESSAGE_TYPE_INBOX) continue

                val isRead = if (readIndex != -1) it.getInt(readIndex) == 1 else false
                if (isRead) continue

                val id = it.getString(idIndex)
                val address = if (addressIndex != -1) it.getString(addressIndex) else ""
                val fullBody = if (bodyIndex != -1) (it.getString(bodyIndex) ?: "") else ""
                
                var senderName = if (address.isBlank()) "Unknown" else address
                var photoUri: String? = null
                
                val isSpam = SpamDetector.isSpam(address, fullBody)

                // Lookup contact info - if found, it's generally not spam
                var finalIsSpam = isSpam
                if (address.isNotBlank() && context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
                    context.contentResolver.query(
                        uri,
                        arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI),
                        null, null, null
                    )?.use { c ->
                        if (c.moveToFirst()) {
                            senderName = c.getString(c.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)) ?: address
                            photoUri = c.getString(c.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI))
                            finalIsSpam = false 
                        }
                    }
                }
                
                if (!finalIsSpam && !isRead) {
                    messages.add(MessageInfo(id, fullBody.take(40), address, senderName, photoUri, finalIsSpam))
                }
            }
        }
    } catch (e: Exception) {
        Log.e("ChronosLauncher", "Error querying messages", e)
    }
    return messages
}

fun getMostUsedApps(context: Context, allApps: List<AppInfo>): List<AppInfo> {
    if (!hasUsageStatsPermission(context)) {
        return allApps.take(10)
    }
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val endTime = System.currentTimeMillis()
    val startTime = endTime - 1000 * 60 * 60 * 24 * 7
    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, startTime, endTime)

    val sortedStats = stats.sortedByDescending { it.totalTimeInForeground }
    
    // Filter out apps that are home launchers
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val launcherPackages = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        .map { it.activityInfo.packageName }
        .toSet()

    val mostUsedPackages = sortedStats
        .map { it.packageName }
        .filter { it !in launcherPackages }
        .distinct()
        .take(10)
    
    val result = mutableListOf<AppInfo>()
    mostUsedPackages.forEach { pkg ->
        allApps.find { it.packageName == pkg }?.let { result.add(it) }
    }
    
    if (result.size < 10) {
        val remaining = allApps
            .filter { it !in result && it.packageName !in launcherPackages }
            .take(10 - result.size)
        result.addAll(remaining)
    }
    
    return result
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

fun getFavoriteContacts(context: Context): List<ContactInfo> {
    val contacts = mutableListOf<ContactInfo>()
    if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
    }
    try {
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
            ),
            "${ContactsContract.Contacts.STARRED} = 1",
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )
        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val photoIndex = it.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = it.getString(nameIndex)
                val photoUri = it.getString(photoIndex)
                contacts.add(ContactInfo(id, name, photoUri))
            }
        }
    } catch (e: Exception) {
        Log.e("TemporalLauncher", "Error querying contacts", e)
    }
    return contacts
}

fun searchContacts(context: Context, query: String): List<ContactInfo> {
    val contacts = mutableMapOf<String, ContactInfo>()
    if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
    }
    
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    
    // Search in display name OR phone number, restricted to favorites (STARRED)
    val selection = "(${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?) AND ${ContactsContract.CommonDataKinds.Phone.STARRED} = 1"
    val selectionArgs = arrayOf("%$query%", "%$query%")
    val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

    try {
        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
            
            while (cursor.moveToNext() && contacts.size < 5) {
                val id = cursor.getString(idIndex)
                if (!contacts.containsKey(id)) {
                    val name = cursor.getString(nameIndex)
                    val photoUri = cursor.getString(photoIndex)
                    contacts[id] = ContactInfo(id, name, photoUri)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("ChronosLauncher", "Error searching contacts", e)
    }
    return contacts.values.toList()
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
