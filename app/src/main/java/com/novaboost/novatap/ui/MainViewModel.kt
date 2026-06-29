package com.novaboost.novatap.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novaboost.novatap.data.database.AppDatabase
import com.novaboost.novatap.data.model.*
import com.novaboost.novatap.data.repository.NovaRepository
import com.novaboost.novatap.accessibility.NovaTapAccessibilityService
import com.novaboost.novatap.overlay.FloatControlPanelService
import com.novaboost.novatap.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class PromoTask(
    val id: String,
    val titleRu: String,
    val titleEn: String,
    val descriptionRu: String,
    val descriptionEn: String,
    val targetPackage: String?,
    val targetUrl: String,
    val rewardTaps: Long = 0,
    val rewardPremiumDays: Int = 0
)

fun Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        @Volatile
        var instance: MainViewModel? = null
            private set
    }

    private val repository: NovaRepository

    var onTriggerPurchase: ((android.app.Activity) -> Unit)? = null

    // Settings States
    var isExpertMode by mutableStateOf(false)
    var selectedLanguage by mutableStateOf("en") // "en" / "ru"
    var selectedTheme by mutableStateOf("dark") // "dark" / "light" / "system"
    var isAdFreeUser by mutableStateOf(false)
    var isOverlayEnabled by mutableStateOf(false)
    var showTapRipples by mutableStateOf(false)

    // Special Promo Tasks
    var bonusTapsLimit by mutableStateOf(0L)
    var premiumExpiresAt by mutableStateOf(0L)
    var completedTaskIds by mutableStateOf<Set<String>>(emptySet())

    val promoTasks = listOf(
        PromoTask(
            id = "novacleaner",
            titleRu = "Установить Nova Cleaner & Booster",
            titleEn = "Install Nova Cleaner & Booster",
            descriptionRu = "Бесплатный оптимизатор памяти и очиститель кэша. Установите его и получите 7 дней Премиум без рекламы и лимитов!",
            descriptionEn = "Free memory optimizer and cache cleaner. Install it to get 7 days of Premium with no ads and unlimited clicks!",
            targetPackage = "com.novaboost.cleaner",
            targetUrl = "https://play.google.com/store/apps/details?id=com.novaboost.cleaner",
            rewardPremiumDays = 7
        ),
        PromoTask(
            id = "novagamebooster",
            titleRu = "Установить Nova Game Booster",
            titleEn = "Install Nova Game Booster",
            descriptionRu = "Ускоритель игр для максимального FPS. Установите его и получите +100,000 кликов к суточному лимиту навсегда!",
            descriptionEn = "Game booster for maximum FPS. Install it to get +100,000 to your daily action limit forever!",
            targetPackage = "com.novaboost.gamebooster",
            targetUrl = "https://play.google.com/store/apps/details?id=com.novaboost.gamebooster",
            rewardTaps = 100000L
        ),
        PromoTask(
            id = "telegram_channel",
            titleRu = "Подписаться на наш Telegram",
            titleEn = "Join our Telegram Channel",
            descriptionRu = "Следите за обновлениями, общайтесь и делитесь сценариями кликера. Награда: +50,000 кликов к лимиту навсегда!",
            descriptionEn = "Follow updates, chat, and share clicker scenarios. Reward: +50,000 to your daily limit forever!",
            targetPackage = null,
            targetUrl = "https://t.me/novaboost_channel",
            rewardTaps = 50000L
        ),
        PromoTask(
            id = "chrometest",
            titleRu = "[ТЕСТ] Проверить установку Google Chrome",
            titleEn = "[TEST] Check Google Chrome Install",
            descriptionRu = "Это тестовое задание для демонстрации работы проверки. Нажмите кнопку, чтобы получить +25,000 кликов навсегда!",
            descriptionEn = "This is a test task to demonstrate how verification works. Click the button to get +25,000 daily clicks forever!",
            targetPackage = "com.android.chrome",
            targetUrl = "https://play.google.com/store/apps/details?id=com.android.chrome",
            rewardTaps = 25000L
        )
    )

    // Statistics States
    private val _todayUsageActions = MutableStateFlow<Long>(0)
    val todayUsageActions = _todayUsageActions.asStateFlow()

    private val _adRewardedAdditionalActions = MutableStateFlow<Long>(0)
    val adRewardedAdditionalActions = _adRewardedAdditionalActions.asStateFlow()

    // Flag for limit-reached popup
    private var _displayLimitDialog by mutableStateOf(false)
    var displayLimitDialog: Boolean
        get() = _displayLimitDialog
        set(value) {
            _displayLimitDialog = value
            if (value) {
                sendLimitReachedNotification()
            }
        }

    // Selected/Active preset details config states for each module
    var activeSingleTapPreset by mutableStateOf(Preset(name = "Single Tap Preset", type = "single", intervalMs = 200, holdMs = 50))
    
    // Multi tap coordinates storage list
    var multiTapPoints by mutableStateOf<List<TapPoint>>(emptyList())
    var activeMultiTapPreset by mutableStateOf(Preset(name = "Multi Tap Preset", type = "multi", mode = "sequential", intervalMs = 300, holdMs = 50))

    // Flagship Area tap zone list
    var areaTapZones by mutableStateOf<List<TapZone>>(emptyList())
    var activeAreaTapPreset by mutableStateOf(Preset(name = "Area Tap Preset", type = "area", mode = "balanced_random", intervalMs = 400, holdMs = 60))

    // Swipe coordinate settings state
    var activeSwipePreset by mutableStateOf(Preset(name = "Swipe Preset", type = "swipe", intervalMs = 800, holdMs = 300))
    var swipeCoordinates by mutableStateOf(SwipeCoordinates(startX = 200f, startY = 800f, endX = 800f, endY = 200f))

    // Scenario builder state
    var activeScenarioName by mutableStateOf("")
    var scenarioSteps by mutableStateOf<List<ScenarioStep>>(emptyList())

    // Engine running state triggers
    var onAutomationActiveChanged: ((Boolean) -> Unit)? = null

    private var _isAutomationActive by mutableStateOf(false)
    var isAutomationActive: Boolean
        get() = _isAutomationActive
        set(value) {
            _isAutomationActive = value
            if (value) {
                resetDiagnostics()
            }
            onAutomationActiveChanged?.invoke(value)
        }
    val executionLog = MutableStateFlow<String>("")
    private var lastLogTime = 0L

    // Live diagnostics states to track tap delivery success vs OS drops
    var showDiagnostics by mutableStateOf(false)
    val diagnosticAttemptedCount = MutableStateFlow<Int>(0)
    val diagnosticDispatchedCount = MutableStateFlow<Int>(0)
    val diagnosticDroppedCount = MutableStateFlow<Int>(0)
    val diagnosticLatencyMs = MutableStateFlow<Long>(0L)
    val diagnosticLastStatus = MutableStateFlow<String>("IDLE")
    val diagnosticIsThrottled = MutableStateFlow<Boolean>(false)

    fun resetDiagnostics() {
        diagnosticAttemptedCount.value = 0
        diagnosticDispatchedCount.value = 0
        diagnosticDroppedCount.value = 0
        diagnosticLatencyMs.value = 0L
        diagnosticLastStatus.value = "RESET"
        diagnosticIsThrottled.value = false
    }

    suspend fun performClickSuspended(x: Float, y: Float, holdDurationMs: Long): Boolean {
        diagnosticAttemptedCount.value += 1
        val service = NovaTapAccessibilityService.instance
        if (service == null) {
            diagnosticDroppedCount.value += 1
            diagnosticLastStatus.value = "DROPPED: Service off"
            return false
        }
        val startTime = System.currentTimeMillis()
        val worked = service.clickSuspended(x, y, holdDurationMs)
        val endTime = System.currentTimeMillis()
        val latency = endTime - startTime
        diagnosticLatencyMs.value = latency
        if (worked) {
            diagnosticDispatchedCount.value += 1
            diagnosticLastStatus.value = "TAP at (${x.toInt()}, ${y.toInt()})"
            // Highlight throttle if gesture dispatch response latency exceeds 150ms
            diagnosticIsThrottled.value = latency > 150
        } else {
            diagnosticDroppedCount.value += 1
            diagnosticLastStatus.value = "DROPPED by OS / busy"
        }
        return worked
    }

    suspend fun performSwipeSuspended(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long): Boolean {
        diagnosticAttemptedCount.value += 1
        val service = NovaTapAccessibilityService.instance
        if (service == null) {
            diagnosticDroppedCount.value += 1
            diagnosticLastStatus.value = "DROPPED: Service off"
            return false
        }
        val startTime = System.currentTimeMillis()
        val worked = service.swipeSuspended(startX, startY, endX, endY, durationMs)
        val endTime = System.currentTimeMillis()
        val latency = endTime - startTime
        diagnosticLatencyMs.value = latency
        if (worked) {
            diagnosticDispatchedCount.value += 1
            diagnosticLastStatus.value = "SWIPE (${startX.toInt()}, ${startY.toInt()}) -> (${endX.toInt()}, ${endY.toInt()})"
            diagnosticIsThrottled.value = latency > (durationMs + 100)
        } else {
            diagnosticDroppedCount.value += 1
            diagnosticLastStatus.value = "DROPPED by OS / busy"
        }
        return worked
    }

    fun logExecution(message: String, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (force || now - lastLogTime >= 400L) {
            executionLog.value = message
            lastLogTime = now
        }
    }

    // List of active visual tap indicators to render as transient overlays
    val visualTapEvents = MutableStateFlow<List<VisualTapEvent>>(emptyList())
    private var lastVisualTapTime = 0L

    fun triggerVisualTap(x: Float, y: Float) {
        val now = System.currentTimeMillis()
        if (now - lastVisualTapTime < 150L) {
            // Rate limit to at most 6.6 ripples per second to save CPU/battery and prevent main-thread choking on old devices
            return
        }
        lastVisualTapTime = now
        val event = VisualTapEvent(x = x, y = y)
        val currentList = visualTapEvents.value.toMutableList()
        currentList.add(event)
        visualTapEvents.value = currentList
        
        // Auto-remove the event after 500 milliseconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            val updated = visualTapEvents.value.filter { it.id != event.id }
            visualTapEvents.value = updated
        }
    }

    // Flag for overlay service running
    var isOverlayWorkspaceActive by mutableStateOf(false)

    // Running Job for active loop executions
    private var automationJob: Job? = null

    // Standard list observables from Room
    val allPresetsFlow: StateFlow<List<Preset>>
    val allScenariosFlow: StateFlow<List<Scenario>>
    val allCrashLogsFlow: StateFlow<List<CrashLog>>

    var validationErrorTitle by mutableStateOf<String?>(null)
    var validationErrorMessage by mutableStateOf<String?>(null)

    // System Status states
    var isAccessibilityGranted by mutableStateOf(false)
    var isOverlayGranted by mutableStateOf(false)
    var isBatteryExempted by mutableStateOf(false)
    var isNotificationGranted by mutableStateOf(false)

    init {
        instance = this
        val db = AppDatabase.getDatabase(application)
        repository = NovaRepository(
            db.presetDao(),
            db.scenarioDao(),
            db.settingDao(),
            db.statisticDao(),
            db.crashLogDao()
        )

        allPresetsFlow = repository.allPresets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allScenariosFlow = repository.allScenarios.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allCrashLogsFlow = repository.allCrashLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Sync setup settings and daily statistics from DB
        viewModelScope.launch {
            val deviceLang = java.util.Locale.getDefault().language
            val defaultLang = if (deviceLang == "ru" || deviceLang.startsWith("ru")) "ru" else "en"
            
            selectedLanguage = repository.getSettingValue("lang", defaultLang)
            selectedTheme = repository.getSettingValue("theme", "dark")
            isAdFreeUser = repository.getSettingValue("ad_free", "false").toBoolean()
            showTapRipples = repository.getSettingValue("show_ripples", "false").toBoolean()
            showDiagnostics = repository.getSettingValue("show_diagnostics", "false").toBoolean()

            // Load special task settings
            bonusTapsLimit = repository.getSettingValue("bonus_taps_limit", "0").toLong()
            premiumExpiresAt = repository.getSettingValue("premium_expires_at", "0").toLong()
            val completedSet = mutableSetOf<String>()
            for (task in promoTasks) {
                if (repository.getSettingValue("task_completed_${task.id}", "false").toBoolean()) {
                    completedSet.add(task.id)
                }
            }
            completedTaskIds = completedSet
            
            // Activate Premium if temporary premium is currently running
            if (premiumExpiresAt > System.currentTimeMillis()) {
                isAdFreeUser = true
            }

            repository.getDailyStatisticFlow().collect { daily ->
                if (daily != null) {
                    _todayUsageActions.value = daily.totalActions
                } else {
                    _todayUsageActions.value = 0
                }
            }
        }
    }

    fun showValidationErrorDialog(title: String, message: String) {
        validationErrorTitle = title
        validationErrorMessage = message
    }

    fun dismissValidationErrorDialog() {
        validationErrorTitle = null
        validationErrorMessage = null
    }

    fun checkAllPermissions(context: Context) {
        try {
            val serviceStr = "${context.packageName}/${NovaTapAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            isAccessibilityGranted = enabledServices.contains(serviceStr) || NovaTapAccessibilityService.instance != null

            isOverlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }

            isBatteryExempted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            } else {
                true
            }

            isNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logException(e, "SettingsPermissionCheck")
        }
    }

    fun isStartupSetupWizardNeeded(context: Context): Boolean {
        checkAllPermissions(context)
        return !isAccessibilityGranted || !isOverlayGranted || !isBatteryExempted || !isNotificationGranted
    }

    fun validateAndTriggerAutomation(type: String, context: Context, onValidStart: () -> Unit): Boolean {
        try {
            checkAllPermissions(context)

            if (!isAccessibilityGranted || NovaTapAccessibilityService.instance == null) {
                showValidationErrorDialog(
                    context.getString(R.string.validation_error_title),
                    context.getString(R.string.validation_acc_service_missing)
                )
                return false
            }

            if (!isOverlayGranted) {
                showValidationErrorDialog(
                    context.getString(R.string.validation_error_title),
                    context.getString(R.string.validation_overlay_missing)
                )
                return false
            }

            if (NovaTapAccessibilityService.instance == null) {
                showValidationErrorDialog(
                    context.getString(R.string.validation_error_title),
                    context.getString(R.string.validation_service_uninitialized)
                )
                return false
            }

            when (type) {
                "single" -> {
                    val config = activeSingleTapPreset
                    val px = config.pointsJson.toDoubleOrNull()?.toFloat() ?: 500f
                    val py = config.zonesJson.toDoubleOrNull()?.toFloat() ?: 1000f
                    if (px < 0 || py < 0) {
                        showValidationErrorDialog(
                            context.getString(R.string.validation_error_title),
                            context.getString(R.string.validation_coords_invalid)
                        )
                        return false
                    }
                }
                "multi" -> {
                    if (multiTapPoints.isEmpty()) {
                        showValidationErrorDialog(
                            context.getString(R.string.validation_error_title),
                            context.getString(R.string.validation_no_multi_points)
                        )
                        return false
                    }
                }
                "area" -> {
                    if (areaTapZones.none { it.isAllowed }) {
                        showValidationErrorDialog(
                            context.getString(R.string.validation_error_title),
                            context.getString(R.string.validation_no_area_zones)
                        )
                        return false
                    }
                }
                "scenarios" -> {
                    if (scenarioSteps.isEmpty()) {
                        showValidationErrorDialog(
                            context.getString(R.string.validation_error_title),
                            context.getString(R.string.validation_invalid_scenario)
                        )
                        return false
                    }
                }
            }

            onValidStart()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            logException(e, "validateAndTriggerAutomation")
            showValidationErrorDialog("Error", e.localizedMessage ?: "Unexpected Automation Validation Error")
            return false
        }
    }

    fun logException(e: Throwable, screen: String) {
        viewModelScope.launch {
            try {
                val logObj = CrashLog(
                    timestamp = System.currentTimeMillis(),
                    error = e.localizedMessage ?: e.toString(),
                    stacktrace = e.stackTraceToString(),
                    screenName = screen
                )
                repository.insertCrashLog(logObj)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun clearCrashLogs() {
        viewModelScope.launch {
            try {
                repository.clearCrashLogs()
            } catch (e: Exception) {
                e.printStackTrace()
                logException(e, "clearCrashLogs")
            }
        }
    }

    // Languages Toggle
    fun toggleLanguage(lang: String) {
        selectedLanguage = lang
        viewModelScope.launch {
            repository.saveSetting("lang", lang)
        }
    }

    // Theme Toggle
    fun toggleTheme(theme: String) {
        selectedTheme = theme
        viewModelScope.launch {
            repository.saveSetting("theme", theme)
        }
    }

    // Tap Ripples Toggle
    fun toggleTapRipples(enabled: Boolean) {
        showTapRipples = enabled
        viewModelScope.launch {
            repository.saveSetting("show_ripples", enabled.toString())
        }
    }

    // Diagnostics HUD Toggle
    fun toggleDiagnostics(enabled: Boolean) {
        showDiagnostics = enabled
        viewModelScope.launch {
            repository.saveSetting("show_diagnostics", enabled.toString())
        }
    }

    // Remove Ads Subscription
    fun removeAdsService() {
        isAdFreeUser = true
        viewModelScope.launch {
            repository.saveSetting("ad_free", "true")
        }
    }

    fun triggerPremiumPurchase(activity: android.app.Activity) {
        val launcher = onTriggerPurchase
        if (launcher != null) {
            launcher(activity)
        } else {
            // Fallback
            removeAdsService()
        }
    }

    fun setPremiumUnlocked(unlocked: Boolean) {
        isAdFreeUser = unlocked
        viewModelScope.launch {
            repository.saveSetting("ad_free", unlocked.toString())
        }
    }

    fun checkPremiumStatus() {
        viewModelScope.launch {
            val permPremium = repository.getSettingValue("ad_free", "false").toBoolean()
            val tempPremiumExpires = repository.getSettingValue("premium_expires_at", "0").toLong()
            premiumExpiresAt = tempPremiumExpires
            isAdFreeUser = permPremium || (tempPremiumExpires > System.currentTimeMillis())
        }
    }

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            val context = getApplication<Application>()
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun claimTaskReward(task: PromoTask, context: android.content.Context) {
        if (completedTaskIds.contains(task.id)) {
            val msg = if (selectedLanguage == "ru") "Награда уже получена!" else "Reward already claimed!"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            return
        }

        if (task.targetPackage != null) {
            val installed = isAppInstalled(task.targetPackage)
            if (!installed) {
                val errorMsg = if (selectedLanguage == "ru") {
                    "Приложение еще не установлено. Пожалуйста, установите его из Play Store."
                } else {
                    "The app is not installed yet. Please install it from the Play Store."
                }
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                return
            }
        }

        viewModelScope.launch {
            repository.saveSetting("task_completed_${task.id}", "true")
            completedTaskIds = completedTaskIds + task.id

            if (task.rewardTaps > 0) {
                val newBonus = bonusTapsLimit + task.rewardTaps
                repository.saveSetting("bonus_taps_limit", newBonus.toString())
                bonusTapsLimit = newBonus
            }

            if (task.rewardPremiumDays > 0) {
                val currentExpires = repository.getSettingValue("premium_expires_at", "0").toLong()
                val now = System.currentTimeMillis()
                val baseTime = if (currentExpires > now) currentExpires else now
                val addedMs = task.rewardPremiumDays * 24L * 60L * 60L * 1000L
                val newExpires = baseTime + addedMs
                repository.saveSetting("premium_expires_at", newExpires.toString())
                premiumExpiresAt = newExpires
                isAdFreeUser = true
            }

            val successMsg = if (selectedLanguage == "ru") {
                "Успешно! Награда получена. Спасибо за поддержку!"
            } else {
                "Success! Reward claimed. Thank you for your support!"
            }
            Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
            checkPremiumStatus()
        }
    }

    fun sendLimitReachedNotification() {
        val context = getApplication<Application>()
        val channelId = "limit_notification"
        val channelName = if (selectedLanguage == "ru") "Уведомления о лимите" else "Limit Notifications"
        val notificationId = 1001

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = if (selectedLanguage == "ru") "Уведомления о превышении дневного лимита действий" else "Notifications when daily actions limit is exceeded"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to launch MainActivity
        val intent = Intent(context, com.novaboost.novatap.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val title = if (selectedLanguage == "ru") {
            "Дневной лимит тапов исчерпан! 🚀"
        } else {
            "Daily click limit reached! 🚀"
        }

        val contentText = if (selectedLanguage == "ru") {
            "Хотите продолжить? Уберите рекламу и лимиты всего за \$0.99 подпиской! Это даст 100% обход детекции (Stealth mode) и снижение нагрузки на 40%!"
        } else {
            "Want to continue? Remove ads and limits for just \$0.99! Get 100% detection bypass (Stealth mode) & 40% CPU load reduction!"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        var isPermissionGranted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (isPermissionGranted) {
            try {
                notificationManager.notify(notificationId, builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
                logException(e, "SendNotification")
            }
        }
    }

    // Simulated Ad States
    var showSimulatedAd by mutableStateOf(false)
    var adType by mutableStateOf("rewarded") // "rewarded" or "short"
    var adCountdownSeconds by mutableStateOf(30)
    var isAdSkippable by mutableStateOf(false)
    var resetStatsAfterAd by mutableStateOf(false)
    private var adTimerJob: kotlinx.coroutines.Job? = null

    // Real AdMob Objects
    private var mInterstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd? = null
    private var isInterstitialAdLoading = false
    private var mRewardedAd: com.google.android.gms.ads.rewarded.RewardedAd? = null
    private var isRewardedAdLoading = false

    private var simulatedAdOnRewarded: (() -> Unit)? = null
    private var simulatedAdOnDismiss: (() -> Unit)? = null

    // Reward Ad Watch Limits: Max 3 watches in 15 minutes
    private val rewardAdWatchTimestamps = mutableListOf<Long>()
    private var lastShortAdShowTime = 0L

    fun loadInterstitialAd(context: Context) {
        if (isAdFreeUser) return
        if (mInterstitialAd != null || isInterstitialAdLoading) return

        isInterstitialAdLoading = true
        val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
        com.google.android.gms.ads.interstitial.InterstitialAd.load(
            context,
            "ca-app-pub-4796438938442217/8794381782",
            adRequest,
            object : com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: com.google.android.gms.ads.LoadAdError) {
                    mInterstitialAd = null
                    isInterstitialAdLoading = false
                }

                override fun onAdLoaded(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isInterstitialAdLoading = false
                }
            }
        )
    }

    fun loadRewardedAd(context: Context) {
        if (isAdFreeUser) return
        if (mRewardedAd != null || isRewardedAdLoading) return

        isRewardedAdLoading = true
        val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
        com.google.android.gms.ads.rewarded.RewardedAd.load(
            context,
            "ca-app-pub-4796438938442217/7746426360",
            adRequest,
            object : com.google.android.gms.ads.rewarded.RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: com.google.android.gms.ads.LoadAdError) {
                    mRewardedAd = null
                    isRewardedAdLoading = false
                }

                override fun onAdLoaded(rewardedAd: com.google.android.gms.ads.rewarded.RewardedAd) {
                    mRewardedAd = rewardedAd
                    isRewardedAdLoading = false
                }
            }
        )
    }

    fun showInterstitialAd(context: Context, onDismiss: () -> Unit) {
        if (isAdFreeUser) {
            onDismiss()
            return
        }

        val activity = context.findActivity()
        if (activity == null) {
            onDismiss()
            return
        }

        val ad = mInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    loadInterstitialAd(context)
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    mInterstitialAd = null
                    loadInterstitialAd(context)
                    onDismiss()
                }
            }
            ad.show(activity)
        } else {
            loadInterstitialAd(context)
            onDismiss()
        }
    }

    fun showRewardedAd(context: Context, onRewarded: () -> Unit, onDismiss: () -> Unit) {
        if (isAdFreeUser) {
            onRewarded()
            onDismiss()
            return
        }

        val activity = context.findActivity()
        if (activity == null) {
            startSimulatedAdWithCallback(onRewarded, onDismiss)
            return
        }

        val ad = mRewardedAd
        if (ad != null) {
            var earned = false
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mRewardedAd = null
                    loadRewardedAd(context)
                    if (earned) {
                        onRewarded()
                    }
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    mRewardedAd = null
                    loadRewardedAd(context)
                    startSimulatedAdWithCallback(onRewarded, onDismiss)
                }
            }
            ad.show(activity) {
                earned = true
            }
        } else {
            loadRewardedAd(context)
            startSimulatedAdWithCallback(onRewarded, onDismiss)
        }
    }

    fun startSimulatedAdWithCallback(onRewarded: () -> Unit, onDismiss: () -> Unit) {
        simulatedAdOnRewarded = onRewarded
        simulatedAdOnDismiss = onDismiss
        startSimulatedAd("rewarded")
    }

    fun canWatchRewardAd(): Boolean {
        val now = System.currentTimeMillis()
        rewardAdWatchTimestamps.removeAll { now - it > 15 * 60 * 1000 }
        return rewardAdWatchTimestamps.size < 3
    }

    fun getMinutesUntilNextRewardAd(): Int {
        val now = System.currentTimeMillis()
        rewardAdWatchTimestamps.removeAll { now - it > 15 * 60 * 1000 }
        if (rewardAdWatchTimestamps.size < 3) return 0
        val oldest = rewardAdWatchTimestamps.firstOrNull() ?: return 0
        val diffMs = (15 * 60 * 1000) - (now - oldest)
        return (diffMs / 60000).toInt().coerceAtLeast(1)
    }

    fun startSimulatedAd(type: String) {
        if (isAdFreeUser) return

        if (type == "rewarded" && !canWatchRewardAd()) {
            return
        }

        adTimerJob?.cancel()
        adType = type
        showSimulatedAd = true
        adCountdownSeconds = if (type == "rewarded") 30 else 5
        isAdSkippable = type == "short"

        adTimerJob = viewModelScope.launch {
            while (adCountdownSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                adCountdownSeconds--
            }
            if (type == "rewarded") {
                rewardAdWatchTimestamps.add(System.currentTimeMillis())
                if (resetStatsAfterAd) {
                    resetStats()
                    resetStatsAfterAd = false
                    executionLog.value = "Watched Reward Ad. Daily counters reset successful!"
                } else {
                    val onRewarded = simulatedAdOnRewarded
                    if (onRewarded != null) {
                        onRewarded()
                        simulatedAdOnRewarded = null
                    } else {
                        _adRewardedAdditionalActions.value += 10000
                        executionLog.value = "Watched Reward Ad. Limits extended by +10,000 actions."
                    }
                }
                val onDismiss = simulatedAdOnDismiss
                if (onDismiss != null) {
                    onDismiss()
                    simulatedAdOnDismiss = null
                }
            }
            showSimulatedAd = false
        }
    }

    fun watchAdToResetStats(context: Context) {
        if (canWatchRewardAd()) {
            resetStatsAfterAd = true
            showRewardedAd(
                context = context,
                onRewarded = {
                    resetStats()
                    executionLog.value = "Watched Ad. Daily counters reset successful!"
                },
                onDismiss = {}
            )
        } else {
            val mins = getMinutesUntilNextRewardAd()
            val text = if (selectedLanguage == "ru") {
                "Вы можете посмотреть максимум 3 рекламы за 15 минут. Подождите еще $mins мин. или купите подписку за \$0.99!"
            } else {
                "You can watch at most 3 ads per 15 minutes. Wait $mins min or remove ads for \$0.99!"
            }
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }

    fun skipOrCloseSimulatedAd() {
        if (adType == "short" || adCountdownSeconds <= 0) {
            adTimerJob?.cancel()
            showSimulatedAd = false
        }
    }

    fun triggerShortAdOnResume(context: Context) {
        if (isAdFreeUser) return
        val now = System.currentTimeMillis()
        if (now - lastShortAdShowTime > 45 * 1000) {
            lastShortAdShowTime = now
            showInterstitialAd(context) {}
        }
    }

    // Watch Ad triggers +10,000 actions (with real AdMob and simulation fallback)
    fun rewardUserByAd(context: Context) {
        if (canWatchRewardAd()) {
            displayLimitDialog = false
            showRewardedAd(
                context = context,
                onRewarded = {
                    _adRewardedAdditionalActions.value += 10000
                    executionLog.value = "Watched Reward Ad. Limits extended by +10,000 actions."
                },
                onDismiss = {}
            )
        } else {
            val mins = getMinutesUntilNextRewardAd()
            val text = if (selectedLanguage == "ru") {
                "Вы можете посмотреть максимум 3 рекламы за 15 минут. Подождите еще $mins мин. или купите подписку за \$0.99!"
            } else {
                "You can watch at most 3 ads per 15 minutes. Wait $mins min or remove ads for \$0.99!"
            }
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }

    fun getActionsLimit(): Long {
        return if (isAdFreeUser) Long.MAX_VALUE else (50000 + _adRewardedAdditionalActions.value + bonusTapsLimit)
    }

    fun getRemainingActions(): Long {
        val totalAllowed = getActionsLimit()
        if (totalAllowed == Long.MAX_VALUE) return 9999999
        val rem = totalAllowed - _todayUsageActions.value
        return rem.coerceAtLeast(0)
    }

    // Add point to Multi-Tap
    fun addMultiTapPoint(x: Float, y: Float): TapPoint? {
        if (multiTapPoints.size < 20) {
            val newPoints = multiTapPoints.toMutableList()
            val point = TapPoint(x = x, y = y, label = "${newPoints.size + 1}")
            newPoints.add(point)
            multiTapPoints = newPoints
            return point
        }
        return null
    }

    fun updateMultiTapPoint(id: String, x: Float, y: Float) {
        val newPoints = multiTapPoints.map {
            if (it.id == id) it.copy(x = x, y = y) else it
        }
        multiTapPoints = newPoints
    }

    fun updateMultiTapPointTimings(id: String, delayMs: Long, holdMs: Long) {
        val newPoints = multiTapPoints.map {
            if (it.id == id) it.copy(delayMs = delayMs, holdMs = holdMs) else it
        }
        multiTapPoints = newPoints
    }

    fun deleteMultiTapPoint(id: String) {
        val filtered = multiTapPoints.filter { it.id != id }
        // Re-label
        multiTapPoints = filtered.mapIndexed { index, point ->
            point.copy(label = "${index + 1}")
        }
    }

    fun duplicateMultiTapPoint(id: String) {
        if (multiTapPoints.size >= 20) return
        val target = multiTapPoints.find { it.id == id } ?: return
        val newPoints = multiTapPoints.toMutableList()
        newPoints.add(target.copy(id = java.util.UUID.randomUUID().toString(), x = target.x + 40f, y = target.y + 40f, label = "${newPoints.size + 1}"))
        multiTapPoints = newPoints
    }

    // Clear Multi Tap Points
    fun clearMultiTapPoints() {
        multiTapPoints = emptyList()
    }

    // Add Area Tap Zone
    fun addAreaTapZone(isAllowed: Boolean, isRect: Boolean, x: Float, y: Float) {
        val newZones = areaTapZones.toMutableList()
        val num = newZones.size + 1
        newZones.add(
            TapZone(
                isAllowed = isAllowed,
                isRect = isRect,
                x = x,
                y = y,
                width = if (isRect) 180f else 140f,
                height = if (isRect) 130f else 140f,
                radius = 70f,
                probability = if (newZones.isEmpty()) 100 else 0
            )
        )
        // Auto balance probabilities equally
        val balancedPct = 100 / newZones.size
        for (i in 0 until newZones.size) {
            newZones[i] = newZones[i].copy(probability = if (i == newZones.size - 1) 100 - (balancedPct * i) else balancedPct)
        }
        areaTapZones = newZones
    }

    fun updateAreaTapZone(id: String, x: Float, y: Float, width: Float, height: Float, radius: Float) {
        val newZones = areaTapZones.map {
            if (it.id == id) it.copy(x = x, y = y, width = width, height = height, radius = radius) else it
        }
        areaTapZones = newZones
    }

    fun deleteAreaTapZone(id: String) {
        val filtered = areaTapZones.filter { it.id != id }
        // Re-balance probabilities
        if (filtered.isNotEmpty()) {
            val balancedPct = 100 / filtered.size
            val updated = filtered.toMutableList()
            for (i in 0 until updated.size) {
                updated[i] = updated[i].copy(probability = if (i == updated.size - 1) 100 - (balancedPct * i) else balancedPct)
            }
            areaTapZones = updated
        } else {
            areaTapZones = emptyList()
        }
    }

    fun duplicateAreaTapZone(id: String) {
        val target = areaTapZones.find { it.id == id } ?: return
        val newZones = areaTapZones.toMutableList()
        newZones.add(target.copy(id = java.util.UUID.randomUUID().toString(), x = target.x + 50f, y = target.y + 50f))
        // Re-balance
        val balancedPct = 100 / newZones.size
        for (i in 0 until newZones.size) {
            newZones[i] = newZones[i].copy(probability = if (i == newZones.size - 1) 100 - (balancedPct * i) else balancedPct)
        }
        areaTapZones = newZones
    }

    // Clear Area Zones
    fun clearAreaZones() {
        areaTapZones = emptyList()
    }

    // Add Step to Scenario
    fun addScenarioStep(type: String, durationMs: Long, x: Float = 0f, y: Float = 0f, label: String = "") {
        val newSteps = scenarioSteps.toMutableList()
        newSteps.add(
            ScenarioStep(
                type = type,
                durationMs = durationMs,
                x = x,
                y = y,
                stepLabel = label.ifEmpty { "$type $durationMs ms" }
            )
        )
        scenarioSteps = newSteps
    }

    // Clear Steps
    fun clearScenarioSteps() {
        scenarioSteps = emptyList()
    }

    // Preset Saving & Loading Local DB
    fun savePreset(name: String, type: String) {
        viewModelScope.launch {
            val points = if (type == "multi") JsonHelpers.serializeTapPoints(multiTapPoints) else "[]"
            val zones = if (type == "area") JsonHelpers.serializeTapZones(areaTapZones) else "[]"
            val swipeData = if (type == "swipe") JsonHelpers.serializeSwipeCoordinates(swipeCoordinates) else "{}"

            val basePreset = when(type) {
                "single" -> activeSingleTapPreset
                "multi" -> activeMultiTapPreset
                "area" -> activeAreaTapPreset
                "swipe" -> activeSwipePreset
                else -> activeSingleTapPreset
            }

            val savedPreset = basePreset.copy(
                name = name,
                type = type,
                pointsJson = points,
                zonesJson = zones,
                swipeStartAndEnd = swipeData,
                timestamp = System.currentTimeMillis()
            )

            repository.insertPreset(savedPreset)
            executionLog.value = "Saved preset: $name"
        }
    }

    fun loadPreset(preset: Preset) {
        viewModelScope.launch {
            when (preset.type) {
                "single" -> {
                    activeSingleTapPreset = preset
                }
                "multi" -> {
                    activeMultiTapPreset = preset
                    multiTapPoints = JsonHelpers.deserializeTapPoints(preset.pointsJson)
                }
                "area" -> {
                    activeAreaTapPreset = preset
                    areaTapZones = JsonHelpers.deserializeTapZones(preset.zonesJson)
                }
                "swipe" -> {
                    activeSwipePreset = preset
                    swipeCoordinates = JsonHelpers.deserializeSwipeCoordinates(preset.swipeStartAndEnd)
                }
            }
            executionLog.value = "Loaded preset: ${preset.name}"
        }
    }

    fun deletePreset(id: Int) {
        viewModelScope.launch {
            repository.deletePresetById(id)
        }
    }

    // Preset Saving & Loading Local DB for Scenario
    fun saveScenario(name: String) {
        if (name.isEmpty()) return
        viewModelScope.launch {
            val stepsJson = JsonHelpers.serializeScenarioSteps(scenarioSteps)
            val sc = Scenario(name = name, stepsJson = stepsJson)
            repository.insertScenario(sc)
            executionLog.value = "Saved scenario: $name"
        }
    }

    fun loadScenario(scenario: Scenario) {
        activeScenarioName = scenario.name
        scenarioSteps = JsonHelpers.deserializeScenarioSteps(scenario.stepsJson)
        executionLog.value = "Loaded scenario: ${scenario.name}"
    }

    fun deleteScenario(id: Int) {
        viewModelScope.launch {
            repository.deleteScenarioById(id)
        }
    }

    // Statistics deletion / configuration resets
    fun resetStats() {
        viewModelScope.launch {
            repository.resetTodayStatistics()
            _adRewardedAdditionalActions.value = 0
            executionLog.value = "Statistics have been reset."
        }
    }

    fun startOverlayWorkspace(context: Context, mode: String) {
        if (!isOverlayGranted) {
            showValidationErrorDialog("Permission Required", "Draw over other apps permission is required to start the visual workspace.")
            return
        }
        isAutomationActive = false
        stopAutomation()

        val intent = Intent(context, FloatControlPanelService::class.java).apply {
            putExtra("mode", mode)
        }
        context.startService(intent)
        isOverlayWorkspaceActive = true
    }

    fun stopOverlayWorkspace(context: Context) {
        val intent = Intent(context, FloatControlPanelService::class.java)
        context.stopService(intent)
        isOverlayWorkspaceActive = false
    }

    // Stop Automation
    fun stopAutomation() {
        isAutomationActive = false
        automationJob?.cancel()
        automationJob = null
        executionLog.value = "Automation session stopped."
    }

    // Trigger Tap automation sequence
    fun startSingleTapAutomation() {
        if (isAutomationActive) {
            stopAutomation()
            return
        }

        val config = activeSingleTapPreset
        // Validate timing safety
        if (config.intervalMs < 10 || config.holdMs < 10) {
            executionLog.value = "Safety Cancelled: Timing parameters cannot be lower than 10ms!"
            return
        }

        isAutomationActive = true
        executionLog.value = "Started Single Tap Loop..."

        val stopDurationMs = when (config.stopDurationUnit) {
            "seconds" -> config.stopDurationAmount * 1000L
            "minutes" -> config.stopDurationAmount * 60 * 1000L
            "hours" -> config.stopDurationAmount * 60 * 60 * 1000L
            else -> 0L
        }
        val startTime = System.currentTimeMillis()

        automationJob = viewModelScope.launch {
            var counter = 0
            while (isAutomationActive) {
                // Check daily limits
                if (getRemainingActions() <= 0) {
                    isAutomationActive = false
                    displayLimitDialog = true
                    executionLog.value = "Daily interaction limit reached."
                    break
                }

                // Check stop condition: duration
                if (config.stopConditionType == "duration" && stopDurationMs > 0) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed >= stopDurationMs) {
                        executionLog.value = "Finished: Stop condition met (duration limit)."
                        isAutomationActive = false
                        break
                    }
                }

                // Parse configurations
                var finalX = config.pointsJson.toDoubleOrNull()?.toFloat() ?: 500f
                var finalY = config.zonesJson.toDoubleOrNull()?.toFloat() ?: 1000f

                // Anti-detection: always add a subtle sub-pixel/micro-pixel jitter of (-1.5 to +1.5 pixels)
                // even when humanization config is off. This prevents identical click coordinate sequences,
                // which is the primary detection signature used by advanced anti-clicker games.
                val antiCheatJitterX = Random.nextFloat() * 3f - 1.5f
                val antiCheatJitterY = Random.nextFloat() * 3f - 1.5f
                finalX += antiCheatJitterX
                finalY += antiCheatJitterY

                // Human engine touch randomness offsets
                var currentInterval = config.intervalMs
                var currentHold = config.holdMs

                if (config.humanTouchEnabled) {
                    if (config.microOffsetPx > 0) {
                        finalX += Random.nextInt(-config.microOffsetPx, config.microOffsetPx)
                        finalY += Random.nextInt(-config.microOffsetPx, config.microOffsetPx)
                    }
                    if (config.randomIntervalRange > 0) {
                        currentInterval += Random.nextLong(-config.randomIntervalRange.toLong(), config.randomIntervalRange.toLong())
                    }
                    if (config.randomHoldRange > 0) {
                        currentHold += Random.nextLong(-config.randomHoldRange.toLong(), config.randomHoldRange.toLong())
                    }
                }

                // Clamp values and dispatch click! Use 10ms minimum hold time for hyper-speed performance.
                val worked = performClickSuspended(finalX, finalY, currentHold.coerceAtLeast(10))
                triggerVisualTap(finalX, finalY)

                if (worked) {
                    logExecution("[Real Access] Tap dispatched at ($finalX, $finalY)")
                } else {
                    logExecution("[Simulated Sandbox] Click pulsed at ($finalX, $finalY)")
                }

                repository.incrementActions("tap", 1)
                counter++

                if (config.stopConditionType == "clicks" && config.repeatCount > 0 && counter >= config.repeatCount) {
                    logExecution("Finished desired repeats limits ($counter).", force = true)
                    isAutomationActive = false
                    break
                }

                delay(currentInterval.coerceAtLeast(10))
            }
        }
    }

    // Trigger Multi-Tap automation sequence
    fun startMultiTapAutomation() {
        if (isAutomationActive) {
            stopAutomation()
            return
        }

        if (multiTapPoints.isEmpty()) {
            executionLog.value = "Error: Place at least one action point inside layout."
            return
        }

        val config = activeMultiTapPreset
        isAutomationActive = true
        executionLog.value = "Started Multi-Tap loop."

        val stopDurationMs = when (config.stopDurationUnit) {
            "seconds" -> config.stopDurationAmount * 1000L
            "minutes" -> config.stopDurationAmount * 60 * 1000L
            "hours" -> config.stopDurationAmount * 60 * 60 * 1000L
            else -> 0L
        }
        val startTime = System.currentTimeMillis()

        automationJob = viewModelScope.launch {
            var counter = 0
            var currentIndex = 0
            while (isRunningAndNotEmpty(multiTapPoints)) {
                if (getRemainingActions() <= 0) {
                    isAutomationActive = false
                    displayLimitDialog = true
                    break
                }

                // Check stop condition: duration
                if (config.stopConditionType == "duration" && stopDurationMs > 0) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed >= stopDurationMs) {
                        executionLog.value = "Finished: Stop condition met (duration limit)."
                        isAutomationActive = false
                        break
                    }
                }

                val selectionIndex = when (config.mode) {
                    "random_order" -> Random.nextInt(multiTapPoints.size)
                    "sequential", "loop" -> currentIndex % multiTapPoints.size
                    else -> currentIndex % multiTapPoints.size
                }

                val target = multiTapPoints[selectionIndex]
                var finalX = target.x
                var finalY = target.y
                var finalInterval = target.delayMs
                var finalHold = target.holdMs

                // Anti-detection: always add a subtle sub-pixel/micro-pixel jitter of (-1.5 to +1.5 pixels)
                // even when humanization config is off. This prevents identical click coordinate sequences,
                // which is the primary detection signature used by advanced anti-clicker games.
                val antiCheatJitterX = Random.nextFloat() * 3f - 1.5f
                val antiCheatJitterY = Random.nextFloat() * 3f - 1.5f
                finalX += antiCheatJitterX
                finalY += antiCheatJitterY

                if (config.humanTouchEnabled) {
                    if (config.microOffsetPx > 0) {
                        finalX += Random.nextInt(-config.microOffsetPx, config.microOffsetPx)
                        finalY += Random.nextInt(-config.microOffsetPx, config.microOffsetPx)
                    }
                    if (config.randomIntervalRange > 0) {
                        finalInterval += Random.nextLong(-config.randomIntervalRange.toLong(), config.randomIntervalRange.toLong())
                    }
                    if (config.randomHoldRange > 0) {
                        finalHold += Random.nextLong(-config.randomHoldRange.toLong(), config.randomHoldRange.toLong())
                    }
                }

                val worked = performClickSuspended(finalX, finalY, finalHold.coerceAtLeast(10))
                triggerVisualTap(finalX, finalY)

                if (worked) {
                    logExecution("[Real Access] Node $selectionIndex clicked ($finalX, $finalY)")
                } else {
                    logExecution("[Sim Sandbox] Node $selectionIndex pulse ($finalX, $finalY)")
                }

                repository.incrementActions("tap", 1)
                counter++

                if (config.stopConditionType == "clicks" && config.repeatCount > 0 && counter >= config.repeatCount && config.mode != "loop") {
                    logExecution("Multi Tap loop completed.", force = true)
                    isAutomationActive = false
                    break
                }

                currentIndex++
                delay(finalInterval.coerceAtLeast(10))
            }
        }
    }

    // Helper checking active status and collections constraints
    private fun isRunningAndNotEmpty(list: List<Any>): Boolean {
        return isAutomationActive && list.isNotEmpty()
    }

    // Flagship Area Tap engine! Includes allowed and blocked zones.
    fun startAreaTapAutomation() {
        if (isAutomationActive) {
            stopAutomation()
            return
        }

        if (areaTapZones.none { it.isAllowed }) {
            executionLog.value = "Declare at least one Allowed Zone (Green)."
            return
        }

        val config = activeAreaTapPreset
        isAutomationActive = true
        executionLog.value = "Flagship Area Tap Loop running stochastic engine..."

        val stopDurationMs = when (config.stopDurationUnit) {
            "seconds" -> config.stopDurationAmount * 1000L
            "minutes" -> config.stopDurationAmount * 60 * 1000L
            "hours" -> config.stopDurationAmount * 60 * 60 * 1000L
            else -> 0L
        }
        val startTime = System.currentTimeMillis()

        automationJob = viewModelScope.launch {
            var counter = 0
            while (isAutomationActive) {
                if (getRemainingActions() <= 0) {
                    isAutomationActive = false
                    displayLimitDialog = true
                    break
                }

                // Check stop condition: duration
                if (config.stopConditionType == "duration" && stopDurationMs > 0) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed >= stopDurationMs) {
                        executionLog.value = "Finished: Stop condition met (duration limit)."
                        isAutomationActive = false
                        break
                    }
                }

                // Weighted probability zone selection!
                val allowedZones = areaTapZones.filter { it.isAllowed }
                if (allowedZones.isEmpty()) {
                    isAutomationActive = false
                    break
                }

                val totalAllowedProb = allowedZones.sumOf { it.probability }.coerceAtLeast(1)
                var picker = Random.nextInt(totalAllowedProb)
                var chosenZone: TapZone = allowedZones.first()

                for (zone in allowedZones) {
                    picker -= zone.probability
                    if (picker < 0) {
                        chosenZone = zone
                        break
                    }
                }

                // Stochastic Target Location Generation
                var foundTarget = false
                var finalX = 0f
                var finalY = 0f
                var stochasticAttempts = 0

                // Generate random coordinate point inside chosen zone and check it doesn't intersect with any red blocked zones!
                while (!foundTarget && stochasticAttempts < 100) {
                    stochasticAttempts++
                    
                    // Generate according to Randomization Mode
                    val dx: Float
                    val dy: Float
                    
                    if (chosenZone.isRect) {
                        val hrw = chosenZone.width / 2f
                        val hrh = chosenZone.height / 2f
                        
                        when (config.mode) {
                            "center_weighted" -> {
                                dx = Random.nextFloat() * Random.nextFloat() * hrw * if (Random.nextBoolean()) 1f else -1f
                                dy = Random.nextFloat() * Random.nextFloat() * hrh * if (Random.nextBoolean()) 1f else -1f
                            }
                            else -> { // true random & balanced modes
                                val minX = (-hrw).toInt()
                                val maxX = hrw.toInt()
                                val minY = (-hrh).toInt()
                                val maxY = hrh.toInt()
                                dx = if (maxX > minX) Random.nextInt(minX, maxX).toFloat() else 0f
                                dy = if (maxY > minY) Random.nextInt(minY, maxY).toFloat() else 0f
                            }
                        }
                        
                        finalX = chosenZone.x + dx
                        finalY = chosenZone.y + dy
                    } else {
                        val r = chosenZone.radius
                        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                        
                        val distance = when (config.mode) {
                            "center_weighted" -> {
                                Random.nextFloat() * Random.nextFloat() * r
                            }
                            else -> {
                                Random.nextFloat() * r
                            }
                        }
                        
                        finalX = chosenZone.x + (distance * Math.cos(angle.toDouble()).toFloat())
                        finalY = chosenZone.y + (distance * Math.sin(angle.toDouble()).toFloat())
                    }

                    // Check if generated coordinates fall within any active red blocked zones!
                    var isBlocked = false
                    val blockedZones = areaTapZones.filter { !it.isAllowed }
                    
                    for (block in blockedZones) {
                        if (block.isRect) {
                            val minX = block.x - block.width/2
                            val maxX = block.x + block.width/2
                            val minY = block.y - block.height/2
                            val maxY = block.y + block.height/2
                            if (finalX in minX..maxX && finalY in minY..maxY) {
                                isBlocked = true
                                break
                            }
                        } else {
                            val dist = Math.hypot((finalX - block.x).toDouble(), (finalY - block.y).toDouble())
                            if (dist < block.radius) {
                                isBlocked = true
                                break
                            }
                        }
                    }

                    if (!isBlocked) {
                        foundTarget = true
                    }
                }

                if (foundTarget) {
                    val worked = performClickSuspended(finalX, finalY, config.holdMs.coerceAtLeast(10))
                    triggerVisualTap(finalX, finalY)

                    if (worked) {
                        logExecution("[Real Zone] Clicked inside zone on safely cleared Target ($finalX, $finalY).")
                    } else {
                        logExecution("[Sim Zone] stochastic pulse inside Allowed spot ($finalX, $finalY).")
                    }

                    repository.incrementActions("tap", 1)
                    counter++
                } else {
                    logExecution("[Safety Warning] No safe target path found. Blocked by overlapping Red Zones!", force = true)
                }

                if (config.stopConditionType == "clicks" && config.repeatCount > 0 && counter >= config.repeatCount) {
                    logExecution("Finished desired repeats.", force = true)
                    isAutomationActive = false
                    break
                }

                delay(config.intervalMs.coerceAtLeast(10))
            }
        }
    }

    // Trigger Swipe gesture automation sequence
    fun startSwipeAutomation() {
        if (isAutomationActive) {
            stopAutomation()
            return
        }

        val config = activeSwipePreset
        isAutomationActive = true
        executionLog.value = "Started Swipe Automation Loop..."

        val stopDurationMs = when (config.stopDurationUnit) {
            "seconds" -> config.stopDurationAmount * 1000L
            "minutes" -> config.stopDurationAmount * 60 * 1000L
            "hours" -> config.stopDurationAmount * 60 * 60 * 1000L
            else -> 0L
        }
        val startTime = System.currentTimeMillis()

        automationJob = viewModelScope.launch {
            var counter = 0
            while (isAutomationActive) {
                if (getRemainingActions() <= 0) {
                    isAutomationActive = false
                    displayLimitDialog = true
                    break
                }

                // Check stop condition: duration
                if (config.stopConditionType == "duration" && stopDurationMs > 0) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed >= stopDurationMs) {
                        executionLog.value = "Finished: Stop condition met (duration limit)."
                        isAutomationActive = false
                        break
                    }
                }

                var sx = swipeCoordinates.startX
                var sy = swipeCoordinates.startY
                var ex = swipeCoordinates.endX
                var ey = swipeCoordinates.endY
                var waitTime = config.intervalMs

                if (config.humanTouchEnabled) {
                    if (config.microOffsetPx > 0) {
                        sx += Random.nextInt(-config.microOffsetPx, config.microOffsetPx)
                        sy += Random.nextInt(-config.microOffsetPx, config.microOffsetPx)
                        ex += Random.nextInt(-config.microOffsetPx, config.microOffsetPx)
                        ey += Random.nextInt(-config.microOffsetPx, config.microOffsetPx)
                    }
                    if (config.randomIntervalRange > 0) {
                        waitTime += Random.nextLong(-config.randomIntervalRange.toLong(), config.randomIntervalRange.toLong())
                    }
                }

                val worked = performSwipeSuspended(sx, sy, ex, ey, config.holdMs.coerceAtLeast(10))

                if (worked) {
                    logExecution("[Real Swipe] Dispatched ($sx, $sy) -> ($ex, $ey)")
                } else {
                    logExecution("[Sim Swipe] Drag outline drawn from ($sx, $sy) to ($ex, $ey)")
                }

                repository.incrementActions("swipe", 1)
                counter++

                if (config.stopConditionType == "clicks" && config.repeatCount > 0 && counter >= config.repeatCount) {
                    logExecution("Finished swipes sequence.", force = true)
                    isAutomationActive = false
                    break
                }

                delay(waitTime.coerceAtLeast(10))
            }
        }
    }

    // Trigger sequential scenarios automation sequence
    fun startScenarioAutomation() {
        if (isAutomationActive) {
            stopAutomation()
            return
        }

        if (scenarioSteps.isEmpty()) {
            executionLog.value = "Empty chain. Add steps first."
            return
        }

        isAutomationActive = true
        executionLog.value = "Running complete automation scenario: $activeScenarioName"

        automationJob = viewModelScope.launch {
            var loopIndex = 0
            while (isAutomationActive) {
                for (step in scenarioSteps) {
                    if (!isAutomationActive) break
                    
                    if (getRemainingActions() <= 0) {
                        isAutomationActive = false
                        displayLimitDialog = true
                        break
                    }

                    executionLog.value = "Executing step: ${step.type} (${step.durationMs}ms)"

                    val service = NovaTapAccessibilityService.instance

                    when (step.type) {
                        "tap" -> {
                            val worked = performClickSuspended(step.x, step.y, 50)
                            triggerVisualTap(step.x, step.y)
                            if (worked) {
                                logExecution("[Real Scenario] Step Tap Clicked Coordinate (${step.x}, ${step.y})")
                            } else {
                                logExecution("[Sim Scenario] Step Tap pulsed coordinate (${step.x}, ${step.y})")
                            }
                            repository.incrementActions("scenario", 1)
                        }
                        "area" -> {
                            // Find any active allowed zone, stochastic tap in it
                            val allowedZone = areaTapZones.find { it.isAllowed }
                            if (allowedZone != null) {
                                val rx = allowedZone.x + Random.nextInt(-30, 30)
                                val ry = allowedZone.y + Random.nextInt(-30, 30)
                                val worked = performClickSuspended(rx, ry, 50)
                                triggerVisualTap(rx, ry)
                                if (worked) {
                                    logExecution("[Real Scenario] Step Area clicked inside allowed space ($rx, $ry)")
                                } else {
                                    logExecution("[Sim Scenario] Step Area pulsed inside allowed space ($rx, $ry)")
                                }
                                repository.incrementActions("scenario", 1)
                            } else {
                                logExecution("Skip Area step: No area zones configured in dashboard", force = true)
                            }
                        }
                        "swipe" -> {
                            val worked = performSwipeSuspended(100f, 800f, 800f, 100f, 350)
                            if (worked) {
                                logExecution("[Real Scenario] Step Swipe executed.")
                            } else {
                                logExecution("[Sim Scenario] Step Swipe emulated.")
                            }
                            repository.incrementActions("scenario", 1)
                        }
                        "wait" -> {
                            delay(step.durationMs.coerceAtLeast(10))
                        }
                        "loop" -> {
                            logExecution("Scenario looping back.", force = true)
                            // Simple continue is loop behavior
                        }
                    }

                    delay(200) // Small breather between scenario nodes
                }

                loopIndex++
                if (!isAdFreeUser && loopIndex > 2) {
                    executionLog.value = "Limit Alert: Free user scenarios loop capped at 2 runs maximum to prevent process loops."
                    isAutomationActive = false
                }
                
                delay(1000)
            }
        }
    }
}
