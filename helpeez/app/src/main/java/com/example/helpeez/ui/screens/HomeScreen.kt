package com.example.helpeez.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.helpeez.data.DatabaseHelper
import com.example.helpeez.data.NetworkClient
import com.example.helpeez.data.UserData
import com.example.helpeez.theme.*
import kotlinx.coroutines.launch

// Mock Helper Profile representing details to display to owner
data class HelperProfile(
    val name: String,
    val rating: String,
    val phone: String,
    val specialty: String,
    val statusText: String,
    val progress: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userId: Int,
    role: String, // "owner" or "helper"
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    // Initialize TextToSpeech engine for real-time voice assistance
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    DisposableEffect(context) {
        var ttsInstance: android.speech.tts.TextToSpeech? = null
        ttsInstance = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                ttsInstance?.setLanguage(java.util.Locale("hi", "IN"))
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }



    // Shared Settings State (Hardcoded online backend database url)
    val syncEnabled = true
    val syncUrl = "https://helpeez-database.onrender.com"

    // User Profile
    var currentUserProfile by remember { mutableStateOf<UserData?>(null) }

    // List of homes (owner's properties or helper's assigned jobs)
    var homesList by remember { mutableStateOf(emptyList<HomeDetailsData>()) }
    var selectedHomeIndex by remember { mutableStateOf(0) }
    var showAddHomeDialog by remember { mutableStateOf(false) }

    // Helper Availability Day Shift state
    var dayShiftStarted by remember { mutableStateOf(false) }

    // Timer Ticker State
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var helperProgress by remember { mutableStateOf(0.1f) }

    // Holiday Replacement substitute state
    var isHolidayReplacement by remember { mutableStateOf(false) }
    var replacementHelperName by remember { mutableStateOf("") }

    // Load SharedPreferences Settings & Database Profile
    LaunchedEffect(userId) {
        val sharedPrefs = context.getSharedPreferences("helpeez_settings", Context.MODE_PRIVATE)
        dayShiftStarted = sharedPrefs.getBoolean("helper_shift_started", false)

        currentUserProfile = dbHelper.getUserById(userId)
    }

    // 1-Second Timer Ticker & Helper Map Progress simulation
    LaunchedEffect(Unit) {
        var p = 0.1f
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = System.currentTimeMillis()
            p = if (p >= 0.95f) 0.1f else p + 0.02f
            helperProgress = p
        }
    }

    // Periodic Database & API Sync (1-second delay when active, 5-second when idle)
    LaunchedEffect(userId, syncEnabled, syncUrl, role, homesList) {
        while (true) {
            try {
                if (role == "owner") {
                    val remoteHomes = if (syncEnabled) NetworkClient.fetchHomes(syncUrl, userId) else emptyList()
                    val localHomes = dbHelper.getHomesForUser(userId)
                    
                    if (syncEnabled) {
                        homesList = remoteHomes
                        // Sync missing remote homes into local db
                        remoteHomes.forEach { rHome ->
                            val existsLocally = localHomes.any { it.id == rHome.id || it.name == rHome.name }
                            if (!existsLocally) {
                                dbHelper.insertHome(userId, rHome)
                            } else {
                                // Sync shift status & check-in time
                                dbHelper.updateHomeShiftStatus(rHome.id, rHome.shiftStatus, rHome.checkInTime)
                            }
                        }
                    } else {
                        homesList = localHomes
                    }
                } else {
                    // Helper View: Fetch assigned jobs
                    val remoteJobs = if (syncEnabled) NetworkClient.fetchJobsForHelper(syncUrl, userId) else emptyList()
                    val localJobs = dbHelper.getJobsForHelper(userId)
                    
                    if (syncEnabled) {
                        homesList = remoteJobs
                        // Sync remote jobs to local db
                        remoteJobs.forEach { rJob ->
                            val existsLocally = localJobs.any { it.id == rJob.id }
                            if (!existsLocally) {
                                dbHelper.insertHome(rJob.assignedHelperId, rJob)
                            } else {
                                dbHelper.updateHomeShiftStatus(rJob.id, rJob.shiftStatus, rJob.checkInTime)
                            }
                        }
                    } else {
                        homesList = localJobs
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // 1-second polling if work session is pending or started, otherwise 5 seconds
            val hasActiveSession = homesList.any { it.shiftStatus == "started" || it.shiftStatus == "pending" }
            val delayTime = if (hasActiveSession && syncEnabled) 1000L else 5000L
            kotlinx.coroutines.delay(delayTime)
        }
    }

    val activeHome = if (homesList.isNotEmpty() && selectedHomeIndex in homesList.indices) {
        homesList[selectedHomeIndex]
    } else null

    // Helper Profile Info Card
    val mockHelper = HelperProfile(
        name = "Aarti Sharma",
        rating = "4.8 (142 completed)",
        phone = "+91 98765 43210",
        specialty = "Dusting & Kitchen Hygiene",
        statusText = "Aarti is on the way (5 mins away)",
        progress = 0.65f
    )

    // Match assigned helper name dynamically
    var assignedHelperName by remember { mutableStateOf("Aarti Sharma") }
    var assignedHelperPhone by remember { mutableStateOf("+91 98765 43210") }

    LaunchedEffect(activeHome) {
        if (activeHome != null && activeHome.assignedHelperId != -1) {
            isHolidayReplacement = activeHome.regularHelperId != -1
            
            val hProfile = dbHelper.getUserById(activeHome.assignedHelperId)
            if (hProfile != null) {
                assignedHelperName = hProfile.name
                assignedHelperPhone = hProfile.phone
            } else {
                assignedHelperName = "Aarti Sharma"
                assignedHelperPhone = "+91 98765 43210"
            }
        }
    }

    // Auto-Transition Shift status to completed when timer runs down (Helper only, to avoid homeowner clock-drift race conditions)
    LaunchedEffect(activeHome, currentTime, role) {
        if (role == "helper" && activeHome != null && activeHome.shiftStatus == "started" && activeHome.checkInTime > 0L) {
            val shiftDurationSeconds = activeHome.dailyCleaningDuration * 60L
            val elapsedTimeSeconds = (currentTime - activeHome.checkInTime) / 1000
            val remainingSeconds = maxOf(0L, shiftDurationSeconds - elapsedTimeSeconds)
            if (remainingSeconds == 0L) {
                if (syncEnabled) {
                    NetworkClient.updateHomeShiftStatus(syncUrl, activeHome.id, "completed", activeHome.checkInTime)
                } else {
                    dbHelper.updateHomeShiftStatus(activeHome.id, "completed", activeHome.checkInTime)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("H", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (role == "owner") "Helpeez Dashboard" else "Helpeez Helper Portal",
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    Text(
                        text = currentUserProfile?.name ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LightBackground)
        ) {
            if (role == "owner") {
                // HOMEOWNER DASHBOARD
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Multi-home switcher cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Homes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        TextButton(onClick = { showAddHomeDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Home", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Home", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        homesList.forEachIndexed { index, home ->
                            val isSelected = index == selectedHomeIndex
                            Card(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable { selectedHomeIndex = index },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surface
                                ),
                                border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else BluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = home.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "${home.rooms} Rooms, ${home.carpetArea} sq ft",
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (homesList.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Homes Setup Yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "Setup your home layout and get a custom monthly subscription quote instantly.",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                )
                                Button(
                                    onClick = { showAddHomeDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Add Your First Home", color = Color.White)
                                }
                            }
                        }
                    }

                    if (activeHome != null) {
                        // Home Status details
                        Text(
                            text = "Matched Help Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF0FDF4)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF22C55E))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(assignedHelperName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                            if (isHolidayReplacement) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Replacement",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEA580C),
                                                    modifier = Modifier
                                                        .background(Color(0xFFFFEDD5), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text("Rating: ${mockHelper.rating}", fontSize = 11.sp, color = TextMuted)
                                    }
                                    Text(
                                        text = when (activeHome.shiftStatus) {
                                            "started" -> "Session Active"
                                            "completed" -> "Completed"
                                            else -> "En Route"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (activeHome.shiftStatus) {
                                            "started" -> Color(0xFF166534)
                                            "completed" -> Color.Gray
                                            else -> BluePrimary
                                        },
                                        modifier = Modifier
                                            .background(
                                                when (activeHome.shiftStatus) {
                                                    "started" -> Color(0xFFDCFCE7)
                                                    "completed" -> Color(0xFFF1F5F9)
                                                    else -> Color(0xFFE0F2FE)
                                                },
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Contact Helper:", fontSize = 11.sp, color = TextMuted)
                                    Text(assignedHelperPhone, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BluePrimary)
                                }

                                if (isHolidayReplacement) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                                        border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFEA580C),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            var regularHelperName by remember { mutableStateOf("Regular Helper") }
                                            LaunchedEffect(activeHome.regularHelperId) {
                                                val h = dbHelper.getUserById(activeHome.regularHelperId)
                                                if (h != null) regularHelperName = h.name
                                            }
                                            Text(
                                                text = "Note: Your regular helper $regularHelperName is on leave. Substitute helper $assignedHelperName has been assigned to complete your house cleaning session today.",
                                                fontSize = 11.sp,
                                                color = Color(0xFFC2410C),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Active State Tracker
                        if (activeHome.shiftStatus == "started") {
                            // ACTIVE WORK SESSION: Show Circular countdown Timer & Shift Extension Controls
                            val shiftDurationSeconds = activeHome.dailyCleaningDuration * 60L
                            val elapsedTimeSeconds = if (activeHome.checkInTime > 0L) (currentTime - activeHome.checkInTime) / 1000 else 0L
                            val remainingSeconds = maxOf(0L, shiftDurationSeconds - elapsedTimeSeconds)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Active Cleaning Session",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Helper is working in your house. Extend the work session or complete early below.",
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                    )

                                    CircularShiftTimer(
                                        remainingSeconds = remainingSeconds,
                                        totalSeconds = shiftDurationSeconds
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    // Extend session by 15 mins (900,000 ms) in checkInTime
                                                    val extendedCheckIn = activeHome.checkInTime + 900000L
                                                    if (syncEnabled) {
                                                        NetworkClient.updateHomeShiftStatus(syncUrl, activeHome.id, "started", extendedCheckIn)
                                                    } else {
                                                        dbHelper.updateHomeShiftStatus(activeHome.id, "started", extendedCheckIn)
                                                    }
                                                    android.widget.Toast.makeText(context, "Session extended by 15 mins (+₹200 added to invoice)", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Extend 15 mins", fontSize = 11.sp, color = Color.White)
                                        }

                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    // End session early
                                                    if (syncEnabled) {
                                                        NetworkClient.updateHomeShiftStatus(syncUrl, activeHome.id, "completed", activeHome.checkInTime)
                                                    } else {
                                                        dbHelper.updateHomeShiftStatus(activeHome.id, "completed", activeHome.checkInTime)
                                                    }
                                                    android.widget.Toast.makeText(context, "Work session completed early.", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("End Session Early", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        } else if (activeHome.shiftStatus == "completed") {
                            // COMPLETED WORK SESSION: Show completion details
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Cleaning Session Complete!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF166534)
                                    )
                                    Text(
                                        text = "Today's work shift has been logged. Daily checklist tasks completed successfully.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF166534).copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            // PENDING WORK SESSION: Show Live Route Map and Random OTP Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LeafletMapView(
                                        address = activeHome.address,
                                        progress = helperProgress,
                                        isBackup = isHolidayReplacement,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Random OTP Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Session Check-in Code (OTP)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BluePrimary
                                    )
                                    Text(
                                        text = "Give this randomized security code to helper Aarti Sharma when she arrives to verify work session start.",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                    Text(
                                        text = activeHome.otp,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BluePrimary,
                                        letterSpacing = 8.sp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }


                }
            } else {
                // DOMESTIC HELPER / WORKER PORTAL VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!dayShiftStarted) {
                        // OFFLINE AVAILABILITY SCREEN: Start Availability Day Shift
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                              ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0F2FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Availability Shift: Offline",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Start your daily availability shift to show you are online and ready to receive matched house cleaning sessions.",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                
                                Button(
                                    onClick = {
                                        dayShiftStarted = true
                                        context.getSharedPreferences("helpeez_settings", Context.MODE_PRIVATE)
                                            .edit()
                                            .putBoolean("helper_shift_started", true)
                                            .apply()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                                ) {
                                    Text("Start Daily Availability Shift", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // HELPER PORTAL WORK SPACE
                        if (homesList.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No Active Bookings Assigned",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = "You are not assigned to any homeowner properties currently. Matched schedules will show up here.",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            // If helper has multiple jobs, let them switch
                            if (homesList.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    homesList.forEachIndexed { index, h ->
                                        val isSelected = index == selectedHomeIndex
                                        Card(
                                            modifier = Modifier
                                                .clickable { selectedHomeIndex = index },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surface
                                            ),
                                            border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null
                                        ) {
                                            Text(
                                                text = h.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color(0xFF1E293B),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Active job display
                            val job = homesList[selectedHomeIndex]
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Active Assignment", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    if (job.regularHelperId != -1 && job.regularHelperId != userId) {
                                        Text(
                                            text = "Replacement Duty / प्रतिस्थापन कर्तव्य",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEA580C)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (job.regularHelperId != -1 && job.regularHelperId != userId) Color(0xFFFFEDD5) else Color(0xFFE0F2FE),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = job.name,
                                        color = if (job.regularHelperId != -1 && job.regularHelperId != userId) Color(0xFFEA580C) else BluePrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (job.regularHelperId == userId) {
                                // HOLIDAY STATUS: Regular helper is currently on holiday for this property
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                                    border = BorderStroke(1.dp, Color(0xFFFFEDD5))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFFEDD5)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(28.dp))
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "आप छुट्टी पर हैं (On Leave)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFFC2410C)
                                        )
                                        var substituteName by remember { mutableStateOf("Replacement Helper") }
                                        LaunchedEffect(job.assignedHelperId) {
                                            val h = dbHelper.getUserById(job.assignedHelperId)
                                            if (h != null) substituteName = h.name
                                        }
                                        Text(
                                            text = "आप इस घर (${job.name}) के काम से छुट्टी पर हैं। आज का काम प्रतिस्थापन सहायक $substituteName संभाल रहे हैं।",
                                            fontSize = 12.sp,
                                            color = Color(0xFF9A3412),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                        )
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    if (syncEnabled) {
                                                        NetworkClient.updateHomeHolidaySync(syncUrl, job.id, userId, -1)
                                                    } else {
                                                        dbHelper.updateHomeHoliday(job.id, userId, -1)
                                                    }
                                                    homesList = if (syncEnabled) {
                                                        NetworkClient.fetchJobsForHelper(syncUrl, userId)
                                                    } else {
                                                        dbHelper.getJobsForHelper(userId)
                                                    }
                                                    android.widget.Toast.makeText(context, "Resumed duty successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "छुट्टी समाप्त करें / काम पर लौटें\n(End Leave / Resume Duty)",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                // NORMAL JOB FLOW (Verification, active timer, tasks checklists, etc.)
                                if (job.shiftStatus == "pending") {
                                    // BEFORE START: Enter OTP to start shift
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Address / पता:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                            }
                                            Text(
                                                text = job.address,
                                                fontSize = 12.sp,
                                                color = Color(0xFF334155),
                                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                            )

                                            HorizontalDivider(color = Color(0xFFF1F5F9))
                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Display Home Layout specs & Session target time
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Text("Rooms: ${job.rooms}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                                                Text("Area: ${job.carpetArea} sq ft", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                                                Text("Target: ${job.dailyCleaningDuration} Mins", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Start Shift Card
                                    HelperBilingualScreen(
                                        helper = mockHelper,
                                        correctOtp = job.otp,
                                        tts = tts,
                                        tasks = listOf(
                                            "Sweep & mop living room" to job.sweepingSelected,
                                            "Sweep & mop 2 bedrooms" to (job.rooms >= 2),
                                            "Wash kitchen dishes" to job.utensilsSelected,
                                            "Mop balcony floor" to job.specialBalcony,
                                            "Wash garbage bins" to job.specialDustbin
                                        ),
                                        onVerifyOTP = {
                                            coroutineScope.launch {
                                                val checkTime = System.currentTimeMillis()
                                                if (syncEnabled) {
                                                    NetworkClient.updateHomeShiftStatus(syncUrl, job.id, "started", checkTime)
                                                    homesList = NetworkClient.fetchJobsForHelper(syncUrl, userId)
                                                } else {
                                                    dbHelper.updateHomeShiftStatus(job.id, "started", checkTime)
                                                    homesList = dbHelper.getJobsForHelper(userId)
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Request Holiday option for Regular Helper (when shift is pending)
                                    if (job.regularHelperId == -1) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text("Request Leave / छुट्टी का आवेदन", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                                Text(
                                                    text = "If you need a holiday today, request it here. A substitute helper will be randomly assigned automatically.",
                                                    fontSize = 11.sp,
                                                    color = TextMuted,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            val allHelpers = dbHelper.getAllHelpers()
                                                            val filterHelpers = allHelpers.filter { it.id != userId && it.email.trim().lowercase() != currentUserProfile?.email?.trim()?.lowercase() }
                                                            if (filterHelpers.isNotEmpty()) {
                                                                val substitute = filterHelpers.random()
                                                                if (syncEnabled) {
                                                                    NetworkClient.updateHomeHolidaySync(syncUrl, job.id, substitute.id, userId)
                                                                } else {
                                                                    dbHelper.updateHomeHoliday(job.id, substitute.id, userId)
                                                                }
                                                                homesList = if (syncEnabled) {
                                                                    NetworkClient.fetchJobsForHelper(syncUrl, userId)
                                                                } else {
                                                                    dbHelper.getJobsForHelper(userId)
                                                                }
                                                                android.widget.Toast.makeText(context, "Holiday requested. substitute helper ${substitute.name} assigned!", android.widget.Toast.LENGTH_LONG).show()
                                                            } else {
                                                                android.widget.Toast.makeText(context, "No other helpers available.", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                                ) {
                                                    Text("छुट्टी का अनुरोध करें (Request Holiday)", color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                } else if (job.shiftStatus == "started") {
                                    // ACTIVE SHIFT: Live Circle countdown and Bilingual Checklists
                                    val shiftDurationSeconds = job.dailyCleaningDuration * 60L
                                    val elapsedTimeSeconds = if (job.checkInTime > 0L) (currentTime - job.checkInTime) / 1000 else 0L
                                    val remainingSeconds = maxOf(0L, shiftDurationSeconds - elapsedTimeSeconds)

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "ड्युटी समय सीमा (Active Shift Timer)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF166534)
                                            )
                                            Text(
                                                text = "Please complete chores. Owner can extend timer or end early.",
                                                fontSize = 11.sp,
                                                color = TextMuted,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                                            )

                                            CircularShiftTimer(
                                                remainingSeconds = remainingSeconds,
                                                totalSeconds = shiftDurationSeconds
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Bilingual task checklist items with Audio helper
                                    Text("कामों की सूची (Work Task Checklist)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(bottom = 8.dp))
                                    
                                    val listTasks = listOf(
                                        "Sweep & mop living room" to job.sweepingSelected,
                                        "Sweep & mop 2 bedrooms" to (job.rooms >= 2),
                                        "Wash kitchen dishes" to job.utensilsSelected,
                                        "Mop balcony floor" to job.specialBalcony,
                                        "Wash garbage bins" to job.specialDustbin
                                    )

                                    val translationMap = mapOf(
                                        "Sweep & mop living room" to "लिविंग रूम में झाड़ू और पोछा लगाना",
                                        "Sweep & mop 2 bedrooms" to "कमरे में झाड़ू और पोछा लगाना",
                                        "Wash kitchen dishes" to "रसोई के बर्तन साफ करना",
                                        "Mop balcony floor" to "बालकनी के फर्श पर पोछा लगाना",
                                        "Wash garbage bins" to "कचरे के डिब्बे साफ करना"
                                    )

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            listTasks.forEachIndexed { index, (taskText, isRequired) ->
                                                if (isRequired) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(translationMap[taskText] ?: "सफाई काम", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                            Text(taskText, fontSize = 11.sp, color = TextMuted)
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                val text = translationMap[taskText] ?: "सफाई काम"
                                                                tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "🔊 बोल रहे हैं: $text",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        ) {
                                                            Icon(Icons.Default.PlayArrow, contentDescription = "Listen", tint = BluePrimary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // COMPLETED VIEW: Shift Success Status
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF22C55E)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "काम सफलतापूर्वक समाप्त हुआ",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color(0xFF166534)
                                            )
                                            Text(
                                                text = "House work shift completed. Return online when ready.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF166534).copy(alpha = 0.8f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Button to end the day availability shift
                        Button(
                            onClick = {
                                dayShiftStarted = false
                                context.getSharedPreferences("helpeez_settings", Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("helper_shift_started", false)
                                    .apply()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("End Availability Day Shift", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal Add Home dialog
    if (showAddHomeDialog) {
        AddHomeDialog(
            onDismiss = { showAddHomeDialog = false },
            onAddHome = { newHome ->
                coroutineScope.launch {
                    val isSync = true
                    val url = "https://helpeez-database.onrender.com"

                    if (isSync) {
                        val success = NetworkClient.saveHome(url, userId, newHome)
                        if (success) {
                            homesList = NetworkClient.fetchHomes(url, userId)
                        }
                    } else {
                        dbHelper.insertHome(userId, newHome)
                        homesList = dbHelper.getHomesForUser(userId)
                    }
                    selectedHomeIndex = homesList.size - 1
                    showAddHomeDialog = false
                }
            }
        )
    }
}

@Composable
fun CircularShiftTimer(
    remainingSeconds: Long,
    totalSeconds: Long,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val sweepAngle = 360f * progress

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val radius = diameter / 2f

            // Background arc (gray)
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // Depleting progress arc (green)
            drawArc(
                color = Color(0xFF22C55E),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "time remaining",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun LeafletMapView(
    address: String,
    progress: Float,
    isBackup: Boolean,
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(address) {
        val escapedAddress = address
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body, html, #map { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #F8FAFC; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var homeLat = 12.9716;
                var homeLng = 77.5946;
                var startLat = 12.9820;
                var startLng = 77.6080;
                
                var map = L.map('map', { zoomControl: false }).setView([homeLat, homeLng], 14);
                
                L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                    maxZoom: 18,
                    attribution: ''
                }).addTo(map);
                
                var homeIcon = L.divIcon({
                    html: '<div style="background-color: #0EA5E9; border: 2px solid white; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 4px rgba(0,0,0,0.3);"><svg width="12" height="12" viewBox="0 0 24 24" fill="white"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg></div>',
                    className: '',
                    iconSize: [24, 24],
                    iconAnchor: [12, 12]
                });
                
                var homeMarker = L.marker([homeLat, homeLng], {icon: homeIcon}).addTo(map);
                homeMarker.bindPopup("<b>Home Location</b><br>${escapedAddress}").openPopup();
                
                var helperIconHtml = '<div id="helper-marker-icon" style="background-color: #22C55E; border: 2px solid white; border-radius: 50%; width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 6px rgba(0,0,0,0.4);"><svg width="14" height="14" viewBox="0 0 24 24" fill="white"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H7c0-2.76 2.24-5 5-5s5 2.24 5 5c0 1.04-.42 1.99-1.07 2.25z"/></svg></div>';
                
                var helperIcon = L.divIcon({
                    html: helperIconHtml,
                    className: '',
                    iconSize: [28, 28],
                    iconAnchor: [14, 14]
                });
                
                var helperMarker = L.marker([startLat, startLng], {icon: helperIcon}).addTo(map);
                var routeLine = null;

                window.updateHelperPosition = function(progress, isBackup) {
                    var el = document.getElementById('helper-marker-icon');
                    if (el) {
                        el.style.backgroundColor = isBackup ? '#EF4444' : '#22C55E';
                    }
                    
                    var currentLat = startLat + (homeLat - startLat) * progress;
                    var currentLng = startLng + (homeLng - startLng) * progress;
                    
                    helperMarker.setLatLng([currentLat, currentLng]);
                    
                    if (routeLine) {
                        map.removeLayer(routeLine);
                    }
                    routeLine = L.polyline([[currentLat, currentLng], [homeLat, homeLng]], {
                        color: isBackup ? '#EF4444' : '#38BDF8',
                        weight: 4,
                        dashArray: '5, 10'
                    }).addTo(map);
                    
                    var group = new L.featureGroup([homeMarker, helperMarker]);
                    map.fitBounds(group.getBounds().pad(0.15));
                };
                
                // Run initially
                updateHelperPosition($progress, $isBackup);
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    androidx.compose.runtime.key(address) {
        AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    webViewClient = android.webkit.WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                val script = "if(typeof updateHelperPosition === 'function') { updateHelperPosition($progress, $isBackup); }"
                webView.evaluateJavascript(script, null)
            },
            modifier = modifier
        )
    }
}

@Composable
fun HelperBilingualScreen(
    helper: HelperProfile,
    correctOtp: String,
    tasks: List<Pair<String, Boolean>>,
    onVerifyOTP: () -> Unit,
    tts: android.speech.tts.TextToSpeech?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var otpInput by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var playingTaskIndex by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Helper translation map
    val translationMap = mapOf(
        "Sweep & mop living room" to "लिविंग रूम में झाड़ू और पोछा लगाना (झाड़ू और पोछा)",
        "Sweep & mop 2 bedrooms" to "2 बेडरूम में झाड़ू और पोछा लगाना (झाड़ू और पोछा)",
        "Wash kitchen dishes" to "रसोई के बर्तन साफ करना (बर्तन धोना)",
        "Mop balcony floor" to "बालकनी के फर्श पर पोछा लगाना (पोछा)",
        "Wash garbage bins" to "कचरे के डिब्बे साफ करना (डस्टबिन)"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome Header (Bilingual)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BluePrimary)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "नमस्ते, ${helper.name}!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Hello, ${helper.name}!",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Voice guidance badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Voice help",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🔊 आवाज सहायता चालू है",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tasks Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "आज के काम",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Today's Tasks",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
            Box(
                modifier = Modifier
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("${tasks.count { it.second }} Tasks", color = BluePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Task List Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                var displayedIdx = 1
                tasks.forEachIndexed { index, (taskText, isRequired) ->
                    if (isRequired) {
                        val hindiTranslation = translationMap[taskText] ?: "घर का काम"
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(
                                    if (playingTaskIndex == index) Color(0xFFF0F9FF) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Task Status Icon
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFEFF6FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$displayedIdx",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))

                            // Bilingual Task Text
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = hindiTranslation,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = taskText,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            // Audio Simulation Trigger Button
                            IconButton(
                                onClick = {
                                    playingTaskIndex = index
                                    tts?.speak(hindiTranslation, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                    android.widget.Toast.makeText(
                                        context,
                                        "🔊 पढ़ रहे हैं: $hindiTranslation",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    
                                    // Reset speaking animation after 3 seconds
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(3000)
                                        if (playingTaskIndex == index) {
                                            playingTaskIndex = null
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (playingTaskIndex == index) Icons.Default.Star else Icons.Default.PlayArrow,
                                    contentDescription = "Listen task",
                                    tint = if (playingTaskIndex == index) Color(0xFFEAB308) else BluePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        displayedIdx++
                        if (index < tasks.size - 1) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // OTP Verification Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "घर पहुँच कर चेक-इन दर्ज करें",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF166534)
                )
                Text(
                    text = "Verify Check-in at Home",
                    fontSize = 12.sp,
                    color = Color(0xFF166534).copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = otpInput,
                    onValueChange = {
                        otpInput = it
                        otpError = null
                    },
                    label = { Text("सुरक्षा कोड (OTP) दर्ज करें") },
                    placeholder = { Text("उदा. $correctOtp") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    isError = otpError != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF22C55E),
                        unfocusedBorderColor = Color(0xFF86EFAC),
                        errorBorderColor = Color.Red
                    )
                )

                if (otpError != null) {
                    Text(
                        text = otpError!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (otpInput.trim() == correctOtp.trim()) {
                            android.widget.Toast.makeText(
                                context,
                                "चेक-इन सफल! (Check-in Successful)",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            onVerifyOTP()
                        } else {
                            otpError = "गलत कोड! मालिक से सही सुरक्षा कोड माँगें। (Incorrect Code!)"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "सत्यापित करें (Verify Code)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
