package com.novaboost.novatap.data.model

import org.json.JSONArray
import org.json.JSONObject

object JsonHelpers {

    fun serializeTapPoints(points: List<TapPoint>): String {
        val array = JSONArray()
        for (p in points) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("x", p.x.toDouble())
            obj.put("y", p.y.toDouble())
            obj.put("label", p.label)
            obj.put("delayMs", p.delayMs)
            obj.put("holdMs", p.holdMs)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeTapPoints(json: String): List<TapPoint> {
        val list = mutableListOf<TapPoint>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TapPoint(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        x = obj.optDouble("x", 0.0).toFloat(),
                        y = obj.optDouble("y", 0.0).toFloat(),
                        label = obj.optString("label", ""),
                        delayMs = obj.optLong("delayMs", 200),
                        holdMs = obj.optLong("holdMs", 50)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeTapZones(zones: List<TapZone>): String {
        val array = JSONArray()
        for (z in zones) {
            val obj = JSONObject()
            obj.put("id", z.id)
            obj.put("isAllowed", z.isAllowed)
            obj.put("isRect", z.isRect)
            obj.put("x", z.x.toDouble())
            obj.put("y", z.y.toDouble())
            obj.put("width", z.width.toDouble())
            obj.put("height", z.height.toDouble())
            obj.put("radius", z.radius.toDouble())
            obj.put("probability", z.probability)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeTapZones(json: String): List<TapZone> {
        val list = mutableListOf<TapZone>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TapZone(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        isAllowed = obj.optBoolean("isAllowed", true),
                        isRect = obj.optBoolean("isRect", true),
                        x = obj.optDouble("x", 0.0).toFloat(),
                        y = obj.optDouble("y", 0.0).toFloat(),
                        width = obj.optDouble("width", 150.0).toFloat(),
                        height = obj.optDouble("height", 150.0).toFloat(),
                        radius = obj.optDouble("radius", 75.0).toFloat(),
                        probability = obj.optInt("probability", 100)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeSwipeCoordinates(data: SwipeCoordinates): String {
        val obj = JSONObject()
        obj.put("startX", data.startX.toDouble())
        obj.put("startY", data.startY.toDouble())
        obj.put("endX", data.endX.toDouble())
        obj.put("endY", data.endY.toDouble())
        obj.put("durationMs", data.durationMs)
        obj.put("isHumanized", data.isHumanized)
        return obj.toString()
    }

    fun deserializeSwipeCoordinates(json: String): SwipeCoordinates {
        try {
            val obj = JSONObject(json)
            return SwipeCoordinates(
                startX = obj.optDouble("startX", 100.0).toFloat(),
                startY = obj.optDouble("startY", 100.0).toFloat(),
                endX = obj.optDouble("endX", 500.0).toFloat(),
                endY = obj.optDouble("endY", 500.0).toFloat(),
                durationMs = obj.optLong("durationMs", 300),
                isHumanized = obj.optBoolean("isHumanized", true)
            )
        } catch (e: Exception) {
            return SwipeCoordinates(100f, 100f, 500f, 500f)
        }
    }

    fun serializeScenarioSteps(steps: List<ScenarioStep>): String {
        val array = JSONArray()
        for (s in steps) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("type", s.type)
            obj.put("durationMs", s.durationMs)
            obj.put("x", s.x.toDouble())
            obj.put("y", s.y.toDouble())
            obj.put("stepLabel", s.stepLabel)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeScenarioSteps(json: String): List<ScenarioStep> {
        val list = mutableListOf<ScenarioStep>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ScenarioStep(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        type = obj.optString("type", "tap"),
                        durationMs = obj.optLong("durationMs", 200),
                        x = obj.optDouble("x", 0.0).toFloat(),
                        y = obj.optDouble("y", 0.0).toFloat(),
                        stepLabel = obj.optString("stepLabel", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
