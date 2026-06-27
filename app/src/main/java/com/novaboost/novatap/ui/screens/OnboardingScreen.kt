package com.novaboost.novatap.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaboost.novatap.ui.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }
    val isRu = viewModel.selectedLanguage == "ru"

    var showExplanationDialog by remember { mutableStateOf(false) }
    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }

    val openSettingsAction = {
        try {
            android.util.Log.d("NovaTapOnboarding", "[Permission] Trying to open application notification settings.")
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("NovaTapOnboarding", "[Permission] Failed to open notification settings via main intent. Fallback to Details UI.", e)
            viewModel.logException(e, "OnboardingOpenNotificationSettings")
            try {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                android.util.Log.e("NovaTapOnboarding", "[Permission] Critical fallback list trigger error.", ex)
                viewModel.logException(ex, "OnboardingOpenNotificationSettingsFallback")
            }
        }
    }

    // Launcher for Notification Permission req (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("NovaTapOnboarding", "[Permission] Launcher callback: isGranted = $isGranted")
        viewModel.isNotificationGranted = isGranted

        // Always set the asked flag once we trigger the system prompt
        val sharedPrefs = context.getSharedPreferences("novatap_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("has_asked_notification", true).apply()

        if (isGranted) {
            android.util.Log.d("NovaTapOnboarding", "[Permission] Notification permission granted successfully!")
            onFinish()
        } else {
            // Check rationale immediately following the deny action
            val act = context as? android.app.Activity
            val currRationale = if (act != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    act,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                false
            }
            android.util.Log.d("NovaTapOnboarding", "[Permission] Post-deny checking rationale: $currRationale")
            if (currRationale) {
                // Denied once (not permanently) -> we can show explanation dialog with a try again button
                showExplanationDialog = true
            } else {
                // Permanently Denied (no rationale allowed, don't ask again)
                showPermanentlyDeniedDialog = true
            }
        }
    }

    // Auto check loop in background to automatically detect user return with granted permissions
    LaunchedEffect(Unit) {
        while (currentStep <= 4) {
            try {
                viewModel.checkAllPermissions(context)
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.logException(e, "OnboardingScreenCheckLoop")
            }
            delay(1000)
        }
    }

    // Advanced-step synchronization on permission collection change
    LaunchedEffect(viewModel.isAccessibilityGranted, viewModel.isOverlayGranted, viewModel.isBatteryExempted, viewModel.isNotificationGranted) {
        if (!viewModel.isAccessibilityGranted) {
            currentStep = 1
        } else if (!viewModel.isOverlayGranted) {
            currentStep = 2
        } else if (!viewModel.isBatteryExempted) {
            currentStep = 3
        } else if (!viewModel.isNotificationGranted) {
            currentStep = 4
        } else {
            onFinish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper section: Title & Step tracker indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = if (isRu) "Мастер Настройки" else "Setup Wizard",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isRu) "Шаг $currentStep из 4" else "Step $currentStep of 4",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Beautiful linear step tracker bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..4) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (i <= currentStep) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                            )
                    )
                }
            }
        }

        // Center card section: Detailed Step instructions and button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp)
        ) {
            when (currentStep) {
                1 -> {
                    StepLayout(
                        icon = Icons.Default.Accessibility,
                        title = if (isRu) "Шаг 1: Специальные возможности" else "Step 1: Accessibility Service",
                        explanation = if (isRu) {
                            "NovaTap требуется подключение службы специальных возможностей, чтобы автоматически выполнять касания и свайпы на экране. Мы никогда не читаем ваши конфиденциальные данные."
                        } else {
                            "NovaTap requires the Accessibility Service to simulate touch gestures programmatically on your screen. No personal data is read or collected."
                        },
                        buttonText = if (isRu) "Включить службу в настройках" else "Open Accessibility Settings",
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                viewModel.logException(e, "OnboardingOpenAccessibility")
                            }
                        }
                    )
                }
                2 -> {
                    StepLayout(
                        icon = Icons.Default.FlipToFront,
                        title = if (isRu) "Шаг 2: Поверх других приложений" else "Step 2: Overlay Permission",
                        explanation = if (isRu) {
                            "Для отображения плавающей панели управления поверх других игр и приложений необходимо предоставить доступ к рисованию поверх окон."
                        } else {
                            "To show float control panel widget controllers on top of active games, please toggle Draw Over Other Apps permission."
                        },
                        buttonText = if (isRu) "Разрешить поверх приложений" else "Open Overlay Settings",
                        onClick = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                viewModel.logException(e, "OnboardingOpenOverlay")
                            }
                        }
                    )
                }
                3 -> {
                    StepLayout(
                        icon = Icons.Default.BatterySaver,
                        title = if (isRu) "Шаг 3: Оптимизация батареи" else "Step 3: Disable Battery Optimization",
                        explanation = if (isRu) {
                            "Для бесперебойной работы службы автокликера в фоновом режиме отключите ограничения энергосбережения для приложения NovaTap."
                        } else {
                            "To prevent background automation clicks from freezing or getting suspended contextually, exclude NovaTap from power restrictions."
                        },
                        buttonText = if (isRu) "Отключить ограничения батареи" else "Open Battery Settings",
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                viewModel.logException(e, "OnboardingOpenBattery")
                            }
                        }
                    )
                }
                4 -> {
                    StepLayout(
                        icon = Icons.Default.NotificationsActive,
                        title = if (isRu) "Шаг 4: Разрешение на уведомления" else "Step 4: Notification Permission",
                        explanation = if (isRu) {
                            "Нам необходимо разрешение на показ уведомлений, чтобы отображать статус выполнения автоматизации в статус-баре."
                        } else {
                            "To present status bar state and control overlay updates, please grant Notification permission."
                        },
                        buttonText = if (isRu) "Предоставить доступ к уведомлениям" else "Grant Notification Permission",
                        onClick = {
                            android.util.Log.d("NovaTapOnboarding", "[Permission] Button clicked on step 4. Device SDK = ${android.os.Build.VERSION.SDK_INT}")
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                android.util.Log.d("NovaTapOnboarding", "[Permission] Current POST_NOTIFICATIONS grant status: $hasPermission")
                                if (hasPermission) {
                                    android.util.Log.d("NovaTapOnboarding", "[Permission] Permission already granted, setting as complete.")
                                    viewModel.isNotificationGranted = true
                                    onFinish()
                                } else {
                                    val sharedPrefs = context.getSharedPreferences("novatap_prefs", Context.MODE_PRIVATE)
                                    val hasAskedOnce = sharedPrefs.getBoolean("has_asked_notification", false)
                                    
                                    val act = context as? android.app.Activity
                                    val currRationale = if (act != null) {
                                        androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                                            act,
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    } else {
                                        false
                                    }
                                    
                                    android.util.Log.d("NovaTapOnboarding", "[Permission] Click decision. hasAskedOnce = $hasAskedOnce, shouldShowRationale = $currRationale")
                                    if (hasAskedOnce && !currRationale) {
                                        android.util.Log.d("NovaTapOnboarding", "[Permission] Permanently Denied. Showing helper dialog.")
                                        showPermanentlyDeniedDialog = true
                                    } else {
                                        android.util.Log.d("NovaTapOnboarding", "[Permission] Requesting permission launcher...")
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            } else {
                                android.util.Log.d("NovaTapOnboarding", "[Permission] OS SDK < 33, auto-granting & finishing.")
                                viewModel.isNotificationGranted = true
                                onFinish()
                            }
                        }
                    )
                }
            }
        }

        // Footer: Brand name and safety claim
        Text(
            text = if (isRu) "Защищено технологией NovaTap Human Touch Engine" else "Powered by NovaTap Human Touch Engine",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = {
                Text(
                    text = if (isRu) "Требуется разрешение" else "Permission Required",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = if (isRu) {
                        "NovaTap отправляет уведомления для отображения панели управления и состояния работы автокликера. Пожалуйста, предоставьте доступ, чтобы гарантировать точность и стабильность работы приложения."
                    } else {
                        "NovaTap sends notifications to maintain active control floating overlays, background execution states, and update tick controllers safely. Please allow notifications."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExplanationDialog = false
                        android.util.Log.d("NovaTapOnboarding", "[Permission] Retrying request from explanation dialog.")
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isRu) "Предоставить" else "Grant")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExplanationDialog = false }
                ) {
                    Text(if (isRu) "Позже" else "Later")
                }
            }
        )
    }

    if (showPermanentlyDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermanentlyDeniedDialog = false },
            title = {
                Text(
                    text = if (isRu) "Доступ заблокирован" else "Permission Permanently Denied",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = if (isRu) {
                        "Похоже, вы заблокировали показ уведомлений для NovaTap. Пожалуйста, откройте настройки приложения и вручную разрешите уведомления, чтобы продолжить."
                    } else {
                        "It looks like you have permanently disabled notifications for NovaTap. To enable background overlays and execution alerts, please open app settings and toggle Notifications."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermanentlyDeniedDialog = false
                        openSettingsAction()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isRu) "Открыть настройки" else "Open Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermanentlyDeniedDialog = false }
                ) {
                    Text(if (isRu) "Отмена" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun StepLayout(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    explanation: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("onboarding_grant_action"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = buttonText.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
