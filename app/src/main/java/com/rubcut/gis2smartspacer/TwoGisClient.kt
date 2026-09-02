package com.rubcut.gis2smartspacer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class GeoPoint(val lat: Double, val lon: Double)

/**
 * Обёртка над публичными cloud-эндпоинтами 2ГИС:
 *  - Geocoder API (catalog.api.2gis.com/3.0/items/geocode)
 *  - Routing API v7 (routing.api.2gis.com/routing/7.0.0/global) — авто и пешком
 *  - Public Transport API v2 (routing.api.2gis.com/public_transport/2.0) — транспорт
 *
 * Важно: точные названия полей в ответе Public Transport API стоит сверить
 * с актуальным API Reference на docs.2gis.com — здесь используется
 * отказоустойчивый разбор (ищем поле total_duration в любом месте ответа),
 * чтобы не сломаться при мелких отличиях схемы.
 */
class TwoGisClient(private val apiKey: String) {

    companion object {
        private const val TAG = "TwoGisClient"
        private const val GEOCODE_URL = "https://catalog.api.2gis.com/3.0/items/geocode"
        private const val ROUTING_URL = "https://routing.api.2gis.com/routing/7.0.0/global"
        private const val TRANSIT_URL = "https://routing.api.2gis.com/public_transport/2.0"
        private const val TIMEOUT_MS = 15000
    }

    /** Геокодирование адреса в координаты. Возвращает null при ошибке. */
    suspend fun geocode(address: String): GeoPoint? = withContext(Dispatchers.IO) {
        try {
            val q = encode(address)
            val url = "$GEOCODE_URL?q=$q&fields=items.point&key=${encode(apiKey)}"
            val response = httpGet(url) ?: return@withContext null
            val json = JSONObject(response)
            val items = json.optJSONObject("result")?.optJSONArray("items") ?: return@withContext null
            if (items.length() == 0) return@withContext null
            val point = items.getJSONObject(0).optJSONObject("point") ?: return@withContext null
            GeoPoint(point.getDouble("lat"), point.getDouble("lon"))
        } catch (e: Exception) {
            Log.w(TAG, "geocode failed", e)
            null
        }
    }

    /** Время в пути в секундах для авто/пешком через /routing/7.0.0/global. */
    suspend fun routeDuration(
        mode: TravelMode,
        from: GeoPoint,
        to: GeoPoint
    ): Int? = withContext(Dispatchers.IO) {
        if (mode == TravelMode.TRANSIT) return@withContext transitDuration(from, to)
        try {
            val transport = if (mode == TravelMode.DRIVING) "driving" else "walking"
            val body = JSONObject().apply {
                put("points", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "walking"); put("lat", from.lat); put("lon", from.lon)
                    })
                    put(JSONObject().apply {
                        put("type", "walking"); put("lat", to.lat); put("lon", to.lon)
                    })
                })
                put("transport", transport)
                put("output", "summary")
                put("locale", "ru")
            }
            val url = "$ROUTING_URL?key=${encode(apiKey)}"
            val response = httpPost(url, body.toString()) ?: return@withContext null
            findFirstDuration(JSONTokener(response).nextValue())
        } catch (e: Exception) {
            Log.w(TAG, "routeDuration failed", e)
            null
        }
    }

    private suspend fun transitDuration(from: GeoPoint, to: GeoPoint): Int? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("source", JSONObject().apply {
                    put("point", JSONObject().apply { put("lat", from.lat); put("lon", from.lon) })
                })
                put("target", JSONObject().apply {
                    put("point", JSONObject().apply { put("lat", to.lat); put("lon", to.lon) })
                })
                put("locale", "ru")
                put("transport", JSONArray().apply {
                    listOf(
                        "pedestrian", "metro", "light_metro", "suburban_train",
                        "aeroexpress", "tram", "bus", "trolleybus", "shuttle_bus",
                        "monorail", "funicular_railway", "river_transport", "cable_car",
                        "light_rail", "premetro", "mcc", "mcd"
                    ).forEach(::put)
                })
                put("max_result_count", 1)
            }
            val url = "$TRANSIT_URL?key=${encode(apiKey)}"
            val response = httpPost(url, body.toString()) ?: return@withContext null
            // Public Transport API returns a root JSON array, unlike Routing API.
            findFirstDuration(JSONTokener(response).nextValue())
        } catch (e: Exception) {
            Log.w(TAG, "transitDuration failed", e)
            null
        }
    }

    /**
     * Рекурсивно ищет первое числовое поле "total_duration" (или "duration")
     * в произвольно вложенном JSON-ответе — так код переживёт небольшие
     * отличия схемы между /routing и /public_transport.
     */
    private fun findFirstDuration(json: Any?): Int? {
        when (json) {
            is JSONObject -> {
                for (key in listOf("total_duration", "duration")) {
                    if (json.has(key)) {
                        val value = json.opt(key)
                        if (value is Number) return value.toInt()
                    }
                }
                val keys = json.keys()
                while (keys.hasNext()) {
                    val result = findFirstDuration(json.get(keys.next()))
                    if (result != null) return result
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    val result = findFirstDuration(json.get(i))
                    if (result != null) return result
                }
            }
        }
        return null
    }

    private fun httpGet(urlString: String): String? {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            readResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun httpPost(urlString: String, body: String): String? {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use {
                it.write(body)
            }
            readResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponse(connection: HttpURLConnection): String? {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.let {
            BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)).use { reader -> reader.readText() }
        }.orEmpty()
        if (code !in 200..299) {
            Log.w(TAG, "HTTP $code: $text")
            return null
        }
        return text.takeIf(String::isNotBlank)
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
