package com.novaboost.novatap.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaboost.novatap.R
import com.novaboost.novatap.ui.theme.CyberBlue
import com.novaboost.novatap.ui.theme.ElectricPurple
import com.novaboost.novatap.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isRu = viewModel.selectedLanguage == "ru"
    val crashLogs by viewModel.allCrashLogsFlow.collectAsState()
    var expandedLogId by remember { mutableStateOf<Int?>(null) }

    // Recheck permissions on screen focus
    LaunchedEffect(Unit) {
        viewModel.checkAllPermissions(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRu) "Диагностика и Логи" else "Diagnostics & Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SYSTEM STATUS HOVER BLOCK
            item {
                Text(
                    text = stringResource(R.string.device_status).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x13FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatusRow(
                            label = if (isRu) "Служба специальных возможностей" else "Accessibility Core Service",
                            granted = viewModel.isAccessibilityGranted
                        )
                        StatusRow(
                            label = if (isRu) "Отображение поверх других приложений" else "Overlay Window Control",
                            granted = viewModel.isOverlayGranted
                        )
                        StatusRow(
                            label = if (isRu) "Игнорирование оптимизации батареи" else "Battery Optimization Exemption",
                            granted = viewModel.isBatteryExempted
                        )
                        StatusRow(
                            label = if (isRu) "Разрешение на push-уведомления" else "Notification Permissions",
                            granted = viewModel.isNotificationGranted
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", context.packageName, null)
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    viewModel.logException(e, "DiagnosticsSettingsClick")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("reopen_settings_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.reopen_settings), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // CRASH LOGS TITLE + CONTROLS ROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.title_crash_logs).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )

                    if (crashLogs.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Copy logs action
                            IconButton(
                                onClick = {
                                    val textToCopy = crashLogs.joinToString("\n---\n") { log ->
                                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(log.timestamp))
                                        "Time: $date\nScreen: ${log.screenName}\nDevice: ${log.deviceModel} (SDK ${log.androidVersion})\nError: ${log.error}\nStacktrace:\n${log.stacktrace}"
                                    }
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Crash Logs", textToCopy))
                                    Toast.makeText(context, context.getString(R.string.log_copied), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("copy_logs_button")
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberBlue)
                            }

                            // Clear logs action
                            IconButton(
                                onClick = {
                                    viewModel.clearCrashLogs()
                                    Toast.makeText(context, context.getString(R.string.log_cleared), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("clear_logs_button")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // CRASH LOGS ENTRIES
            if (crashLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.empty_logs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(crashLogs) { log ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    val formattedDate = sdf.format(Date(log.timestamp))
                    val isExpanded = expandedLogId == log.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedLogId = if (isExpanded) null else log.id }
                            .testTag("crash_log_item_${log.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0x0FFFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.error,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Screen: ${log.screenName} | $formattedDate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand/Collapse",
                                    tint = CyberBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    Divider(color = Color(0x0FFFFFFF))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    LogMetaItem(if (isRu) "Устройство" else "Device Model", log.deviceModel)
                                    LogMetaItem(if (isRu) "Версия ОС" else "OS Version", "Android ${log.androidVersion}")
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isRu) "Стек вызовов:" else "System Stacktrace:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0F172A))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = log.stacktrace,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFF1F5F9),
                                            lineHeight = 15.sp,
                                            modifier = Modifier.testTag("crash_log_stacktrace_text")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (granted) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(if (granted) R.string.status_granted else R.string.status_missing),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (granted) Color(0xFF10B981) else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun LogMetaItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
