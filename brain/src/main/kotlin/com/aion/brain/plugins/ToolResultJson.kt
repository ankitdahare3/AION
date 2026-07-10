package com.aion.brain.plugins

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Safely builds `{"observation": ...}` — raw string interpolation would break on quotes in screen text. */
internal fun observationJson(observation: String): String =
    JsonObject(mapOf("observation" to JsonPrimitive(observation))).toString()
