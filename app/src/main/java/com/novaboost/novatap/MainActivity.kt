package com.novaboost.novatap

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaboost.novatap.ui.screens.*
import com.novaboost.novatap.ui.theme.NovaTapTheme
import com.novaboost.novatap.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            NovaTapTheme {
                MainLayout(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            viewModel.checkAllPermissions(this)
        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.logException(e, "MainActivityOnResume")
        }
    }
}

@Composable
fun MainLayout(viewModel: MainViewModel) {
    val context = LocalContext.current
    
    // Robust startup checks before opening main app
    val initialScreen = remember { if (viewModel.isStartupSetupWizardNeeded(context)) "onboarding" else "dashboard" }
    var currentScreen by remember { mutableStateOf(initialScreen) }

    // Navigation back-stack logic for subscreen navigations
    val screenHistory = remember { mutableStateListOf(initialScreen) }

    LaunchedEffect(viewModel.isAccessibilityGranted, viewModel.isOverlayGranted, viewModel.isBatteryExempted, viewModel.isNotificationGranted) {
        val needed = !viewModel.isAccessibilityGranted || !viewModel.isOverlayGranted || !viewModel.isBatteryExempted || !viewModel.isNotificationGranted
        if (needed) {
            if (currentScreen != "onboarding") {
                currentScreen = "onboarding"
            }
        } else {
            if (currentScreen == "onboarding") {
                currentScreen = "dashboard"
            }
        }
    }

    val navigateTo: (String) -> Unit = { screen ->
        screenHistory.add(currentScreen)
        currentScreen = screen
    }

    val navigateBack: () -> Unit = {
        if (screenHistory.isNotEmpty()) {
            currentScreen = screenHistory.removeAt(screenHistory.lastIndex)
        } else {
            currentScreen = if (viewModel.isStartupSetupWizardNeeded(context)) "onboarding" else "dashboard"
        }
    }

    val isRu = viewModel.selectedLanguage == "ru"

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            // Render Bottom Bar only when we are in primary dashboard screens
            if (currentScreen == "dashboard" || currentScreen == "help" || currentScreen == "settings") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    // Home
                    NavigationBarItem(
                        selected = currentScreen == "dashboard",
                        onClick = { currentScreen = "dashboard" },
                        icon = { Icon(imageVector = Icons.Default.GridView, contentDescription = "Home") },
                        label = { Text(if (isRu) "Управление" else "Dashboard", fontSize = 11.sp) }
                    )

                    // Help Center
                    NavigationBarItem(
                        selected = currentScreen == "help",
                        onClick = { currentScreen = "help" },
                        icon = { Icon(imageVector = Icons.Default.HelpCenter, contentDescription = "Help") },
                        label = { Text(if (isRu) "Справка" else "Help Center", fontSize = 11.sp) }
                    )

                    // Settings
                    NavigationBarItem(
                        selected = currentScreen == "settings",
                        onClick = { currentScreen = "settings" },
                        icon = { Icon(imageVector = Icons.Default.Tune, contentDescription = "Settings") },
                        label = { Text(if (isRu) "Настройки" else "Settings", fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Simple animations transitions crossfade between screens
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    "dashboard" -> DashboardScreen(viewModel, onNavigate = navigateTo)
                    "single_tap" -> SingleTapScreen(viewModel, onBack = navigateBack)
                    "multi_tap" -> MultiTapScreen(viewModel, onBack = navigateBack)
                    "area_tap" -> AreaTapScreen(viewModel, onBack = navigateBack)
                    "swipe" -> SwipeScreen(viewModel, onBack = navigateBack)
                    "scenarios" -> ScenarioScreen(viewModel, onBack = navigateBack)
                    "help" -> HelpScreen(viewModel)
                    "settings" -> SettingsScreen(viewModel, onNavigateToDiagnostics = { navigateTo("diagnostics") })
                    "diagnostics" -> DiagnosticsScreen(viewModel, onBack = navigateBack)
                    "onboarding" -> OnboardingScreen(viewModel, onFinish = { currentScreen = "dashboard" })
                    else -> DashboardScreen(viewModel, onNavigate = navigateTo)
                }
            }

            // Limit Exceeded Dialog Modal Popup overlay (Strict Monetization Rules)
            if (viewModel.displayLimitDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.displayLimitDialog = false },
                    title = {
                        Text(
                            text = if (isRu) "Дневной лимит кликов исчерпан" else "Daily click limit reached",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    text = {
                        Text(
                            text = if (isRu) {
                                "Вы использовали свободные 50 000 кликов на сегодня. Отключите лимиты навсегда или посмотрите короткое видео, чтобы получить еще +50 000 кликов!"
                            } else {
                                "You have reached your daily limit of 50,000 interactions. To unlock unlimited inputs, remove ads, or watch a video to extend limits by +50,000."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.rewardUserByAd()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.SmartDisplay, contentDescription = "Watch Ad")
                                Text(if (isRu) "Смотреть Рекламу (+50к)" else "Watch Video Ad (+50k)")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                viewModel.removeAdsService()
                                viewModel.displayLimitDialog = false
                            }
                        ) {
                            Text(
                                text = if (isRu) "Купить подписку (Убрать рекламу)" else "Remove Ads Subscription",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                )
            }

            if (viewModel.validationErrorTitle != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissValidationErrorDialog() },
                    title = {
                        Text(
                            text = viewModel.validationErrorTitle ?: "",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    text = {
                        Text(
                            text = viewModel.validationErrorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissValidationErrorDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isRu) "Понятно" else "OK")
                        }
                    }
                )
            }
        }
    }
}
