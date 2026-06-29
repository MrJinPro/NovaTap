package com.novaboost.novatap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.novaboost.novatap.data.model.Preset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaboost.novatap.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaTapScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isRu = viewModel.selectedLanguage == "ru"

    var bActivePresetName by remember { mutableStateOf("My Area Preset") }
    var intervalMs by remember { mutableStateOf("400") }
    var holdMs by remember { mutableStateOf("40") }
    var repeats by remember { mutableStateOf("100") }
    var stopConditionType by remember { mutableStateOf("infinite") } // "infinite", "duration", "clicks"
    var stopDurationAmount by remember { mutableStateOf("10") }
    var stopDurationUnit by remember { mutableStateOf("seconds") } // "seconds", "minutes", "hours"
    var distributionMode by remember { mutableStateOf("balanced_random") } // "true_random", "center_weighted", "balanced_random"
    var isDrawingAllowed by remember { mutableStateOf(true) } // DrawAllowed vs DrawBlocked
    var isDrawingRect by remember { mutableStateOf(true) } // Rect vs Circle shapes

    LaunchedEffect(viewModel.activeAreaTapPreset) {
        val preset = viewModel.activeAreaTapPreset
        bActivePresetName = preset.name
        intervalMs = preset.intervalMs.toString()
        holdMs = preset.holdMs.toString()
        repeats = preset.repeatCount.toString()
        stopConditionType = preset.stopConditionType
        stopDurationAmount = preset.stopDurationAmount.toString()
        stopDurationUnit = preset.stopDurationUnit
        distributionMode = preset.mode
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Zones™") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isRu) "Профи" else "Expert",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Switch(
                            checked = viewModel.isExpertMode,
                            onCheckedChange = { viewModel.isExpertMode = it },
                            modifier = Modifier.testTag("expert_mode_switch")
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Area automation overview card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "AREA AUTOMATION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isRu) "Нарисуй зону. Запусти кликер." else "Draw a zone. Press Start.",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = 21.sp),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isRu) {
                            "Клики распределяются внутри разрешенных зон. Запрещённые зоны остаются недоступными для действий."
                        } else {
                            "Allowed zones receive the generated taps, while blocked zones are excluded from action targets."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ONLY DISPLAY DRAWING CANVAS Grid inside the screen in EXPERT MODE:
            if (viewModel.isExpertMode) {
                Text(
                    text = if (isRu) "Режим эксперта: Инструменты рисования" else "Expert Mode: On-Board Canvas Editor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isDrawingAllowed = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDrawingAllowed) Color(0xFF10B981) else Color.DarkGray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isRu) "Зеленая зона" else "Allowed Slot")
                    }

                    Button(
                        onClick = { isDrawingAllowed = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDrawingAllowed) Color(0xFFEF4444) else Color.DarkGray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isRu) "Красный Блок" else "Blocked Obstacle")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isDrawingRect = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDrawingRect) MaterialTheme.colorScheme.secondary else Color.DarkGray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Rectangle, contentDescription = "Rect")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRu) "Прямоуг" else "Rectangle")
                    }

                    Button(
                        onClick = { isDrawingRect = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDrawingRect) MaterialTheme.colorScheme.secondary else Color.DarkGray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Circle, contentDescription = "Circle")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRu) "Круглая" else "Circle")
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(280.dp)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .pointerInput(isDrawingAllowed, isDrawingRect) {
                            detectTapGestures { offset ->
                                val canvasWidth = size.width.toFloat()
                                val canvasHeight = size.height.toFloat()
                                val displayMetrics = context.resources.displayMetrics
                                val screenWidth = displayMetrics.widthPixels.toFloat()
                                val screenHeight = displayMetrics.heightPixels.toFloat()
                                
                                val realX = offset.x * (screenWidth / canvasWidth)
                                val realY = offset.y * (screenHeight / canvasHeight)
                                viewModel.addAreaTapZone(isDrawingAllowed, isDrawingRect, realX, realY)
                            }
                        }
                ) {
                    val displayMetrics = context.resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels.toFloat()
                    val screenHeight = displayMetrics.heightPixels.toFloat()
                    val density = displayMetrics.density
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val scaleX = canvasWidth / screenWidth
                        val scaleY = canvasHeight / screenHeight

                        val spacing = 20f
                        for (x in 0..canvasWidth.toInt() step spacing.toInt()) {
                            drawLine(Color.White.copy(alpha = 0.04f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), canvasHeight))
                        }
                        for (y in 0..canvasHeight.toInt() step spacing.toInt()) {
                            drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, y.toFloat()), Offset(canvasWidth, y.toFloat()))
                        }

                        viewModel.areaTapZones.forEach { zone ->
                            val color = if (zone.isAllowed) Color(0xFF10B981) else Color(0xFFEF4444)
                            val drawX = zone.x * scaleX
                            val drawY = zone.y * scaleY
                            val drawW = (zone.width * density) * scaleX
                            val drawH = (zone.height * density) * scaleY
                            val drawRadius = (zone.radius * density) * scaleX

                            if (zone.isRect) {
                                drawRect(
                                    color = color.copy(alpha = 0.2f),
                                    topLeft = Offset(drawX - drawW / 2f, drawY - drawH / 2f),
                                    size = Size(drawW, drawH)
                                )
                                drawRect(
                                    color = color,
                                    topLeft = Offset(drawX - drawW / 2f, drawY - drawH / 2f),
                                    size = Size(drawW, drawH),
                                    style = Stroke(3f)
                                )
                            } else {
                                drawCircle(
                                    color = color.copy(alpha = 0.2f),
                                    center = Offset(drawX, drawY),
                                    radius = drawRadius
                                )
                                drawCircle(
                                    color = color,
                                    center = Offset(drawX, drawY),
                                    radius = drawRadius,
                                    style = Stroke(3f)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isRu) "Зон: ${viewModel.areaTapZones.size}/10" else "Zones: ${viewModel.areaTapZones.size}/10",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )

                    IconButton(
                        onClick = { viewModel.clearAreaZones() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // RANDOMIZATION MODE Toggles
            Text(
                text = if (isRu) "Режим распределения " else "Randomization Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("true_random", "center_weighted", "balanced_random").forEach { mode ->
                    val isSelected = distributionMode == mode
                    val modeTitle = when (mode) {
                        "true_random" -> if (isRu) "Хаос" else "True Chaos"
                        "center_weighted" -> if (isRu) "Эпицентр" else "Center Weighted"
                        else -> if (isRu) "Сбалансир" else "Balanced"
                    }

                    Button(
                        onClick = { distributionMode = mode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = modeTitle, fontSize = 10.sp, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            // TIMINGS
            Text(
                text = if (isRu) "Временные настройки зон" else "Latency Configurations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = intervalMs,
                onValueChange = { intervalMs = it },
                label = { Text(if (isRu) "Паза зон (мс)" else "Zones Pause (ms)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Universal stop condition configuration (all modes)
            StopConditionSelector(
                isRu = isRu,
                stopConditionType = stopConditionType,
                onStopConditionTypeChange = { stopConditionType = it },
                stopDurationAmount = stopDurationAmount,
                onStopDurationAmountChange = { stopDurationAmount = it },
                stopDurationUnit = stopDurationUnit,
                onStopDurationUnitChange = { stopDurationUnit = it },
                repeats = repeats,
                onRepeatsChange = { repeats = it }
            )

            // IF EXPERT DB / METADATA Saving lists
            if (viewModel.isExpertMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = if (isRu) "Запомнить конфигурацию зон" else "Preset Name to Save", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = bActivePresetName,
                            onValueChange = { bActivePresetName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val activeVal = viewModel.activeAreaTapPreset.copy(
                                    name = bActivePresetName,
                                    intervalMs = intervalMs.toLongOrNull()?.coerceAtLeast(10) ?: 400,
                                    holdMs = holdMs.toLongOrNull()?.coerceAtLeast(10) ?: 40,
                                    repeatCount = repeats.toIntOrNull() ?: 0,
                                    stopConditionType = stopConditionType,
                                    stopDurationAmount = stopDurationAmount.toLongOrNull() ?: 10L,
                                    stopDurationUnit = stopDurationUnit,
                                    mode = distributionMode
                                )
                                viewModel.activeAreaTapPreset = activeVal
                                viewModel.savePreset(bActivePresetName, "area")
                                Toast.makeText(context, if (isRu) "Зона сохранена!" else "Zone database committed!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRu) "Сохранить карты зон" else "Save Zones Blueprint")
                        }
                    }
                }
            }

            if (!viewModel.isAdFreeUser) {
                com.novaboost.novatap.ui.components.AdmobBanner(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // CTA TRIGGER
            val isActive = viewModel.isAutomationActive
            Button(
                onClick = {
                    val finalInt = intervalMs.toLongOrNull()?.coerceAtLeast(10) ?: 400
                    val finalHold = holdMs.toLongOrNull()?.coerceAtLeast(10) ?: 40L

                    viewModel.activeAreaTapPreset = Preset(
                        name = bActivePresetName,
                        type = "area",
                        intervalMs = finalInt,
                        holdMs = finalHold,
                        repeatCount = repeats.toIntOrNull() ?: 0,
                        stopConditionType = stopConditionType,
                        stopDurationAmount = stopDurationAmount.toLongOrNull() ?: 10L,
                        stopDurationUnit = stopDurationUnit,
                        mode = distributionMode,
                        pointsJson = "",
                        zonesJson = "",
                        humanTouchEnabled = true
                    )

                    if (viewModel.isOverlayWorkspaceActive) {
                        viewModel.stopOverlayWorkspace(context)
                    } else if (viewModel.isExpertMode) {
                        if (isActive) {
                            viewModel.stopAutomation()
                        } else {
                            viewModel.validateAndTriggerAutomation("area", context) {
                                viewModel.startAreaTapAutomation()
                            }
                        }
                    } else {
                        // Normal mode launches the beautiful interactive smart zones overlay workspace!
                        viewModel.startOverlayWorkspace(context, "area")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_area_tap_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isOverlayWorkspaceActive) MaterialTheme.colorScheme.error
                                     else if (isActive && viewModel.isExpertMode) MaterialTheme.colorScheme.error
                                     else Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (viewModel.isOverlayWorkspaceActive) Icons.Default.Stop
                                      else if (viewModel.isExpertMode && isActive) Icons.Default.Stop
                                      else Icons.Default.FilterCenterFocus,
                        contentDescription = "Trigger"
                    )
                    Text(
                        text = if (viewModel.isOverlayWorkspaceActive) {
                            if (isRu) "ЗАКРЫТЬ ПАНЕЛЬ УПРАВЛЕНИЯ" else "STOP OVERLAY WORKSPACE"
                        } else if (viewModel.isExpertMode) {
                            if (isActive) (if (isRu) "ОСТАНОВИТЬ СЛУЧАЙНЫЙ КЛИК" else "STOP SMART ZONES")
                            else (if (isRu) "ЗАПУСТИТЬ ЗОНЫ В ФОНЕ" else "START SMART ZONES IN BG")
                        } else {
                            if (isRu) "ВЫБРАТЬ РЕЖИМ (ОТКРЫТЬ ПАНЕЛЬ)" else "SELECT MODE (LAUNCH OVERLAY)"
                        },
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            val logText by viewModel.executionLog.collectAsState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                Text(
                    text = logText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Green,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}
