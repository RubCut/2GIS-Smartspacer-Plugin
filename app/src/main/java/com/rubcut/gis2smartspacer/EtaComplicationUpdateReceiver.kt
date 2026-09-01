package com.rubcut.gis2smartspacer

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerComplicationUpdateReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.rubcut.gis2smartspacer.complications.CarEtaComplication
import com.rubcut.gis2smartspacer.complications.TransitEtaComplication
import com.rubcut.gis2smartspacer.complications.WalkEtaComplication

/**
 * Smartspacer шлёт сюда широковещательный запрос на обновление раз в
 * REFRESH_PERIOD_MINUTES минут (см. Config.refreshPeriodMinutes у каждого
 * Complication-провайдера). Именно здесь, а не в getSmartspaceActions(),
 * можно спокойно делать сетевые запросы.
 */
class EtaComplicationUpdateReceiver : SmartspacerComplicationUpdateReceiver() {

    override fun onRequestSmartspaceComplicationUpdate(
        context: Context,
        requestComplications: List<RequestComplication>
    ) {
        val pendingResult = goAsync()
        val settings = SettingsRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (settings.isConfigured) {
                    refreshAll(context, settings)
                }
            } finally {
                // Уведомляем каждый запрошенный Complication, что данные могли обновиться
                requestComplications.forEach { request ->
                    when (request.authority) {
                        Constants.AUTHORITY_CAR ->
                            SmartspacerComplicationProvider.notifyChange(context, CarEtaComplication::class.java, request.smartspacerId)
                        Constants.AUTHORITY_WALK ->
                            SmartspacerComplicationProvider.notifyChange(context, WalkEtaComplication::class.java, request.smartspacerId)
                        Constants.AUTHORITY_TRANSIT ->
                            SmartspacerComplicationProvider.notifyChange(context, TransitEtaComplication::class.java, request.smartspacerId)
                    }
                }
                pendingResult.finish()
            }
        }
    }

    private suspend fun refreshAll(context: Context, settings: SettingsRepository) {
        val origin = LocationHelper.getLastKnownLocation(context) ?: return
        val destination = GeoPoint(settings.destLat, settings.destLon)
        val client = TwoGisClient(settings.apiKey)

        for (mode in TravelMode.values()) {
            val seconds = client.routeDuration(mode, origin, destination)
            settings.setEtaMinutes(mode, seconds?.let { (it / 60.0).let { m -> kotlin.math.ceil(m).toInt() } })
        }
    }
}
