package com.novaboost.novatap.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "single", "multi", "area", "swipe"
    val intervalMs: Long = 200,
    val holdMs: Long = 50,
    val repeatCount: Int = 0,
    val mode: String = "sequential", // "sequential", "random_order", "loop"
    val pointsJson: String = "[]",
    val zonesJson: String = "[]",
    val swipeStartAndEnd: String = "{}",
    val humanTouchEnabled: Boolean = false,
    val microOffsetPx: Int = 10,
    val randomIntervalRange: Int = 50,
    val randomHoldRange: Int = 20,
    val timestamp: Long = System.currentTimeMillis(),
    val stopConditionType: String = "infinite", // "infinite", "duration", "clicks"
    val stopDurationAmount: Long = 10,
    val stopDurationUnit: String = "seconds" // "seconds", "minutes", "hours"
)

@Entity(tableName = "scenarios")
data class Scenario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val stepsJson: String = "[]",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "statistics")
data class DailyStatistic(
    @PrimaryKey val date: String, // e.g. "2026-06-20"
    val tapCount: Long = 0,
    val swipeCount: Long = 0,
    val scenarioCount: Long = 0,
    val totalActions: Long = 0
)

@Entity(tableName = "crash_logs")
data class CrashLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val error: String,
    val stacktrace: String,
    val screenName: String,
    val androidVersion: String = android.os.Build.VERSION.RELEASE,
    val deviceModel: String = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
)

// In-Memory Representation Models used in JSON Serialization:
data class TapPoint(
    val id: String = java.util.UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val label: String = "",
    val delayMs: Long = 200,
    val holdMs: Long = 50
)

data class TapZone(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isAllowed: Boolean, // true = Allowed (Green), false = Blocked (Red)
    val isRect: Boolean, // true = Rectangle, false = Circle
    val x: Float,
    val y: Float,
    val width: Float = 150f,
    val height: Float = 150f,
    val radius: Float = 75f,
    val probability: Int = 100
)

data class VisualTapEvent(
    val id: Long = java.util.UUID.randomUUID().mostSignificantBits,
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class SwipeCoordinates(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val durationMs: Long = 300,
    val isHumanized: Boolean = true
)

data class ScenarioStep(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String, // "tap", "area", "swipe", "wait", "loop"
    val durationMs: Long = 200, // delay/duration/wait duration
    val x: Float = 0f,
    val y: Float = 0f,
    val stepLabel: String = ""
)
