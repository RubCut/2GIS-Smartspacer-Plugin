package com.rubcut.gis2smartspacer

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.rubcut.gis2smartspacer.complications.CarEtaComplication
import com.rubcut.gis2smartspacer.complications.TransitEtaComplication
import com.rubcut.gis2smartspacer.complications.WalkEtaComplication

/**
 * Tells Smartspacer that a Complication instance changed. When the authority
 * extra is unknown, all three providers are notified to stay on the safe side
 * (for example when the settings screen was opened without Smartspacer extras).
 */
object ComplicationNotifier {

    fun notifyChange(context: Context, authority: String?, smartspacerId: String) {
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
