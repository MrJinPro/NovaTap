package com.novaboost.novatap.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.novaboost.novatap.data.model.AppSetting
import com.novaboost.novatap.data.model.CrashLog
import com.novaboost.novatap.data.model.DailyStatistic
import com.novaboost.novatap.data.model.Preset
import com.novaboost.novatap.data.model.Scenario

@Database(
    entities = [
        Preset::class,
        Scenario::class,
        AppSetting::class,
        DailyStatistic::class,
        CrashLog::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun scenarioDao(): ScenarioDao
    abstract fun settingDao(): SettingDao
    abstract fun statisticDao(): StatisticDao
    abstract fun crashLogDao(): CrashLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "novatap_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
