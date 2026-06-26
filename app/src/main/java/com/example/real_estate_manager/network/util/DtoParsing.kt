package com.example.real_estate_manager.network.util

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken

inline fun <reified T> Gson.decodeListEnvelope(element: JsonElement): List<T> {
    val payload = if (element.isJsonObject && element.asJsonObject.has("results")) {
        element.asJsonObject.get("results")
    } else {
        element
    }
    return fromJson(payload, object : TypeToken<List<T>>() {}.type)
}

fun JsonElement?.extractId(): String? {
    if (this == null || isJsonNull) return null
    return when {
        isJsonPrimitive -> asJsonPrimitive.asString
        isJsonObject -> {
            val obj = asJsonObject
            when {
                obj.has("id") -> obj.get("id").asString
                obj.has("uuid") -> obj.get("uuid").asString
                obj.has("pk") -> obj.get("pk").asString
                else -> null
            }
        }
        else -> null
    }
}
