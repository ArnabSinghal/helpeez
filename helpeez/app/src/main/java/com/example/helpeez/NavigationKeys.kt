package com.example.helpeez

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Login : NavKey
@Serializable data class Home(val userId: Int, val role: String) : NavKey
