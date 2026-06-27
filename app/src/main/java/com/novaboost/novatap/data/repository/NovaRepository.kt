package com.novaboost.novatap.data.repository

import com.novaboost.novatap.data.database.PresetDao
import com.novaboost.novatap.data.database.ScenarioDao
import com.novaboost.novatap.data.database.SettingDao
import com.novaboost.novatap.data.database.StatisticDao
import com.novaboost.novatap.data.database.CrashLogDao
import com.novaboost.novatap.data.model.AppSetting
import com.novaboost.novatap.data.model.DailyStatistic
import com.novaboost.novatap.data.model.Preset
import com.novaboost.novatap.data.model.Scenario
import com.novaboost.novatap.data.model.CrashLog
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NovaRepository(
    private val presetDao: PresetDao,
    private val scenarioDao: ScenarioDao,
    private val settingDao: SettingDao,
    private val statisticDao: StatisticDao,
    private val crashLogDao: CrashLogDao
) {
    val allPresets: Flow<List<Preset>> = presetDao.getAllPresets()
    val allScenarios: Flow<List<Scenario>> = scenarioDao.getAllScenarios()
    val allSettings: Flow<List<AppSetting>> = settingDao.getAllSettingsFlow()
    val allCrashLogs: Flow<List<CrashLog>> = crashLogDao.getAllCrashLogs()

    suspend fun insertCrashLog(log: CrashLog) {
        try {
            crashLogDao.insertCrashLog(log)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearCrashLogs() {
        try {
            crashLogDao.clearCrashLogs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    // Preset operations
    suspend fun insertPreset(preset: Preset): Long = presetDao.insertPreset(preset)
    suspend fun getPresetById(id: Int): Preset? = presetDao.getPresetById(id)
    suspend fun deletePresetById(id: Int) = presetDao.deletePresetById(id)

    // Scenario operations
    suspend fun insertScenario(scenario: Scenario): Long = scenarioDao.insertScenario(scenario)
    suspend fun getScenarioById(id: Int): Scenario? = scenarioDao.getScenarioById(id)
    suspend fun deleteScenarioById(id: Int) = scenarioDao.deleteScenarioById(id)

    // Settings operations
    suspend fun saveSetting(key: String, value: String) {
        settingDao.insertSetting(AppSetting(key, value))
    }

    suspend fun getSettingValue(key: String, defaultValue: String): String {
        return settingDao.getSettingByKey(key)?.value ?: defaultValue
    }

    // Statistics operations (with lazy daily row insertion)
    fun getDailyStatisticFlow(): Flow<DailyStatistic?> {
        val today = getCurrentDate()
        return statisticDao.getStatisticByDateFlow(today)
    }

    suspend fun incrementActions(actionType: String, amount: Long) {
        val today = getCurrentDate()
        var stat = statisticDao.getStatisticByDate(today)
        if (stat == null) {
            stat = DailyStatistic(date = today)
            statisticDao.insertStatistic(stat)
        }

        when (actionType) {
            "tap" -> statisticDao.incrementTaps(today, amount)
            "swipe" -> statisticDao.incrementSwipes(today, amount)
            "scenario" -> statisticDao.incrementScenarios(today, amount)
            else -> {
                val updated = stat.copy(
                    totalActions = stat.totalActions + amount
                )
                statisticDao.insertStatistic(updated)
            }
        }
    }

    suspend fun resetTodayStatistics() {
        statisticDao.nukeStatistics()
        val today = getCurrentDate()
        statisticDao.insertStatistic(DailyStatistic(date = today))
    }
}
