package com.soc.launcher.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.soc.launcher.*
import com.soc.launcher.data.model.Reminder
import com.soc.launcher.data.model.MissedCallInfo
import com.soc.launcher.data.model.NewsArticle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.ui.text.TextStyle
import java.util.UUID
import com.soc.launcher.util.NewsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PastScreen(
    newsAppPackage: String,
    newsApiKey: String,
    reminders: List<Reminder>,
    onRemindersChanged: (List<Reminder>) -> Unit
) {
    val context = LocalContext.current

    val callPermission = Manifest.permission.READ_CALL_LOG
    val smsPermission = Manifest.permission.READ_SMS

    var hasCallPermission by remember { mutableStateOf(context.checkSelfPermission(callPermission) == PackageManager.PERMISSION_GRANTED) }
    var hasSmsPermission by remember { mutableStateOf(context.checkSelfPermission(smsPermission) == PackageManager.PERMISSION_GRANTED) }
    var hasWriteCallPermission by remember { mutableStateOf(context.checkSelfPermission("android.permission.WRITE_CALL_LOG") == PackageManager.PERMISSION_GRANTED) }
    var hasWriteSmsPermission by remember { mutableStateOf(context.checkSelfPermission("android.permission.WRITE_SMS") == PackageManager.PERMISSION_GRANTED) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCallPermission = permissions[callPermission] ?: hasCallPermission
        hasSmsPermission = permissions[smsPermission] ?: hasSmsPermission
        hasWriteCallPermission = permissions["android.permission.WRITE_CALL_LOG"] ?: hasWriteCallPermission
        hasWriteSmsPermission = permissions["android.permission.WRITE_SMS"] ?: hasWriteSmsPermission
    }

    var missedCalls by remember { mutableStateOf<List<MissedCallInfo>>(emptyList()) }

    val sharedPrefs = remember { context.getSharedPreferences("launcher_prefs", android.content.Context.MODE_PRIVATE) }

    var parkingLat by remember { mutableStateOf(sharedPrefs.getFloat("parking_lat", 0f)) }
    var parkingLng by remember { mutableStateOf(sharedPrefs.getFloat("parking_lng", 0f)) }
    var parkingTime by remember { mutableStateOf(sharedPrefs.getLong("parking_time", 0L)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, hasCallPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasCallPermission) missedCalls = getMissedCalls(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (hasCallPermission) missedCalls = getMissedCalls(context)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val coroutineScope = rememberCoroutineScope()

    val newsArticles by produceState<List<NewsArticle>>(initialValue = emptyList(), newsApiKey) {
        value = withContext(Dispatchers.IO) {
            try {
                val calendar = Calendar.getInstance()
                val dateStr = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

                // Get city name from coordinates
                var cityName = "US"
                try {
                    val geocoder = android.location.Geocoder(context, Locale.getDefault())
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val locationTask = fusedLocationClient.lastLocation
                        val location = withContext(Dispatchers.IO) {
                            try {
                                com.google.android.gms.tasks.Tasks.await(locationTask)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (location != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val geocodeListener = object : android.location.Geocoder.GeocodeListener {
                                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                        if (addresses.isNotEmpty()) {
                                            cityName = addresses[0].locality ?: addresses[0].adminArea ?: "US"
                                        }
                                    }
                                    override fun onError(errorMessage: String?) {
                                        Log.e("ChronosLauncher", "Geocoder error: $errorMessage")
                                    }
                                }
                                geocoder.getFromLocation(location.latitude, location.longitude, 1, geocodeListener)
                                delay(800)
                            } else {
                                @Suppress("DEPRECATION")
                                val addresses = withContext(Dispatchers.IO) {
                                    try {
                                        geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (!addresses.isNullOrEmpty()) {
                                    cityName = addresses[0].locality ?: addresses[0].adminArea ?: "US"
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChronosLauncher", "Geocoder failed", e)
                }

                val searchQuery = "$cityName news $dateStr"
                Log.d("ChronosLauncher", "Searching news for: $searchQuery")

                if (newsApiKey.isNotBlank()) {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://newsapi.org/v2/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val api = retrofit.create(NewsOrgApi::class.java)
                    val response = api.getEverything(searchQuery, "publishedAt", newsApiKey)
                    response.articles.take(10).map { it.copy(source = it.source ?: it.title.substringAfterLast(" - ").trim()) }
                } else {
                    val encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                    val rssUrl = "https://news.google.com/rss/search?q=$encodedQuery&hl=en-US&gl=US&ceid=US:en"
                    withContext(Dispatchers.IO) {
                        NewsParser.parseRss(URL(rssUrl).openStream())
                    }
                }
            } catch (e: Exception) {
                Log.e("ChronosLauncher", "News fetch failed", e)
                emptyList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A10).copy(alpha = 0.85f))
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 32.dp, bottom = 16.dp)
        ) {
            if (missedCalls.isNotEmpty()) {
                Section(
                    title = "Missed Calls",
                    items = missedCalls.map { if (it.count > 1) "${it.name} (${it.count})" else it.name },
                    onItemClick = { index ->
                        if (missedCalls.isNotEmpty()) {
                            val call = missedCalls[index]
                            val intent = Intent(Intent.ACTION_VIEW, CallLog.Calls.CONTENT_URI)
                            context.startActivity(intent)

                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val values = android.content.ContentValues()
                                    values.put(CallLog.Calls.IS_READ, 1)
                                    values.put(CallLog.Calls.NEW, 0)
                                    context.contentResolver.update(
                                        CallLog.Calls.CONTENT_URI,
                                        values,
                                        "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.IS_READ} = 0",
                                        arrayOf(call.number)
                                    )
                                    val updated = getMissedCalls(context)
                                    withContext(Dispatchers.Main) { missedCalls = updated }
                                } catch (e: Exception) {
                                    Log.e("ChronosLauncher", "Error marking call as read", e)
                                }
                            }
                        }
                    },
                    onPermissionClick = if (!hasCallPermission || !hasWriteCallPermission) { { launcher.launch(arrayOf(callPermission, "android.permission.WRITE_CALL_LOG")) } } else null,
                    onClearClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val values = android.content.ContentValues()
                                values.put(CallLog.Calls.IS_READ, 1)
                                values.put(CallLog.Calls.NEW, 0)
                                context.contentResolver.update(
                                    CallLog.Calls.CONTENT_URI,
                                    values,
                                    "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE} AND ${CallLog.Calls.IS_READ} = 0",
                                    null
                                )
                                val updated = getMissedCalls(context)
                                withContext(Dispatchers.Main) { missedCalls = updated }
                            } catch (e: Exception) {
                                Log.e("ChronosLauncher", "Error clearing missed calls", e)
                            }
                        }
                    },
                    maxItems = 3
                )
                Spacer(Modifier.height(24.dp))
            }

            // Parking Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).clickable {
                    if (parkingTime > 0) {
                        val uri = "geo:$parkingLat,$parkingLng?q=$parkingLat,$parkingLng(Parking Spot)"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                        context.startActivity(intent)
                    } else {
                        // Mark current location
                        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    if (location != null) {
                                        val now = System.currentTimeMillis()
                                        parkingLat = location.latitude.toFloat()
                                        parkingLng = location.longitude.toFloat()
                                        parkingTime = now
                                        sharedPrefs.edit()
                                            .putFloat("parking_lat", parkingLat)
                                            .putFloat("parking_lng", parkingLng)
                                            .putLong("parking_time", parkingTime)
                                            .apply()
                                    }
                                }
                            } catch (e: SecurityException) {
                                Log.e("ChronosLauncher", "Location permission denied", e)
                            }
                        } else {
                            launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                        }
                    }
                }) {
                    if (parkingTime > 0) {
                        val date = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(parkingTime))
                        Text(
                            "You Parked Here at $date".uppercase(),
                            color = Color(0xFF4A90E2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color(0xFF4A90E2),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Mark Your Parking Spot".uppercase(),
                                color = Color(0xFF4A90E2),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
                if (parkingTime > 0) {
                    IconButton(onClick = {
                        parkingTime = 0L
                        sharedPrefs.edit().remove("parking_time").apply()
                    }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF4A90E2).copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            ReminderSection(
                modifier = Modifier.weight(1f),
                reminders = reminders,
                onAddReminder = { text, isImportant, isPink ->
                    onRemindersChanged(reminders + Reminder(UUID.randomUUID().toString(), text, System.currentTimeMillis(), isImportant, isPink))
                },
                onRemoveReminder = { id ->
                    onRemindersChanged(reminders.filter { it.id != id })
                },
                onUpdateReminder = { id, newText, isImportant, isPink ->
                    onRemindersChanged(reminders.map { if (it.id == id) it.copy(text = newText, isImportant = isImportant, isPink = isPink) else it })
                }
            )
        }

        Column(
            modifier = Modifier
                .weight(1.2f)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Top News".uppercase(),
                    color = Color(0xFF4A90E2),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    "OPEN APP",
                    color = Color(0xFF4A90E2),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        val intent = context.packageManager.getLaunchIntentForPackage(newsAppPackage)
                        if (intent != null) {
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                    }
                )
            }

            if (newsArticles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White.copy(alpha = 0.3f),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Fetching latest news...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(newsArticles) { article ->
                        NewsCard(article)
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(article: NewsArticle) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("ChronosLauncher", "Could not open news link", e)
                }
            }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (article.urlToImage != null) {
            AsyncImage(
                model = article.urlToImage,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Newspaper, contentDescription = null, tint = Color.White.copy(alpha = 0.2f))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            if (article.source != null) {
                Text(
                    text = article.source.uppercase(),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 4.dp),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ReminderSection(
    modifier: Modifier = Modifier,
    reminders: List<Reminder>,
    onAddReminder: (String, Boolean, Boolean) -> Unit,
    onRemoveReminder: (String) -> Unit,
    onUpdateReminder: (String, String, Boolean, Boolean) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var isImportantNew by remember { mutableStateOf(false) }
    var isPinkNew by remember { mutableStateOf(false) }

    var editingId by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    var editIsImportant by remember { mutableStateOf(false) }
    var editIsPink by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Notes from my past self".uppercase(),
            color = Color(0xFF4A90E2),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            reminders.forEach { reminder ->
                if (editingId == reminder.id) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            editIsImportant = !editIsImportant
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.PriorityHigh,
                                contentDescription = "Toggle Important",
                                tint = if (editIsImportant) Color.Yellow else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(onClick = {
                            editIsPink = !editIsPink
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Toggle Pink",
                                tint = if (editIsPink) Color(0xFFFF69B4) else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        TextField(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color.White,
                                focusedTextColor = when {
                                    editIsPink -> Color(0xFFFF69B4)
                                    editIsImportant -> Color.Yellow
                                    else -> Color.White
                                },
                                unfocusedTextColor = when {
                                    editIsPink -> Color(0xFFFF69B4)
                                    editIsImportant -> Color.Yellow
                                    else -> Color.White
                                }
                            ),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 15.sp)
                        )
                        IconButton(onClick = {
                            if (editText.isNotBlank()) {
                                onUpdateReminder(reminder.id, editText, editIsImportant, editIsPink)
                                editingId = null
                            } else {
                                onRemoveReminder(reminder.id)
                                editingId = null
                            }
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFF4A90E2))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingId = reminder.id
                                editText = reminder.text
                                editIsImportant = reminder.isImportant
                                editIsPink = reminder.isPink
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PriorityHigh,
                            contentDescription = "Important",
                            tint = if (reminder.isImportant) Color.Yellow else Color.Transparent,
                            modifier = Modifier.size(16.dp).padding(horizontal = 4.dp)
                        )
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Pink",
                            tint = if (reminder.isPink) Color(0xFFFF69B4) else Color.Transparent,
                            modifier = Modifier.size(16.dp).padding(horizontal = 4.dp)
                        )
                        Text(
                            text = reminder.text,
                            color = when {
                                reminder.isPink -> Color(0xFFFF69B4)
                                reminder.isImportant -> Color.Yellow
                                else -> Color.White
                            },
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveReminder(reminder.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (isAdding) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        isImportantNew = !isImportantNew
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.PriorityHigh,
                            contentDescription = "Toggle Important",
                            tint = if (isImportantNew) Color.Yellow else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = {
                        isPinkNew = !isPinkNew
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Toggle Pink",
                            tint = if (isPinkNew) Color(0xFFFF69B4) else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Note to self...", color = Color.White.copy(alpha = 0.3f), fontSize = 15.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White,
                            focusedTextColor = when {
                                isPinkNew -> Color(0xFFFF69B4)
                                isImportantNew -> Color.Yellow
                                else -> Color.White
                            },
                            unfocusedTextColor = when {
                                isPinkNew -> Color(0xFFFF69B4)
                                isImportantNew -> Color.Yellow
                                else -> Color.White
                            }
                        ),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp)
                    )
                    IconButton(onClick = {
                        if (text.isNotBlank()) {
                            onAddReminder(text, isImportantNew, isPinkNew)
                            text = ""
                            isImportantNew = false
                            isPinkNew = false
                            isAdding = false
                        }
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFF4A90E2))
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isAdding = true
                            isImportantNew = false
                            isPinkNew = false
                        }
                        .padding(vertical = 8.dp, horizontal = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(24.dp))
                    Text(
                        if (reminders.isEmpty()) "Write a note to yourself..." else "Add another note...",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

