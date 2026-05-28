package com.Azelmods.App.data.local

import org.json.JSONArray
import org.json.JSONObject

/**
 * Helper functions to convert between complex types and JSON strings
 * for Room storage (since Room doesn't support Map/List natively).
 */
object Converters {

    // ── Map<String, String> ────────────────────────────────

    fun mapToString(map: Map<String, String>?): String {
        if (map.isNullOrEmpty()) return "{}"
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    fun stringToMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { obj.getString(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ── Map<String, Boolean> ───────────────────────────────

    fun booleanMapToString(map: Map<String, Boolean>?): String {
        if (map.isNullOrEmpty()) return "{}"
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    fun stringToBooleanMap(json: String?): Map<String, Boolean> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { obj.optBoolean(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ── List<String> ──────────────────────────────────────

    fun listToString(list: List<String>?): String {
        if (list.isNullOrEmpty()) return "[]"
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    fun stringToStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
