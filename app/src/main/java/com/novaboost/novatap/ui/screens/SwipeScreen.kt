package com.novaboost.novatap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.novaboost.novatap.data.model.Preset
import com.novaboost.novatap.data.model.SwipeCoordinates
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaboost.novatap.ui.MainViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isRu = viewModel.selectedLanguage == "ru"

    var bActivePresetName by remember { mutableStateOf("My Swipe Preset") }
    var intervalMs by remember { mutableStateOf("1000") }
    var swipeDurationMs by remember { mutableStateOf("300") }
    var repeats by remember { mutableStateOf("100") }
    var stopConditionType by remember { mutableStateOf("infinite") } // "infinite", "duration", "clicks"
    var stopDurationAmount by remember { mutableStateOf("10") }
    var stopDurationUnit by remember { mutableStateOf("seconds") } // "seconds", "minutes", "hours"
    var pathHumanized by remember { mutableStateOf(true) }

    // Swipe coordinates
    var startX by remember { mutableStateOf(200f) }
    var startY by remember { mutableStateOf(800f) }
    var endX by remember { mutableStateOf(800f) }
    var endY by remember { mutableStateOf(200f) }

    // Init from active state
    LaunchedEffect(viewModel.activeSwipePreset) {
        val preset = viewModel.activeSwipePreset
        bActivePresetName = preset.name
        intervalMs = preset.intervalMs.toString()
        swipeDurationMs = preset.holdMs.toString()
        repeats = preset.repeatCount.toString()
        stopConditionType = preset.stopConditionType
        stopDurationAmount = preset.stopDurationAmount.toString()
        stopDurationUnit = preset.stopDurationUnit
        pathHumanized = preset.humanTouchEnabled

        startX = viewModel.swipeCoordinates.startX
        startY = viewModel.swipeCoordinates.startY
        endX = viewModel.swipeCoordinates.endX
        endY = viewModel.swipeCoordinates.endY
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRu) "Свайп-эффекты" else "Swipe Gestures") },
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
            // Slogan / Explanation
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Swipe,
                        contentDescription = "Swipe",
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = if (isRu) "Линейные прокрутки экрана" else "Linear Screen Swiping Gests",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isRu) {
                                "Задайте время проведения за пальцем, нажмите на кнопку оверлея ниже и расположите узлы начала (S) и конца (E) визуально."
                            } else {
                                "Configure duration and path in dynamic overlay workspace by visually manipulating start (S) and ending (E) handle pegs."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ONLY ENFORCE CANVAS WORKSPACE INSIDE EXPERT MODE SCREEN
            if (viewModel.isExpertMode) {
                Text(
                    text = if (isRu) "Режим эксперта: Интерактивный эскиз" else "Expert Mode: Dynamic Gestures Sketcher",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 40f
                        for (x in 0..size.width.toInt() step step.toInt()) {
                            drawLine(Color.White.copy(alpha = 0.04f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height))
                        }
                        for (y in 0..size.height.toInt() step step.toInt()) {
                            drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()))
                        }

                        val wScale = size.width / 1000f
                        val hScale = size.height / 1000f
                        val ms = Offset(startX * wScale, startY * hScale)
                        val me = Offset(endX * wScale, endY * hScale)

                        drawLine(
                            color = Color(0xFFFFB74D),
                            start = ms,
                            end = me,
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )

                        drawCircle(Color(0xFF10B981), 12f, ms)
                        drawCircle(Color(0xFFEF4444), 12f, me)
                    }

                    Text(
                        text = if (isRu) "[Начало] Зеленый -> [Конец] Красный" else "[Start] Green Node -> [End] Red Node",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                    )
                }

                Text(
                    text = if (isRu) "Режим эксперта: Ручные координаты" else "Expert Mode: Precision Coordinates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startX.roundToInt().toString(),
                        onValueChange = { startX = it.toFloatOrNull() ?: 200f },
                        label = { Text("Start X (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = startY.roundToInt().toString(),
                        onValueChange = { startY = it.toFloatOrNull() ?: 800f },
                        label = { Text("Start Y (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = endX.roundToInt().toString(),
                        onValueChange = { endX = it.toFloatOrNull() ?: 800f },
                        label = { Text("End X (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = endY.roundToInt().toString(),
                        onValueChange = { endY = it.toFloatOrNull() ?: 200f },
                        label = { Text("End Y (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // TIMINGS
            Text(
                text = if (isRu) "Временные настройки жеста" else "Timing Configurations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = intervalMs,
                    onValueChange = { intervalMs = it },
                    label = { Text(if (isRu) "Пауза между (мс)" else "Pause Interval (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = swipeDurationMs,
                    onValueChange = { swipeDurationMs = it },
                    label = { Text(if (isRu) "Длительность (мс)" else "Swipe Duration (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

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

            // DB SAVING (Expert)
            if (viewModel.isExpertMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = if (isRu) "Запомнить координатный жест" else "Save Preset Name", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = bActivePresetName,
                            onValueChange = { bActivePresetName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                viewModel.swipeCoordinates = SwipeCoordinates(startX, startY, endX, endY)
                                val activeVal = viewModel.activeSwipePreset.copy(
                                    name = bActivePresetName,
                                    intervalMs = intervalMs.toLongOrNull()?.coerceAtLeast(100) ?: 1000,
                                    holdMs = swipeDurationMs.toLongOrNull()?.coerceAtLeast(100) ?: 300,
                                    repeatCount = repeats.toIntOrNull() ?: 0,
                                    stopConditionType = stopConditionType,
                                    stopDurationAmount = stopDurationAmount.toLongOrNull() ?: 10L,
                                    stopDurationUnit = stopDurationUnit,
                                    humanTouchEnabled = pathHumanized
                                )
                                viewModel.activeSwipePreset = activeVal
                                viewModel.savePreset(bActivePresetName, "swipe")
                                Toast.makeText(context, if (isRu) "Свайп сохранён!" else "Swipe preset committed!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRu) "Записать жест в базу" else "Commit Swipe Preset")
                        }
                    }
                }
            }

            // PRIMARY BIG TRIGGER
            val isActive = viewModel.isAutomationActive
            Button(
                onClick = {
                    val finalInt = intervalMs.toLongOrNull()?.coerceAtLeast(100) ?: 1000
                    val finalSwipeDur = swipeDurationMs.toLongOrNull()?.coerceAtLeast(100) ?: 300

                    viewModel.activeSwipePreset = Preset(
                        name = bActivePresetName,
                        type = "swipe",
                        intervalMs = finalInt,
                        holdMs = finalSwipeDur,
                        repeatCount = repeats.toIntOrNull() ?: 0,
                        stopConditionType = stopConditionType,
                        stopDurationAmount = stopDurationAmount.toLongOrNull() ?: 10L,
                        stopDurationUnit = stopDurationUnit,
                        pointsJson = "",
                        zonesJson = "",
                        humanTouchEnabled = pathHumanized
                    )

                    viewModel.swipeCoordinates = SwipeCoordinates(startX, startY, endX, endY)

                    if (viewModel.isOverlayWorkspaceActive) {
                        viewModel.stopOverlayWorkspace(context)
                    } else if (viewModel.isExpertMode) {
                        if (isActive) {
                            viewModel.stopAutomation()
                        } else {
                            viewModel.validateAndTriggerAutomation("swipe", context) {
                                viewModel.startSwipeAutomation()
                            }
                        }
                    } else {
                        // Normal mode minimizes the app and places the beautiful interactive Swipe vectors handles on screen!
                        viewModel.startOverlayWorkspace(context, "swipe")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_swipe_button"),
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
                                      else Icons.Default.Swipe,
                        contentDescription = "Trigger"
                    )
                    Text(
                        text = if (viewModel.isOverlayWorkspaceActive) {
                            if (isRu) "ЗАКРЫТЬ ПАНЕЛЬ УПРАВЛЕНИЯ" else "STOP OVERLAY WORKSPACE"
                        } else if (viewModel.isExpertMode) {
                            if (isActive) (if (isRu) "ОСТАНОВИТЬ СВАЙП" else "STOP BACKGROUND SWIPING")
                            else (if (isRu) "СТАРТ ЖЕСТА В ФОНЕ" else "START SWIPING IN BG")
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
