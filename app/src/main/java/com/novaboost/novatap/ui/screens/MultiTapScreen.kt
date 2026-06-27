package com.novaboost.novatap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaboost.novatap.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiTapScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isRu = viewModel.selectedLanguage == "ru"

    var presetName by remember { mutableStateOf("My Multi Preset") }
    var intervalMs by remember { mutableStateOf("300") }
    var holdMs by remember { mutableStateOf("50") }
    var repeats by remember { mutableStateOf("0") }
    var seqMode by remember { mutableStateOf("sequential") } // "sequential", "random_order", "loop"
    var humanTouchEnabled by remember { mutableStateOf(false) }
    var activeEditPoint by remember { mutableStateOf<com.novaboost.novatap.data.model.TapPoint?>(null) }

    LaunchedEffect(viewModel.activeMultiTapPreset) {
        val preset = viewModel.activeMultiTapPreset
        presetName = preset.name
        intervalMs = preset.intervalMs.toString()
        holdMs = preset.holdMs.toString()
        repeats = preset.repeatCount.toString()
        seqMode = preset.mode
        humanTouchEnabled = preset.humanTouchEnabled
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRu) "Мульти-нажатие" else "Multi Tap Mode") },
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
                        imageVector = Icons.Default.Stream,
                        contentDescription = "Multi",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = if (isRu) "Визуальное размещение" else "Visual Multi-Point Automation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isRu) {
                                "Запустите рабочую панель, нажимайте плюсик на виджете и распределяйте точки пальцем прямо поверх нужного приложения."
                            } else {
                                "Launch workspace panel, tap plus, and place multiple coordinate triggers sequentially with ease."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ONLY DISPLAY DESIGN CANVAS IN EXPERT MODE
            if (viewModel.isExpertMode) {
                Text(
                    text = if (isRu) "Режим эксперта: Холст экрана" else "Expert Mode: Interactive Grid Plotter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(280.dp)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val canvasWidth = size.width.toFloat()
                                val canvasHeight = size.height.toFloat()
                                val displayMetrics = context.resources.displayMetrics
                                val screenWidth = displayMetrics.widthPixels.toFloat()
                                val screenHeight = displayMetrics.heightPixels.toFloat()
                                
                                val realX = offset.x * (screenWidth / canvasWidth)
                                val realY = offset.y * (screenHeight / canvasHeight)
                                val newPoint = viewModel.addMultiTapPoint(realX, realY)
                                if (newPoint != null) {
                                    activeEditPoint = newPoint
                                }
                            }
                        }
                ) {
                    val accentColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val displayMetrics = context.resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels.toFloat()
                    val screenHeight = displayMetrics.heightPixels.toFloat()
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val scaleX = canvasWidth / screenWidth
                        val scaleY = canvasHeight / screenHeight

                        val gridSpacing = 20f
                        for (x in 0..canvasWidth.toInt() step gridSpacing.toInt()) {
                            drawLine(Color.White.copy(alpha = 0.04f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), canvasHeight))
                        }
                        for (y in 0..canvasHeight.toInt() step gridSpacing.toInt()) {
                            drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, y.toFloat()), Offset(canvasWidth, y.toFloat()))
                        }

                        viewModel.multiTapPoints.forEachIndexed { index, tapPoint ->
                            val drawX = tapPoint.x * scaleX
                            val drawY = tapPoint.y * scaleY
                            drawCircle(color = secondaryColor.copy(alpha = 0.25f), center = Offset(drawX, drawY), radius = 16f)
                            drawCircle(color = accentColor, center = Offset(drawX, drawY), radius = 5f)
                        }
                    }

                    Text(
                        text = if (isRu) "Точек: ${viewModel.multiTapPoints.size}/20" else "Points: ${viewModel.multiTapPoints.size}/20",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )

                    IconButton(
                        onClick = { viewModel.clearMultiTapPoints() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                    }
                }

                // List of added points and their timings
                if (viewModel.multiTapPoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRu) "Список точек:" else "List of points:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    viewModel.multiTapPoints.forEachIndexed { index, tapPoint ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isRu) "Точка ${index + 1} (${tapPoint.x.toInt()}, ${tapPoint.y.toInt()})" 
                                               else "Point ${index + 1} (${tapPoint.x.toInt()}, ${tapPoint.y.toInt()})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (isRu) "Пауза: ${tapPoint.delayMs}мс, Зажатие: ${tapPoint.holdMs}мс"
                                               else "Pause: ${tapPoint.delayMs}ms, Hold: ${tapPoint.holdMs}ms",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { activeEditPoint = tapPoint },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteMultiTapPoint(tapPoint.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ORDER / SEQUENCE MODES Choice
            Text(
                text = if (isRu) "Режим порядка клико�            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("sequential", "random_order", "loop").forEach { mode ->
                    val isSelected = seqMode == mode
                    val title = when (mode) {
                        "sequential" -> if (isRu) "Последов." else "Sequential"
                        "random_order" -> if (isRu) "Случайно" else "Random"
                        else -> if (isRu) "Спирально" else "Loop"
                    }
 
                    Button(
                        onClick = { seqMode = mode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
 
            Spacer(modifier = Modifier.height(8.dp))
 
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
 
            Spacer(modifier = Modifier.height(1.dp))
 
            // IF EXPERT MODE: Show loops counter, Preset Naming & Save Preset form
            if (viewModel.isExpertMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = if (isRu) "Сохранить пресет мульти-клика" else "Save Preset Name", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = presetName,
                            onValueChange = { presetName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val activeVal = viewModel.activeMultiTapPreset.copy(
                                    name = presetName,
                                    intervalMs = 300,
                                    holdMs = 50,
                                    repeatCount = repeats.toIntOrNull() ?: 0,
                                    stopConditionType = stopConditionType,
                                    stopDurationAmount = stopDurationAmount.toLongOrNull() ?: 10L,
                                    stopDurationUnit = stopDurationUnit,
                                    mode = seqMode,
                                    humanTouchEnabled = humanTouchEnabled
                                )
                                viewModel.activeMultiTapPreset = activeVal
                                viewModel.savePreset(presetName, "multi")
                                Toast.makeText(context, if (isRu) "Пресет сохранён!" else "Preset Saved!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRu) "Сохранить пресет в базу" else "Commit Preset Database")
                        }
                    }
                }
            }tor = Icons.Default.Save, contentDescription = "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRu) "Сохранить пресет в базу" else "Commit Preset Database")
                        }
                    }
                }
            }

            val isActive = viewModel.isAutomationActive
            Button(
                onClick = {
                    val finalInt = 300L
                    val finalHold = 50L

                    viewModel.activeMultiTapPreset = Preset(
                        name = presetName,
                        type = "multi",
                        intervalMs = finalInt,
                        holdMs = finalHold,
                        repeatCount = repeats.toIntOrNull() ?: 0,
                        stopConditionType = stopConditionType,
                        stopDurationAmount = stopDurationAmount.toLongOrNull() ?: 10L,
                        stopDurationUnit = stopDurationUnit,
                        mode = seqMode,
                        pointsJson = "", // Handled by inmemory multiTapPoints mapping
                        zonesJson = "",
                        humanTouchEnabled = humanTouchEnabled
                    )

                    if (viewModel.isOverlayWorkspaceActive) {
                        viewModel.stopOverlayWorkspace(context)
                    } else if (viewModel.isExpertMode) {
                        if (isActive) {
                            viewModel.stopAutomation()
                        } else {
                            viewModel.validateAndTriggerAutomation("multi", context) {
                                viewModel.startMultiTapAutomation()
                            }
                        }
                    } else {
                        // Normal mode minimizes to overlays setup list instantly so users can layout visual touch paths!
                        viewModel.startOverlayWorkspace(context, "multi")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("start_multi_tap_button"),
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
                                      else Icons.Default.GridView,
                        contentDescription = "Trigger"
                    )
                    Text(
                        text = if (viewModel.isOverlayWorkspaceActive) {
                            if (isRu) "ЗАКРЫТЬ ПАНЕЛЬ УПРАВЛЕНИЯ" else "STOP OVERLAY WORKSPACE"
                        } else if (viewModel.isExpertMode) {
                            if (isActive) (if (isRu) "ОСТАНОВИТЬ АВТОМАТ" else "STOP BACKGROUND SEQUENCE")
                            else (if (isRu) "СТАРТ ЦЕПОЧКИ КЛИКОВ" else "START GRID SEQUENCE")
                        } else {
                            if (isRu) "ВЫБРАТЬ РЕЖИМ (ОТКРЫТЬ ПАНЕЛЬ)" else "SELECT MODE (LAUNCH OVERLAY)"
                        },
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Embedded Console Logging
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

    val currentEditPoint = activeEditPoint
    if (currentEditPoint != null) {
        val point = currentEditPoint
        var tempDelay by remember(point.id) { mutableStateOf(point.delayMs.toString()) }
        var tempHold by remember(point.id) { mutableStateOf(point.holdMs.toString()) }

        AlertDialog(
            onDismissRequest = { activeEditPoint = null },
            title = { Text(if (isRu) "Настройки Точки #${viewModel.multiTapPoints.indexOf(point) + 1}" else "Point #${viewModel.multiTapPoints.indexOf(point) + 1} Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempDelay,
                        onValueChange = { tempDelay = it },
                        label = { Text(if (isRu) "Пауза (мс)" else "Pause (ms)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = tempHold,
                        onValueChange = { tempHold = it },
                        label = { Text(if (isRu) "Удержание (мс)" else "Hold (ms)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val d = tempDelay.toLongOrNull()?.coerceAtLeast(10) ?: 300L
                        val h = tempHold.toLongOrNull()?.coerceAtLeast(10) ?: 50L
                        viewModel.updateMultiTapPointTimings(point.id, d, h)
                        activeEditPoint = null
                    }
                ) {
                    Text(if (isRu) "Применить" else "Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeEditPoint = null }) {
                    Text(if (isRu) "Отмена" else "Cancel")
                }
            }
        )
    }
}
