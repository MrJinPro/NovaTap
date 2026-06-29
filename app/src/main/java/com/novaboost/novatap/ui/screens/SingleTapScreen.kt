package com.novaboost.novatap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaboost.novatap.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleTapScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isRu = viewModel.selectedLanguage == "ru"

    // Controller states linked to model preset values
    var presetName by remember { mutableStateOf("My Single Preset") }
    var inputX by remember { mutableStateOf("500") }
    var inputY by remember { mutableStateOf("1000") }
    var intervalMs by remember { mutableStateOf("200") }
    var holdMs by remember { mutableStateOf("40") }
    var repeats by remember { mutableStateOf("100") }
    var stopConditionType by remember { mutableStateOf("infinite") } // "infinite", "duration", "clicks"
    var stopDurationAmount by remember { mutableStateOf("10") }
    var stopDurationUnit by remember { mutableStateOf("seconds") } // "seconds", "minutes", "hours"
    var humanTouch by remember { mutableStateOf(false) }
    var microOffset by remember { mutableStateOf(10f) }
    var randomInterval by remember { mutableStateOf(15f) }
    var randomHold by remember { mutableStateOf(5f) }

    var showSafetyWarning by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.activeSingleTapPreset) {
        val preset = viewModel.activeSingleTapPreset
        presetName = preset.name
        intervalMs = preset.intervalMs.toString()
        holdMs = preset.holdMs.toString()
        repeats = preset.repeatCount.toString()
        stopConditionType = preset.stopConditionType
        stopDurationAmount = preset.stopDurationAmount.toString()
        stopDurationUnit = preset.stopDurationUnit
        humanTouch = preset.humanTouchEnabled
        microOffset = preset.microOffsetPx.toFloat()
        randomInterval = preset.randomIntervalRange.toFloat()
        randomHold = preset.randomHoldRange.toFloat()
        inputX = preset.pointsJson.ifEmpty { "500" }
        inputY = preset.zonesJson.ifEmpty { "1000" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRu) "Одиночное нажатие" else "Single Tap Mode") },
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
            // Slogan / Description
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdsClick,
                        contentDescription = "Tap",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = if (isRu) "Без лишних координат" else "No Coordinates Needed",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isRu) {
                                "Просто задайте интервал задержки, нажмите кнопку ниже и перетащите появившуюся точку кликера в любое место на экране."
                            } else {
                                "Set timings below, click launch, and drag the visual clicker point directly on top of your target app."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // TIMINGS CONFIGURATION (Universal)
            Text(
                text = if (isRu) "1. Настройка задержки" else "1. Timing Configurations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = intervalMs,
                onValueChange = {
                    intervalMs = it
                    val num = it.toLongOrNull() ?: 0
                    showSafetyWarning = num < 40 && num > 0
                },
                label = { Text(if (isRu) "Интервал (мс)" else "Interval (ms)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (showSafetyWarning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (isRu) "Минимальный интервал защиты экрана составляет 40 мс!" else "Minimum safety filter threshold is 40ms!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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

            // HUMAN TOUCH ENGINE
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRu) "Движок Human Touch" else "Human Touch Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRu) "Защита от обнаружения автокликеров" else "Anti-detection micro-variations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }

                        Switch(
                            checked = humanTouch,
                            onCheckedChange = { humanTouch = it }
                        )
                    }

                    if (humanTouch) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isRu) {
                                "Индивидуальная настройка рандомизации клика:"
                            } else {
                                "Custom anti-detection configuration:"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Coordinate Offset Slider
                            Column {
                                Text(
                                    text = if (isRu) "Разброс клика (пиксели): ±${microOffset.toInt()}px" else "Coordinate jitter: ±${microOffset.toInt()}px",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Slider(
                                    value = microOffset,
                                    onValueChange = { microOffset = it },
                                    valueRange = 0f..50f,
                                    steps = 50
                                )
                            }

                            // Interval Jitter Slider
                            Column {
                                Text(
                                    text = if (isRu) "Флуктуация задержки: ±${randomInterval.toInt()}мс" else "Delay fluctuation: ±${randomInterval.toInt()}ms",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Slider(
                                    value = randomInterval,
                                    onValueChange = { randomInterval = it },
                                    valueRange = 0f..200f,
                                    steps = 200
                                )
                            }

                            // Hold Jitter Slider
                            Column {
                                Text(
                                    text = if (isRu) "Флуктуация времени зажатия: ±${randomHold.toInt()}мс" else "Hold time fluctuation: ±${randomHold.toInt()}ms",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Slider(
                                    value = randomHold,
                                    onValueChange = { randomHold = it },
                                    valueRange = 0f..100f,
                                    steps = 100
                                )
                            }
                        }
                    }
                }
            }

            // IF EXPERT MODE IS ACTIVE -> Display coordinate input boxes and save presets configurations
            if (viewModel.isExpertMode) {
                Text(
                    text = if (isRu) "Режим эксперта: Координаты" else "Expert Mode: Coordinates & Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputX,
                        onValueChange = { inputX = it },
                        label = { Text("Raw Position X (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = inputY,
                        onValueChange = { inputY = it },
                        label = { Text("Raw Position Y (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Presets saving row
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = if (isRu) "Имя сохраняемого пресета" else "Preset Name to Save", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = presetName,
                            onValueChange = { presetName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val activeVal = viewModel.activeSingleTapPreset.copy(
                                    name = presetName,
                                    intervalMs = intervalMs.toLongOrNull()?.coerceAtLeast(10) ?: 200,
                                    holdMs = holdMs.toLongOrNull()?.coerceAtLeast(10) ?: 40,
                                    repeatCount = repeats.toIntOrNull() ?: 0,
                                    stopConditionType = stopConditionType,
                                    stopDurationAmount = stopDurationAmount.toLongOrNull() ?: 10L,
                                    stopDurationUnit = stopDurationUnit,
                                    pointsJson = inputX,
                                    zonesJson = inputY,
                                    humanTouchEnabled = humanTouch,
                                    microOffsetPx = microOffset.toInt(),
                                    randomIntervalRange = randomInterval.toInt(),
                                    randomHoldRange = randomHold.toInt()
                                )
                                viewModel.activeSingleTapPreset = activeVal
                                viewModel.savePreset(presetName, "single")
                                Toast.makeText(context, if (isRu) "Сохранен!" else "Saved!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRu) "Запомнить настройки" else "Save Coordinates Preset")
                        }
                    }
                }
            }

            if (!viewModel.isAdFreeUser) {
                com.novaboost.novatap.ui.components.AdmobBanner(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // PRIMARY BIG TRIGGER CTA (Launches Workspace Overlay in Normal mode, runs directly in Expert Mode)
            val isActive = viewModel.isAutomationActive
            Button(
                onClick = {
                    val finalInt = intervalMs.toLongOrNull()?.coerceAtLeast(10) ?: 200
                    val finalHold = holdMs.toLongOrNull()?.coerceAtLeast(10) ?: 40L

                    viewModel.activeSingleTapPreset = Preset(
                        name = presetName,
                        type = "single",
                        intervalMs = finalInt,
                        holdMs = finalHold,
                        repeatCount = repeats.toIntOrNull() ?: 0,
                        stopConditionType = stopConditionType,
                        stopDurationAmount = stopDurationAmount.toLongOrNull() ?: 10L,
                        stopDurationUnit = stopDurationUnit,
                        pointsJson = inputX,
                        zonesJson = inputY,
                        humanTouchEnabled = humanTouch,
                        microOffsetPx = microOffset.toInt(),
                        randomIntervalRange = randomInterval.toInt(),
                        randomHoldRange = randomHold.toInt()
                    )

                    if (viewModel.isOverlayWorkspaceActive) {
                        viewModel.stopOverlayWorkspace(context)
                    } else if (viewModel.isExpertMode) {
                        // Expert Mode triggers DIRECT automation without widget overlay
                        if (isActive) {
                            viewModel.stopAutomation()
                        } else {
                            viewModel.validateAndTriggerAutomation("single", context) {
                                viewModel.startSingleTapAutomation()
                            }
                        }
                    } else {
                        // Normal mode minimizes the app and places the beautiful interactive Draggable point and panel overlay!
                        viewModel.startOverlayWorkspace(context, "single")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_single_tap_button"),
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
                                      else Icons.Default.AdsClick,
                        contentDescription = "Trigger"
                    )
                    Text(
                        text = if (viewModel.isOverlayWorkspaceActive) {
                            if (isRu) "ЗАКРЫТЬ ПАНЕЛЬ УПРАВЛЕНИЯ" else "STOP OVERLAY WORKSPACE"
                        } else if (viewModel.isExpertMode) {
                            if (isActive) (if (isRu) "ОСТАНОВИТЬ КЛИКЕР" else "STOP BACKGROUND CLICK")
                            else (if (isRu) "СТАРТ В ФОНЕ" else "START IN BACKGROUND")
                        } else {
                            if (isRu) "ВЫБРАТЬ РЕЖИМ (ОТКРЫТЬ ПАНЕЛЬ)" else "SELECT MODE (LAUNCH OVERLAY)"
                        },
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Mini debug status console log
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

@Composable
fun StopConditionSelector(
    isRu: Boolean,
    stopConditionType: String,
    onStopConditionTypeChange: (String) -> Unit,
    stopDurationAmount: String,
    onStopDurationAmountChange: (String) -> Unit,
    stopDurationUnit: String,
    onStopDurationUnitChange: (String) -> Unit,
    repeats: String,
    onRepeatsChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isRu) "Остановить после:" else "Stop condition:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Option 1: Infinite
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStopConditionTypeChange("infinite") },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = stopConditionType == "infinite",
                    onClick = { onStopConditionTypeChange("infinite") }
                )
                Column {
                    Text(
                        text = if (isRu) "Работать бесконечно" else "Run indefinitely",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (stopConditionType == "infinite") FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = if (isRu) "Пока не остановите вручную" else "Until manually stopped",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Option 2: Duration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStopConditionTypeChange("duration") },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = stopConditionType == "duration",
                    onClick = { onStopConditionTypeChange("duration") }
                )
                Column {
                    Text(
                        text = if (isRu) "Работа по времени" else "Run by duration",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (stopConditionType == "duration") FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = if (isRu) "Остановится по таймеру" else "Will stop on timer countdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (stopConditionType == "duration") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = stopDurationAmount,
                        onValueChange = onStopDurationAmountChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        label = { Text(if (isRu) "Значение" else "Value") }
                    )

                    // Simple select button group for Unit (seconds, minutes, hours)
                    val units = listOf("seconds", "minutes", "hours")
                    val unitLabels = if (isRu) {
                        listOf("Сек", "Мин", "Час")
                    } else {
                        listOf("Sec", "Min", "Hour")
                    }

                    Row(
                        modifier = Modifier
                            .weight(1.5f)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        units.forEachIndexed { index, unit ->
                            val isSelected = stopDurationUnit == unit
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onStopDurationUnitChange(unit) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unitLabels[index],
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Option 3: Clicks / cycles count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStopConditionTypeChange("clicks") },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = stopConditionType == "clicks",
                    onClick = { onStopConditionTypeChange("clicks") }
                )
                Column {
                    Text(
                        text = if (isRu) "Количество кликов / циклов" else "By number of clicks / loops",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (stopConditionType == "clicks") FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = if (isRu) "Остановится после указанного количества" else "Will stop after the count is reached",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (stopConditionType == "clicks") {
                OutlinedTextField(
                    value = repeats,
                    onValueChange = onRepeatsChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    label = { Text(if (isRu) "Количество" else "Amount") }
                )
            }
        }
    }
}
