package com.example.helpeez

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.helpeez.ui.screens.LoginScreen
import com.example.helpeez.ui.screens.HomeScreen

@Composable
fun MainNavigation() {
  // Start with Login screen
  val backStack = rememberNavBackStack(Login)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = entryProvider {
      entry<Login> {
        LoginScreen(
          onLoginSuccess = { userId, role ->
            backStack.add(Home(userId = userId, role = role))
          },
          modifier = Modifier.fillMaxSize()
        )
      }
      entry<Home> { key ->
        HomeScreen(
          userId = key.userId,
          role = key.role,
          onLogout = {
            backStack.add(Login)
          },
          modifier = Modifier.fillMaxSize()
        )
      }
    },
  )
}
