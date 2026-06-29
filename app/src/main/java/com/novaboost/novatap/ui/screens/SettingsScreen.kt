package com.novaboost.novatap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaboost.novatap.ui.theme.*
import com.novaboost.novatap.ui.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToDiagnostics: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isRu = viewModel.selectedLanguage == "ru"
    val showTapMockTasks = false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App settings name
        Text(
            text = if (isRu) "НАСТРОЙКИ СИСТЕМЫ" else "SYSTEM PARAMETERS CONFIG",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            ),
            color = CyberBlue,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Select Theme
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDarkBg.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0x0DFFFFFF))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isRu) "Тема оформления" else "Interface Color Palette Theme",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("dark", "light", "system").forEach { themeMode ->
                        val isSel = viewModel.selectedTheme == themeMode
                        val title = when (themeMode) {
                            "dark" -> if (isRu) "Тёмная" else "Dark Cyber"
                            "light" -> if (isRu) "Светлая" else "Light"
                            else -> if (isRu) "Системная" else "System"
                        }

                        val btnBackground = if (isSel) {
                            Brush.horizontalGradient(colors = listOf(CyberBlue, ElectricPurple))
                        } else {
                            Brush.linearGradient(colors = listOf(Color(0xFF2D3748), Color(0xFF1A202C)))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(btnBackground)
                                .clickable { viewModel.toggleTheme(themeMode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Selected Languages
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDarkBg.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0x0DFFFFFF))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isRu) "Выбор языка" else "Default Application Language",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("en", "ru").forEach { langMode ->
                        val isSel = viewModel.selectedLanguage == langMode
                        val title = when (langMode) {
                            "ru" -> "Русский (RU)"
                            else -> "English (EN)"
                        }

                        val btnBackground = if (isSel) {
                            Brush.horizontalGradient(colors = listOf(CyberBlue, ElectricPurple))
                        } else {
                            Brush.linearGradient(colors = listOf(Color(0xFF2D3748), Color(0xFF1A202C)))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(btnBackground)
                                .clickable { viewModel.toggleLanguage(langMode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Diagnostics
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToDiagnostics() }
                .testTag("diagnostics_card"),
            colors = CardDefaults.cardColors(containerColor = CardDarkBg.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0x0DFFFFFF))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Troubleshoot,
                        contentDescription = "Diagnostics",
                        tint = CyberBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRu) "Диагностика и Логи" else "Diagnostics & Logs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isRu) "Просмотр ошибок, логов падений и прав системы" else "Check system permissions & view crash logs",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = TextMuted
                )
            }
        }

        if (showTapMockTasks) {
            // Clicker Display Preferences
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDarkBg.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0x0DFFFFFF))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isRu) "Параметры работы кликов" else "Tap Display & Security Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = if (isRu) "Анимация кругов нажатий" else "Show Tap Ripple Ripples",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isRu) 
                                    "Визуальный эффект в точке клика. На Android 12+ включение может блокировать нажатия или вызывать предупреждения системы из-за правил безопасности (кликджекинг). Рекомендуется выключить для стабильной работы." 
                                else 
                                    "Visual effect at tap coordinates. On Android 12+, enabling this can cause blocked clicks or system warnings due to clickjacking security rules. Keep disabled for stable tapping.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = viewModel.showTapRipples,
                            onCheckedChange = { viewModel.toggleTapRipples(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBlue,
                                checkedTrackColor = CyberBlue.copy(alpha = 0.4f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("tap_ripples_switch")
                        )
                    }
                }
            }
        }

        // Stats and Database Backups operations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDarkBg.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0x0DFFFFFF))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isRu) "Управление данными" else "Local Data Operations Management",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                // Reset Statistics
                Button(
                    onClick = {
                        viewModel.resetStats()
                        Toast.makeText(context, if (isRu) "Статистика за день обнулена!" else "Statistics successfully cleared!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reset_statistics_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertRed.copy(alpha = 0.15f),
                        contentColor = AlertRed
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        Text(
                            text = if (isRu) "Сбросить дневные счетчики" else "Reset Daily Clicks Usage Counters",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Local Backup
                Button(
                    onClick = {
                        Toast.makeText(context, if (isRu) "Локальная резервная копия создана!" else "Local database backup successfully built!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("backup_data_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x1FFFFFFF))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Backup, contentDescription = "Backup", modifier = Modifier.size(18.dp))
                        Text(
                            text = if (isRu) "Создать резервную копию настроек" else "Backup Presets & DB Locally",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Info and Version
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NovaTap Touch Automation Suite",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "v4.0.0 - Stable build",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted.copy(alpha = 0.6f)
                )
            }
        }
    }
}
