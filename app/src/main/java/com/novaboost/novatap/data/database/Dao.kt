package com.novaboost.novatap.data.database

import androidx.room.*
import com.novaboost.novatap.data.model.AppSetting
import com.novaboost.novatap.data.model.CrashLog
import com.novaboost.novatap.data.model.DailyStatistic
import com.novaboost.novatap.data.model.Preset
import com.novaboost.novatap.data.model.Scenario
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY timestamp DESC")
    fun getAllPresets(): Flow<List<Preset>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getPresetById(id: Int): Preset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: Preset): Long

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deletePresetById(id: Int)
}

@Dao
interface ScenarioDao {
    @Query("SELECT * FROM scenarios ORDER BY timestamp DESC")
    fun getAllScenarios(): Flow<List<Scenario>>

    @Query("SELECT * FROM scenarios WHERE id = :id")
    suspend fun getScenarioById(id: Int): Scenario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenario(scenario: Scenario): Long

    @Query("DELETE FROM scenarios WHERE id = :id")
    suspend fun deleteScenarioById(id: Int)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings")
    fun getAllSettingsFlow(): Flow<List<AppSetting>>

    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getSettingByKey(key: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteSettingByKey(key: String)
}

@Dao
interface StatisticDao {
    @Query("SELECT * FROM statistics WHERE date = :date")
    suspend fun getStatisticByDate(date: String): DailyStatistic?

    @Query("SELECT * FROM statistics WHERE date = :date")
    fun getStatisticByDateFlow(date: String): Flow<DailyStatistic?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistic(statistic: DailyStatistic)

    @Query("UPDATE statistics SET tapCount = tapCount + :count, totalActions = totalActions + :count WHERE date = :date")
    suspend fun incrementTaps(date: String, count: Long)

    @Query("UPDATE statistics SET swipeCount = swipeCount + :count, totalActions = totalActions + :count WHERE date = :date")
    suspend fun incrementSwipes(date: String, count: Long)

    @Query("UPDATE statistics SET scenarioCount = scenarioCount + :count, totalActions = totalActions + :count WHERE date = :date")
    suspend fun incrementScenarios(date: String, count: Long)

    @Query("DELETE FROM statistics")
    suspend fun nukeStatistics()
}

@Dao
interface CrashLogDao {
    @Query("SELECT * FROM crash_logs ORDER BY timestamp DESC")
    fun getAllCrashLogs(): Flow<List<CrashLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrashLog(log: CrashLog)

    @Query("DELETE FROM crash_logs")
    suspend fun clearCrashLogs()
}
