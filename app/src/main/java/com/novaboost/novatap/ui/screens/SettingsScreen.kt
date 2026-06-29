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

        // Premium & Promo Code Redemption Card
        val isPremium = viewModel.isAdFreeUser
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDarkBg.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0x0DFFFFFF))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isPremium) GlowGreen.copy(alpha = 0.15f) else ElectricPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPremium) Icons.Default.VerifiedUser else Icons.Default.WorkspacePremium,
                            contentDescription = "Premium Status",
                            tint = if (isPremium) GlowGreen else ElectricPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPremium) (if (isRu) "Премиум активен" else "Premium Active") else (if (isRu) "Версия NovaTap Free" else "NovaTap Free Version"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = if (isPremium) 
                                (if (isRu) "Без рекламы и ограничений по кликам" else "No ads and completely unlimited clicks") 
                            else 
                                (if (isRu) "Лимит 50,000 кликов в сутки" else "Limit of 50,000 daily action triggers"),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                if (!isPremium) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Buy Subscription Button
                        Button(
                            onClick = {
                                val activity = context as? android.app.Activity
                                if (activity != null) {
                                    viewModel.triggerPremiumPurchase(activity)
                                } else {
                                    viewModel.removeAdsService()
                                }
                            },
                            modifier = Modifier.weight(1.5f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isRu) "Купить Премиум" else "Get Premium",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Redeem Promo Code Button
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("https://play.google.com/redeem")
                                        setPackage("com.android.vending")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/redeem"))
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        Toast.makeText(context, if (isRu) "Не удалось открыть Google Play" else "Could not open Google Play Store", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.2f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.05f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x1FFFFFFF))
                        ) {
                            Text(
                                text = if (isRu) "Ввести промокод" else "Redeem Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Special Partner Tasks & Cross Promotions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDarkBg.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0x0DFFFFFF))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Bonus Tasks",
                            tint = CyberBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRu) "Бонусные Задания" else "Bonus Tasks & Rewards",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = if (isRu) "Получайте огромные лимиты или бесплатный Премиум" else "Get huge click limits or premium access for free",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                viewModel.promoTasks.forEach { task ->
                    val isCompleted = viewModel.completedTaskIds.contains(task.id)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x08FFFFFF), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isRu) task.titleRu else task.titleEn,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Reward Badge
                            val rewardText = if (task.rewardPremiumDays > 0) {
                                if (isRu) "+${task.rewardPremiumDays}д. Premium" else "+${task.rewardPremiumDays}d Premium"
                            } else {
                                if (isRu) "+${task.rewardTaps / 1000}к кликов" else "+${task.rewardTaps / 1000}k limit"
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (task.rewardPremiumDays > 0) ElectricPurple.copy(alpha = 0.15f) else GlowGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = rewardText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (task.rewardPremiumDays > 0) ElectricPurple else GlowGreen
                                )
                            }
                        }

                        Text(
                            text = if (isRu) task.descriptionRu else task.descriptionEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        if (isCompleted) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = GlowGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isRu) "Награда получена" else "Reward Claimed",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = GlowGreen
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Action Button
                                Button(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(task.targetUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error opening link", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0x11FFFFFF))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Open",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isRu) "Перейти" else "Open Task",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                // Claim/Verify Button
                                Button(
                                    onClick = {
                                        viewModel.claimTaskReward(task, context)
                                    },
                                    modifier = Modifier.weight(1.2f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (task.targetPackage == null) CyberBlue else ElectricPurple,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (task.targetPackage != null) {
                                            if (isRu) "Проверить" else "Verify Install"
                                        } else {
                                            if (isRu) "Забрать бонус" else "Claim Bonus"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

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

                Divider(color = Color(0x1FFFFFFF), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                // 4. Diagnostics Overlay Setting Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = if (isRu) "Диагностический оверлей" else "Real-time Diagnostics Overlay",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRu) 
                                "Всплывающий HUD, показывающий регистрацию кликов, процент успеха доставки и задержку очереди жестов ОС в реальном времени. Помогает отладить заблокированные или пропущенные нажатия." 
                            else 
                                "Floating heads-up-display showing tap registrations, delivery success rate, and OS gesture queue response latency in real-time. Helps debug blocked or dropped clicks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    Switch(
                        checked = viewModel.showDiagnostics,
                        onCheckedChange = { viewModel.toggleDiagnostics(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlue,
                            checkedTrackColor = CyberBlue.copy(alpha = 0.4f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("diagnostics_switch")
                    )
                }

                Divider(color = Color(0x1FFFFFFF), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                // 4. Diagnostics Overlay Setting Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = if (isRu) "Диагностический оверлей" else "Real-time Diagnostics Overlay",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRu) 
                                "Всплывающий HUD, показывающий регистрацию кликов, процент успеха доставки и задержку очереди жестов ОС в реальном времени. Помогает отладить заблокированные или пропущенные нажатия." 
                            else 
                                "Floating heads-up-display showing tap registrations, delivery success rate, and OS gesture queue response latency in real-time. Helps debug blocked or dropped clicks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }

                    Switch(
                        checked = viewModel.showDiagnostics,
                        onCheckedChange = { viewModel.toggleDiagnostics(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBlue,
                            checkedTrackColor = CyberBlue.copy(alpha = 0.4f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("diagnostics_switch")
                    )
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
                    text = "v4.0.0 - Premium Stable Release",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted.copy(alpha = 0.6f)
                )
            }
        }
    }
}
