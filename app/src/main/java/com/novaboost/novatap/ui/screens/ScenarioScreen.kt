package com.novaboost.novatap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaboost.novatap.data.model.Scenario
import com.novaboost.novatap.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isRu = viewModel.selectedLanguage == "ru"

    var scenarioName by remember { mutableStateOf("My Custom Scenario") }

    LaunchedEffect(viewModel.activeScenarioName) {
        if (viewModel.activeScenarioName.isNotEmpty()) {
            scenarioName = viewModel.activeScenarioName
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRu) "Конструктор сценариев" else "Visual Scenarios Builder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Visual Scenario design help header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isRu) "ПОРЯДОК ЦЕПОЧЕК ОЧЕРЕДИ" else "CHAIN QUEUES WORKFLOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRu) {
                            "Стройте сложные сценарии: Одиночные клики по позициям (например, Step 1), паузы ожидания, свайпы и бесконечные петли loops!"
                        } else {
                            "Combine interactions nodes sequence into an automated playlist flow (e.g. Area Tap -> Delay Wait -> Swipe Stroke -> Loop back)."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // NAME EDITING BOX
            OutlinedTextField(
                value = scenarioName,
                onValueChange = { scenarioName = it },
                label = { Text(if (isRu) "Имя сценария" else "Scenario Playlist Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // CHAIN STEPS PREVIEW WINDOW LISTS
            Text(
                text = if (isRu) "2. Цепочка действий сценария" else "2. Automation Steps Blocks Playlist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (viewModel.scenarioSteps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRu) "Цепочка действий пуста. Добавьте шаги кнопками ниже!" else "Chain is empty. Compile steps using toolbox triggers below!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.scenarioSteps.forEachIndexed { idx, step ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = when(step.type) {
                                                "tap" -> if (isRu) "Клик по координатам" else "Tap Point coordinate"
                                                "area" -> if (isRu) "Кликер Зоны" else "Stochastic Area click"
                                                "swipe" -> if (isRu) "Жест свайпа" else "Gesture Swipe swipe"
                                                "wait" -> if (isRu) "Задержка / Пауза" else "Delay wait breath"
                                                "loop" -> if (isRu) "Вернуться в начало" else "Loop queue repeat"
                                                else -> step.type
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${step.durationMs} ms",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val mList = viewModel.scenarioSteps.toMutableList()
                                        mList.removeAt(idx)
                                        viewModel.scenarioSteps = mList
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Drop node",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // WORKBOX TRIGGER FOR STEPS ENTRIES
            Text(
                text = if (isRu) "3. Добавить шаги автоматизации" else "3. Construct step chains",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { viewModel.addScenarioStep("tap", MainViewModel.MIN_INTERVAL_MS, 500f, 1000f, "Point coordinates tap") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isRu) "+ Клик" else "+ Tap", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                }

                Button(
                    onClick = { viewModel.addScenarioStep("area", MainViewModel.MIN_INTERVAL_MS, 0f, 0f, "Stochastic Area tap") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isRu) "+ Зона" else "+ Area", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                }

                Button(
                    onClick = { viewModel.addScenarioStep("swipe", MainViewModel.MIN_INTERVAL_MS, 0f, 0f, "Drags Swipe stroke") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isRu) "+ Свайп" else "+ Swipe", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { viewModel.addScenarioStep("wait", MainViewModel.MIN_INTERVAL_MS, 0f, 0f, "Delay timer") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isRu) "+ Пауза" else "+ Wait", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                }

                Button(
                    onClick = { viewModel.addScenarioStep("loop", 10, 0f, 0f, "Loop repetition") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isRu) "+ Цикл" else "+ Loop", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                }

                Button(
                    onClick = { viewModel.clearScenarioSteps() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isRu) "Очистить" else "Clear All", fontSize = 11.sp, color = Color.White)
                }
            }

            // Save playlist
            Button(
                onClick = {
                    viewModel.activeScenarioName = scenarioName
                    viewModel.saveScenario(scenarioName)
                    Toast.makeText(context, if (isRu) "Сценарий успешно записан!" else "Scenario playlist successfully saved!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().testTag("save_scenario_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRu) "Сохранить Сценарий в БД" else "Save Automation Scenario Bundle")
            }

            if (!viewModel.isAdFreeUser) {
                com.novaboost.novatap.ui.components.AdmobBanner(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Run scenario
            val isActiveState = viewModel.isAutomationActive
            Button(
                onClick = {
                    viewModel.activeScenarioName = scenarioName
                    if (isActiveState) {
                        viewModel.stopAutomation()
                    } else {
                        viewModel.validateAndTriggerAutomation("scenarios", context) {
                            viewModel.startScenarioAutomation()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_scenario_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActiveState) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isActiveState) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Trigger"
                    )
                    Text(
                        text = if (isActiveState) {
                            if (isRu) "ОСТАНОВИТЬ СЦЕНАРИЙ" else "STOP CURRENT PLAYLIST"
                        } else {
                            if (isRu) "ЗАПУСТИТЬ СЦЕНАРИЙ" else "START SCENARIOS CHAIN"
                        },
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Outputs console feedback
            val logText by viewModel.executionLog.collectAsState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .padding(10.dp)
            ) {
                Text(
                    text = logText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Green,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}
