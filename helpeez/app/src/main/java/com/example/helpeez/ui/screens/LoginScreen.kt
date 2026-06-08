package com.example.helpeez.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helpeez.data.DatabaseHelper
import com.example.helpeez.data.NetworkClient
import com.example.helpeez.theme.BluePrimary
import com.example.helpeez.theme.BlueSecondary
import com.example.helpeez.theme.TextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (Int, String) -> Unit, // Updated to pass role string
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("owner") } // "owner" or "helper"
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Smooth entry alpha animation
    var startAnim by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "Alpha"
    )

    LaunchedEffect(Unit) {
        startAnim = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE), // Light sky blue
                        Color(0xFFF8FAFC)  // Soft white
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(alphaAnim)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(BluePrimary, BlueSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "H",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Helpeez",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )

                Text(
                    text = "Reliable daily help, zero inconvenience",
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Modern Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isLoginMode) Color.White else Color.Transparent)
                            .clickable { 
                                isLoginMode = true 
                                errorMessage = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Login",
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLoginMode) BluePrimary else TextMuted,
                            fontSize = 14.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!isLoginMode) Color.White else Color.Transparent)
                            .clickable { 
                                isLoginMode = false 
                                errorMessage = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign Up",
                            fontWeight = FontWeight.SemiBold,
                            color = if (!isLoginMode) BluePrimary else TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error Message Section
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                // Fields
                AnimatedVisibility(
                    visible = !isLoginMode,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Column {
                        // Role Picker Switcher
                        Text(
                            text = "Register As",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (role == "owner") Color.White else Color.Transparent)
                                    .clickable { role = "owner" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Homeowner",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (role == "owner") BluePrimary else TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (role == "helper") Color.White else Color.Transparent)
                                    .clickable { role = "helper" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Helper / Worker",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (role == "helper") BluePrimary else TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 10) {
                                    phone = input
                                }
                            },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // CTA Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            errorMessage = ""
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please fill in all email and password fields."
                                return@launch
                            }
                            if (!isLoginMode) {
                                if (name.isBlank() || phone.isBlank()) {
                                    errorMessage = "Please fill in all profile fields."
                                    return@launch
                                }
                                if (phone.length != 10) {
                                    errorMessage = "Phone number must be exactly 10 digits."
                                    return@launch
                                }
                            }
                            if (!email.contains("@") || !email.contains(".")) {
                                errorMessage = "Please enter a valid email address."
                                return@launch
                            }

                            isLoading = true

                            val syncEnabled = true
                            val syncUrl = "https://helpeez-database.onrender.com"

                            if (isLoginMode) {
                                // Login Flow
                                if (syncEnabled) {
                                    val user = NetworkClient.login(syncUrl, email, password)
                                    if (user != null) {
                                        // Cache locally for offline fallback
                                        dbHelper.registerUser(user.email, user.name, user.phone, password, user.role, id = user.id)
                                        isLoading = false
                                        onLoginSuccess(user.id, user.role)
                                    } else {
                                        isLoading = false
                                        errorMessage = "API connection failed. Please verify the sync server is running at the configured URL or turn OFF Local Server Sync to login locally."
                                    }
                                } else {
                                    val user = dbHelper.loginUser(email, password)
                                    isLoading = false
                                    if (user != null) {
                                        onLoginSuccess(user.id, user.role)
                                    } else {
                                        errorMessage = "Invalid email address or password."
                                    }
                                }
                            } else {
                                // Sign Up Flow
                                if (syncEnabled) {
                                    val success = NetworkClient.register(syncUrl, email, name, phone, password, role)
                                    if (success) {
                                        val user = NetworkClient.login(syncUrl, email, password)
                                        isLoading = false
                                        if (user != null) {
                                            dbHelper.registerUser(user.email, user.name, user.phone, password, user.role, id = user.id)
                                            onLoginSuccess(user.id, user.role)
                                        } else {
                                            errorMessage = "Signup succeeded but login failed."
                                        }
                                    } else {
                                        isLoading = false
                                        errorMessage = "API connection failed. Please verify the sync server is running at the configured URL, or turn OFF Local Server Sync to register locally."
                                    }
                                } else {
                                    val success = dbHelper.registerUser(email, name, phone, password, role)
                                    isLoading = false
                                    if (success) {
                                        val user = dbHelper.loginUser(email, password)
                                        if (user != null) {
                                            onLoginSuccess(user.id, user.role)
                                        } else {
                                            errorMessage = "Registration completed. Please try logging in."
                                        }
                                    } else {
                                        errorMessage = "Registration failed. Email may already exist."
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    enabled = !isLoading,
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(BluePrimary, BlueSecondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isLoginMode) "Log In" else "Create Account",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
