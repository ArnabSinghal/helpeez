package com.example.helpeez.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.helpeez.theme.BluePrimary
import com.example.helpeez.theme.BlueSecondary
import com.example.helpeez.theme.TextMuted

// Data class representing registered home details
data class HomeDetailsData(
    val name: String,
    val rooms: Int,
    val halls: Int,
    val balconies: Int,
    val address: String,
    val sweepingSelected: Boolean,
    val utensilsSelected: Boolean,
    val cookingSelected: Boolean,
    val hasDishwasher: Boolean,
    val hasWashingMachine: Boolean,
    val specialBalcony: Boolean,
    val specialDustbin: Boolean,
    val customRequest: String,
    val timingSlot: String,
    val sundayTimingSlot: String,
    val id: Int = 0,
    val assignedHelperId: Int = -1,
    val regularHelperId: Int = -1,
    val otp: String = "",
    val checkInTime: Long = 0L,
    val shiftStatus: String = "pending",
    val carpetArea: Int = 1000,
    val dailyCleaningDuration: Int = 60
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHomeDialog(
    onDismiss: () -> Unit,
    onAddHome: (HomeDetailsData) -> Unit
) {
    var homeName by remember { mutableStateOf("") }
    var roomsCount by remember { mutableStateOf(2) }
    var hallsCount by remember { mutableStateOf(1) }
    var balconiesCount by remember { mutableStateOf(1) }
    var address by remember { mutableStateOf("") }

    var sweepingSelected by remember { mutableStateOf(true) }
    var utensilsSelected by remember { mutableStateOf(false) }
    var cookingSelected by remember { mutableStateOf(false) }

    var hasDishwasher by remember { mutableStateOf(false) }
    var hasWashingMachine by remember { mutableStateOf(false) }

    var specialBalcony by remember { mutableStateOf(false) }
    var specialDustbin by remember { mutableStateOf(false) }
    var customRequest by remember { mutableStateOf("") }

    var timingSlot by remember { mutableStateOf("08:00 AM - 09:00 AM") }
    var sundayTimingSlot by remember { mutableStateOf("Same as daily") }
    
    // New Quoting Variables
    var carpetArea by remember { mutableStateOf(1000f) } // Range 300 to 3000
    var dailyCleaningDuration by remember { mutableStateOf(60) } // 30, 45, 60, 90 mins

    // Dynamic Pricing Quote Calculation
    val basePrice = 1500
    val layoutCharge = (roomsCount * 200) + (hallsCount * 300) + (balconiesCount * 150)
    val taskCharge = (if (sweepingSelected) 300 else 0) + (if (utensilsSelected) 400 else 0) + (if (cookingSelected) 800 else 0)
    val addonCharge = (if (specialBalcony) 100 else 0) + (if (specialDustbin) 50 else 0)
    
    // Multipliers
    val areaMultiplier = carpetArea / 1000f
    val durationMultiplier = dailyCleaningDuration / 60f
    
    val estimatedMonthlyQuote = ((basePrice + layoutCharge + taskCharge + addonCharge) * areaMultiplier * durationMultiplier).toInt()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add New Home",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Home Nickname
                    OutlinedTextField(
                        value = homeName,
                        onValueChange = { homeName = it },
                        label = { Text("Home Nickname (e.g. My Apartment, Parent's)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BluePrimary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Counters for Rooms, Halls, Balconies
                    Text(
                        text = "Layout Details",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BluePrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CounterWidget(label = "Rooms", count = roomsCount, onValueChange = { roomsCount = it })
                        CounterWidget(label = "Halls", count = hallsCount, onValueChange = { hallsCount = it })
                        CounterWidget(label = "Balconies", count = balconiesCount, onValueChange = { balconiesCount = it })
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Carpet Area Slider Selector
                    Text(
                        text = "Carpet Area: ${carpetArea.toInt()} sq. ft.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BluePrimary
                    )
                    Slider(
                        value = carpetArea,
                        onValueChange = { carpetArea = it },
                        valueRange = 300f..3000f,
                        steps = 26, // Steps of 100 sq ft
                        colors = SliderDefaults.colors(
                            thumbColor = BluePrimary,
                            activeTrackColor = BluePrimary,
                            inactiveTrackColor = Color(0xFFE2E8F0)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Daily Cleaning Session Duration Selector
                    Text(
                        text = "Daily Clean Session Duration",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BluePrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 45, 60, 90).forEach { mins ->
                            val isSelected = dailyCleaningDuration == mins
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { dailyCleaningDuration = mins },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) BluePrimary else Color(0xFFF1F5F9)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$mins Min",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Coverage Warning Banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), // Soft green/blue tint
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Standard sweeping covers rooms, halls, kitchen & balconies. Bathrooms deep clean must be requested separately.",
                                fontSize = 11.sp,
                                color = Color(0xFF15803D),
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Address Form
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Exact Home Address / Location") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BluePrimary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chores Selector
                    Text(
                        text = "Select Domestic Tasks Needed",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BluePrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = sweepingSelected,
                            onClick = { sweepingSelected = !sweepingSelected },
                            label = { Text("Sweeping/Mopping") }
                        )
                        FilterChip(
                            selected = utensilsSelected,
                            onClick = { utensilsSelected = !utensilsSelected },
                            label = { Text("Utensils Clean") }
                        )
                        FilterChip(
                            selected = cookingSelected,
                            onClick = { cookingSelected = !cookingSelected },
                            label = { Text("Daily Cooking") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Appliances details
                    Text(
                        text = "Applicable Appliances Available",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BluePrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasDishwasher, onCheckedChange = { hasDishwasher = it })
                            Text("Dishwasher", fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasWashingMachine, onCheckedChange = { hasWashingMachine = it })
                            Text("Washing Machine", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Special Requests
                    Text(
                        text = "Special Daily Requests (Add-ons)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BluePrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = specialBalcony, onCheckedChange = { specialBalcony = it })
                        Text("Mop balcony floor (+₹100)", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = specialDustbin, onCheckedChange = { specialDustbin = it })
                        Text("Wash garbage bins (+₹50)", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom Requests
                    OutlinedTextField(
                        value = customRequest,
                        onValueChange = { customRequest = it },
                        label = { Text("Custom Requests / Notes for Helpers") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BluePrimary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timing Slots
                    Text(
                        text = "Preferred Cleaning Timings",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BluePrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = timingSlot,
                        onValueChange = { timingSlot = it },
                        label = { Text("Daily Time Slot (e.g. 08:00 AM - 09:00 AM)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = sundayTimingSlot,
                        onValueChange = { sundayTimingSlot = it },
                        label = { Text("Sunday Cleaning Slot (e.g. Same as daily, or Offline)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Subscription Pricing Quote Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), // Soft green/blue
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Estimated Monthly Subscription Quote",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹$estimatedMonthlyQuote / month",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "Includes stable salary, insurance, replacement helper coverage, and Aarti Sharma assigned.",
                                fontSize = 9.sp,
                                color = Color(0xFF166534).copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // CTA Button
                Button(
                    onClick = {
                        if (homeName.isNotBlank() && address.isNotBlank()) {
                            onAddHome(
                                HomeDetailsData(
                                    name = homeName,
                                    rooms = roomsCount,
                                    halls = hallsCount,
                                    balconies = balconiesCount,
                                    address = address,
                                    sweepingSelected = sweepingSelected,
                                    utensilsSelected = utensilsSelected,
                                    cookingSelected = cookingSelected,
                                    hasDishwasher = hasDishwasher,
                                    hasWashingMachine = hasWashingMachine,
                                    specialBalcony = specialBalcony,
                                    specialDustbin = specialDustbin,
                                    customRequest = customRequest,
                                    timingSlot = timingSlot,
                                    sundayTimingSlot = sundayTimingSlot,
                                    carpetArea = carpetArea.toInt(),
                                    dailyCleaningDuration = dailyCleaningDuration
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    enabled = homeName.isNotBlank() && address.isNotBlank()
                ) {
                    Text(
                        text = "Save & Matches Helpers",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CounterWidget(
    label: String,
    count: Int,
    onValueChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(10.dp)
            .width(80.dp)
    ) {
        Text(text = label, fontSize = 12.sp, color = TextMuted)
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = BluePrimary,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { if (count > 0) onValueChange(count - 1) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = BluePrimary)
            ) {
                Text("-", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Button(
                onClick = { onValueChange(count + 1) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = BluePrimary)
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
