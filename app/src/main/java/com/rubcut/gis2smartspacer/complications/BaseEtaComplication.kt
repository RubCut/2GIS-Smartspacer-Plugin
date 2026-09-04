package com.rubcut.gis2smartspacer.complications

import android.content.Intent
import android.graphics.drawable.Icon as AndroidIcon
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider.Config
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.rubcut.gis2smartspacer.Constants
import com.rubcut.gis2smartspacer.EtaFormatter
import com.rubcut.gis2smartspacer.SettingsActivity
import com.rubcut.gis2smartspacer.SettingsRepository
import com.rubcut.gis2smartspacer.TapActionMode
import com.rubcut.gis2smartspacer.TravelMode
import com.rubcut.gis2smartspacer.UpdateEtaActivity

abstract class BaseEtaComplication(
    private val mode: TravelMode,
    private val iconRes: Int
) : SmartspacerComplicationProvider() {

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val context = provideContext()
        // Settings belong to this exact Complication instance.
        val settings = SettingsRepository(context).forComplication(smartspacerId)
        val tapAction = settings.tapAction
        val minutes = settings.getEtaMinutes(mode)
        val content = when {
            !settings.isConfigured -> context.getString(
                com.rubcut.gis2smartspacer.R.string.complication_needs_setup
            )
            minutes == null -> context.getString(
                com.rubcut.gis2smartspacer.R.string.complication_no_eta
            )
            else -> EtaFormatter.formatMinutes(context, minutes)
        }

        return listOf(
            ComplicationTemplate.Basic(
                // The chosen tap target is part of the action identity. Smartspacer
                // matches the rendered action by this id, so when the user switches
                // from "Settings" to "Update ETA" (and back) the id changes and the
                // new onClick intent is bound; otherwise the tap would keep firing the
                // previously stored (Settings) intent.
                id = "eta_${mode.name.lowercase()}_${tapAction.name.lowercase()}_$smartspacerId",
                icon = Icon(AndroidIcon.createWithResource(context, iconRes), shouldTint = true),
                content = Text(content),
                onClick = TapAction(
                    intent = Intent(context, tapTarget(tapAction)).apply {
                        putExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID, smartspacerId)
                        putExtra(
                            SmartspacerConstants.EXTRA_AUTHORITY,
                            Constants.authorityForMode(mode)
                        )
                    }
                )
            ).create()
        )
    }

    // The tap action is chosen per instance in the settings screen: open the
    // settings, or refresh the ETA right from the Smartspace.
    private fun tapTarget(tapAction: TapActionMode): Class<*> = when (tapAction) {
        TapActionMode.SETTINGS -> SettingsActivity::class.java
        TapActionMode.UPDATE_ETA -> UpdateEtaActivity::class.java
    }

    override fun getConfig(smartspacerId: String?): Config {
        val context = provideContext()
        val modeLabel = context.getString(
            when (mode) {
                TravelMode.DRIVING -> com.rubcut.gis2smartspacer.R.string.mode_driving
                TravelMode.WALKING -> com.rubcut.gis2smartspacer.R.string.mode_walking
                TravelMode.TRANSIT -> com.rubcut.gis2smartspacer.R.string.mode_transit
            }
        )
        // Each instance sets its own refresh interval (5–480 minutes); the
        // generic config (unknown id) falls back to the default period.
        val refreshPeriodMinutes = smartspacerId
            ?.let { SettingsRepository(context).forComplication(it).updateIntervalMinutes }
            ?: Constants.DEFAULT_REFRESH_PERIOD_MINUTES
        return Config(
            label = context.getString(com.rubcut.gis2smartspacer.R.string.complication_label, modeLabel),
            description = context.getString(
                com.rubcut.gis2smartspacer.R.string.complication_description,
                modeLabel.lowercase()
            ),
            icon = AndroidIcon.createWithResource(context, iconRes),
            refreshPeriodMinutes = refreshPeriodMinutes,
            refreshIfNotVisible = true,
            configActivity = Intent(context, SettingsActivity::class.java)
        )
    }

    override fun onProviderRemoved(smartspacerId: String) {
        // The removed instance's settings are useless now — drop them.
        SettingsRepository(provideContext()).clearComplication(smartspacerId)
    }
}
