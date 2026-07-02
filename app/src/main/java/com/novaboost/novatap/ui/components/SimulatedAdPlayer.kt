package com.novaboost.novatap.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaboost.novatap.ui.MainViewModel
import com.novaboost.novatap.ui.theme.*

@Composable
fun SimulatedAdPlayer(viewModel: MainViewModel) {
    if (!viewModel.showSimulatedAd) return

    val isRu = viewModel.selectedLanguage == "ru"
    val isRewarded = viewModel.adType == "rewarded"
    val remainingSec = viewModel.adCountdownSeconds

    // Shimmer effect for video simulation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val backgroundOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerOffset"
    )

    val videoBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF1E1B4B),
            Color(0xFF311042),
            Color(0xFF0F172A)
        ),
        start = androidx.compose.ui.geometry.Offset(backgroundOffset, backgroundOffset),
        end = androidx.compose.ui.geometry.Offset(backgroundOffset + 600f, backgroundOffset + 600f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60F172A)) // Dark dim overlay
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .border(2.dp, CyberBlue, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceDarkBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header (Ad Info)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(CyberBlue.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AD",
                                color = CyberBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = if (isRu) "Google AdMob Спонсор" else "Google AdMob Sponsor",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Skip button or Countdown
                    if (!isRewarded && remainingSec <= 0) {
                        IconButton(
                            onClick = { viewModel.skipOrCloseSimulatedAd() },
                            modifier = Modifier
                                .size(32.dp)
                                .background(CyberBlue, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = SpaceDarkBg,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else if (remainingSec > 0) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .border(1.5.dp, ElectricPurple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$remainingSec",
                                color = ElectricPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Simulated Video Player Canvas / Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(videoBrush)
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isRewarded) Icons.Default.SmartDisplay else Icons.Default.PlayArrow,
                            contentDescription = "Simulated Video",
                            tint = if (isRewarded) ElectricPurple else CyberBlue,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = if (isRewarded) {
                                if (isRu) "Идет показ вознаграждаемой рекламы..." else "Playing Rewarded Ad..."
                            } else {
                                if (isRu) "Показ интерактивной промо-рекламы" else "Interactive Promo Ad Playing"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isRu) "Не закрывайте приложение" else "Do not close the application",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // Footer (Offerings or Reward summary)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isRewarded) {
                            if (isRu) "Награда за просмотр: +10 000 кликов ⚡" else "Reward after video: +10,000 clicks ⚡"
                        } else {
                            if (isRu) "Вы можете пропустить рекламу через 5 секунд" else "You can skip this ad after 5 seconds"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isRewarded) GlowGreen else Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (!isRewarded && remainingSec <= 0) {
                        Button(
                            onClick = { viewModel.skipOrCloseSimulatedAd() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = null, tint = SpaceDarkBg)
                                Text(
                                    text = if (isRu) "Пропустить рекламу" else "Skip Advertisement",
                                    color = SpaceDarkBg,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isRewarded && remainingSec > 0) {
                        Text(
                            text = if (isRu) "Обязательный просмотр 30 секунд для получения бонуса" else "Mandatory 30-second view required to claim reward",
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertRed.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    } else if (isRewarded && remainingSec <= 0) {
                        Button(
                            onClick = { viewModel.skipOrCloseSimulatedAd() },
                            colors = ButtonDefaults.buttonColors(containerColor = GlowGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isRu) "Забрать награду" else "Claim Reward",
                                color = SpaceDarkBg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
