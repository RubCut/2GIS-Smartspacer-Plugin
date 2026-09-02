package com.rubcut.gis2smartspacer

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerComplicationUpdateReceiver
import com.rubcut.gis2smartspacer.complications.CarEtaComplication
import com.rubcut.gis2smartspacer.complications.TransitEtaComplication
import com.rubcut.gis2smartspacer.complications.WalkEtaComplication

class EtaComplicationUpdateReceiver : SmartspacerComplicationUpdateReceiver() {

    override fun onRequestSmartspaceComplicationUpdate(
        context: Context,
        requestComplications: List<RequestComplication>
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Every requested Complication instance has its own settings
                // (key, destination), so each one is refreshed independently.
                val repository = SettingsRepository(context)
                val requests = requestComplications
                    .mapNotNull { request ->
                        Constants.modeForAuthority(request.authority)?.let {
                            request.smartspacerId to it
                        }
                    }
                    .groupBy({ it.first }, { it.second })
                coroutineScope {
                    requests.map { (smartspacerId, modes) ->
                        async {
                            val settings = repository.forComplication(smartspacerId)
                            if (settings.isConfigured) {
                                EtaUpdater.refresh(context, settings, modes.toSet())
                            } else {
                                false
                            }
                        }
                    }.awaitAll()
                }
            } finally {
                requestComplications.forEach { request ->
                    notifyComplication(context, request.authority, request.smartspacerId)
                }
                pendingResult.finish()
            }
        }
    }

    private fun notifyComplication(context: Context, authority: String, smartspacerId: String) {
        when (authority) {
            Constants.AUTHORITY_CAR -> SmartspacerComplicationProvider.notifyChange(
                context, CarEtaComplication::class.java, smartspacerId
            )
            Constants.AUTHORITY_WALK -> SmartspacerComplicationProvider.notifyChange(
                context, WalkEtaComplication::class.java, smartspacerId
            )
            Constants.AUTHORITY_TRANSIT -> SmartspacerComplicationProvider.notifyChange(
                context, TransitEtaComplication::class.java, smartspacerId
            )
            else -> {
                SmartspacerComplicationProvider.notifyChange(
                    context, CarEtaComplication::class.java, smartspacerId
                )
                SmartspacerComplicationProvider.notifyChange(
                    context, WalkEtaComplication::class.java, smartspacerId
                )
                SmartspacerComplicationProvider.notifyChange(
                    context, TransitEtaComplication::class.java, smartspacerId
                )
            }
        }
    }
}
