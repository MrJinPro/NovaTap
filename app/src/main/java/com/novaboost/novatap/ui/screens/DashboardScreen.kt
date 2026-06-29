package com.novaboost.novatap.ui.screens

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
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaboost.novatap.ui.theme.*
import com.novaboost.novatap.ui.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val todayUsage by viewModel.todayUsageActions.collectAsState()
    val remaining = viewModel.getRemainingActions()
    val isPremium = viewModel.isAdFreeUser

    val isRu = viewModel.selectedLanguage == "ru"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sleek Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "NovaTap",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(CyberBlue, ElectricPurple)
                        )
                    )
                )
                Text(
                    text = if (isRu) "СИМУЛЯТОР НАЖАТИЙ v4.0" else "HUMAN TOUCH ENGINE v4.0",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = TextMuted
                )
            }

            // Remove Ads badge/button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable {
                        val activity = context as? android.app.Activity
                        if (activity != null) {
                            viewModel.triggerPremiumPurchase(activity)
                        } else {
                            viewModel.removeAdsService()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Glowing status dot
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isPremium) GlowGreen.copy(alpha = 0.4f)
                                    else ElectricPurple.copy(alpha = 0.4f)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isPremium) GlowGreen
                                    else ElectricPurple
                                )
                        )
                    }
                    Text(
                        text = if (isPremium) (if (isRu) "БЕЗ РЕКЛАМЫ" else "PREMIUM ACTIVE") else (if (isRu) "УБРАТЬ РЕКЛАМУ" else "REMOVE ADS"),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (isPremium) GlowGreen else Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // 2. Today's Usage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, Color(0x0DFFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CardDarkBg, SpaceDarkBg)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = if (isRu) "Использовано сегодня" else "Today's Usage",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                ),
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = String.format("%,d", todayUsage),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Light,
                                        letterSpacing = (-1).sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = if (isPremium) "/ ∞" else "/ 50K",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        // Icon container
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberBlue.copy(alpha = 0.1f))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Usage Status",
                                tint = CyberBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Bar
                    val progressPct = if (isPremium) 1.0f else (todayUsage.toFloat() / 50000f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF111827))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPct)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(CyberBlue, ElectricPurple)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val reachedSoon = !isPremium && viewModel.getRemainingActions() < 10000
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPremium) {
                                if (isRu) "БЕЗЛИМИТНЫЙ ДОСТУП АКТИВЕН" else "UNLIMITED INTERACTIONS ACTIVE"
                            } else if (reachedSoon) {
                                if (isRu) "ДНЕВНОЙ ЛИМИТ ПОЧТИ ИСЧЕРПАН" else "DAILY LIMIT REACHED SOON"
                            } else {
                                if (isRu) "СТАБИЛЬНОСТЬ СИСТЕМЫ ОТЛИЧНАЯ" else "LIMIT STABILITY CODES OK"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (reachedSoon) AlertRed else Color.White.copy(alpha = 0.4f)
                        )

                        if (!isPremium) {
                            Text(
                                text = if (isRu) "+10К ЗА ПРОСМОТР" else "+10K WITH AD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = CyberBlue,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { viewModel.rewardUserByAd(context) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Automation Modules Title
        Text(
            text = if (isRu) "ИНСТРУМЕНТЫ АРХИТЕКТУРЫ" else "AUTOMATION MODULES",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 1.1.sp
        )

        // 4. Grid of Module Cards
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Single Tap
                ModuleGridCard(
                    title = if (isRu) "Одиночное" else "Single Tap",
                    subtitle = if (isRu) "1 точка" else "1 Static point",
                    icon = Icons.Default.AdsClick,
                    accentColor = CyberBlue,
                    isFlagship = false,
                    isRu = isRu,
                    onClick = { onNavigate("single_tap") },
                    modifier = Modifier.weight(1f)
                )

                // Card 2: Multi Tap
                ModuleGridCard(
                    title = if (isRu) "Мульти" else "Multi Tap",
                    subtitle = if (isRu) "До 20 точек" else "Up to 20 points",
                    icon = Icons.Default.Stream,
                    accentColor = ElectricPurple,
                    isFlagship = false,
                    isRu = isRu,
                    onClick = { onNavigate("multi_tap") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 3: Area Tap (Flagship!)
                ModuleGridCard(
                    title = if (isRu) "Нажатие Зон" else "Area Tap",
                    subtitle = if (isRu) "ФЛАГМАНСКИЙ" else "FLAGSHIP MODULE",
                    icon = Icons.Default.FilterCenterFocus,
                    accentColor = CyberBlue,
                    isFlagship = true,
                    isRu = isRu,
                    onClick = { onNavigate("area_tap") },
                    modifier = Modifier.weight(1f)
                )

                // Card 4: Swipe
                ModuleGridCard(
                    title = if (isRu) "Свайп" else "Swipe",
                    subtitle = if (isRu) "Жесты" else "Custom paths",
                    icon = Icons.Default.Swipe,
                    accentColor = Color(0xFFFFB74D),
                    isFlagship = false,
                    isRu = isRu,
                    onClick = { onNavigate("swipe") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Card 5: Scenario Builder (Full-width for balance)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("scenarios") },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardDarkBg.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color(0x0DFFFFFF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEC407A).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayForWork,
                                contentDescription = null,
                                tint = Color(0xFFEC407A),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isRu) "Сценарии кликов" else "Scenario Chains",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (isRu) "Конструктор цепочки действий" else "Chains of actions (Tap, Wait, Swipe)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 5. Quick Start Panel Button
        Button(
            onClick = { onNavigate("onboarding") },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("quick_start_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = SpaceDarkBg
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isRu) "БЫСТРЫЙ СТАРТ" else "QUICK START",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Quick Start",
                    tint = SpaceDarkBg,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 6. AdMob Banner Simulator
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color(0x1FFFFFFF)), RoundedCornerShape(16.dp))
                    .background(Color(0x0AFFFFFF))
                    .clickable { viewModel.rewardUserByAd(context) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Ads",
                        tint = CyberBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isRu) "Нажмите, чтобы бесплатно получить +10k кликов" else "Click to claim free +10k clicks",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = CyberBlue
                    )
                }
            }
        }
    }
}

@Composable
fun ModuleGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isFlagship: Boolean,
    isRu: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBackgroundModifier = if (isFlagship) {
        Modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    CyberBlue.copy(alpha = 0.1f),
                    ElectricPurple.copy(alpha = 0.1f)
                )
            )
        )
    } else {
        Modifier.background(CardDarkBg.copy(alpha = 0.5f))
    }

    val borderStroke = if (isFlagship) {
        BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.3f))
    } else {
        BorderStroke(1.dp, Color(0x0DFFFFFF))
    }

    Card(
        modifier = modifier
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = borderStroke
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(cardBackgroundModifier)
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (isFlagship) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(CyberBlue, ElectricPurple)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isFlagship) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isFlagship) CyberBlue else TextMuted
                    )
                }
            }
        }
    }
}
