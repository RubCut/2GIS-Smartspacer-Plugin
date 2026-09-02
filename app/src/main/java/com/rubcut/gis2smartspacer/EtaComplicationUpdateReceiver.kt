package com.rubcut.gis2smartspacer

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerComplicationUpdateReceiver
import com.rubcut.gis2smartspacer.complications.CarEtaComplication
import com.rubcut.gis2smartspacer.complications.TransitEtaComplication
import com.rubcut.gis2smartspacer.complications.WalkEtaComplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EtaComplicationUpdateReceiver : SmartspacerComplicationUpdateReceiver() {

    override fun onRequestSmartspaceComplicationUpdate(
        context: Context,
        requestComplications: List<RequestComplication>
    ) {
        val pendingResult = goAsync()
        val settings = SettingsRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestedModes = requestComplications.mapNotNull { request ->
                    when (request.authority) {
                        Constants.AUTHORITY_CAR -> TravelMode.DRIVING
                        Constants.AUTHORITY_WALK -> TravelMode.WALKING
                        Constants.AUTHORITY_TRANSIT -> TravelMode.TRANSIT
                        else -> null
                    }
                }.toSet()
                EtaUpdater.refresh(context, settings, requestedModes)
            } finally {
                requestComplications.forEach { request ->
                    when (request.authority) {
                        Constants.AUTHORITY_CAR -> SmartspacerComplicationProvider.notifyChange(
                            context, CarEtaComplication::class.java, request.smartspacerId
                        )
                        Constants.AUTHORITY_WALK -> SmartspacerComplicationProvider.notifyChange(
                            context, WalkEtaComplication::class.java, request.smartspacerId
                        )
                        Constants.AUTHORITY_TRANSIT -> SmartspacerComplicationProvider.notifyChange(
                            context, TransitEtaComplication::class.java, request.smartspacerId
                        )
                    }
                }
                pendingResult.finish()
            }
        }
    }
}
