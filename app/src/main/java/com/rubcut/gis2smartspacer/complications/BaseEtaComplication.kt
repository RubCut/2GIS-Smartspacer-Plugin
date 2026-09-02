package com.rubcut.gis2smartspacer.complications

import android.content.Intent
import android.graphics.drawable.Icon as AndroidIcon
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider.Config
import com.rubcut.gis2smartspacer.SettingsActivity
import com.rubcut.gis2smartspacer.SettingsRepository
import com.rubcut.gis2smartspacer.TravelMode

/**
 * Общая логика для трёх Complication-провайдеров (авто/пешком/транспорт).
 * getSmartspaceActions() тут ничего не качает из сети — только читает
 * последнее закэшированное значение, обновляемое в EtaComplicationUpdateReceiver.
 */
abstract class BaseEtaComplication(
    private val mode: TravelMode,
    private val iconRes: Int
) : SmartspacerComplicationProvider() {

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val context = provideContext()
        val settings = SettingsRepository(context)
        val minutes = settings.getEtaMinutes(mode)
        val content = when {
            !settings.isConfigured -> context.getString(
                com.rubcut.gis2smartspacer.R.string.complication_needs_setup
            )
            minutes == null -> context.getString(
                com.rubcut.gis2smartspacer.R.string.complication_no_eta
            )
            else -> formatMinutes(minutes)
        }

        // Complication никогда не исчезает: до настройки она сама сообщает,
        // что нужно открыть More settings, а при временной ошибке показывает No ETA.
        return listOf(
            ComplicationTemplate.Basic(
                id = "eta_${mode.name.lowercase()}_$smartspacerId",
                icon = Icon(AndroidIcon.createWithResource(context, iconRes), shouldTint = true),
                content = Text(content),
                onClick = TapAction(
                    intent = Intent(context, SettingsActivity::class.java)
                )
            ).create()
        )
    }

    // Basic-шаблон ограничивает текст 12 символами, поэтому держим формат коротким
    private fun formatMinutes(minutes: Int): String {
        val context = provideContext()
        if (minutes < 60) {
            return context.getString(com.rubcut.gis2smartspacer.R.string.eta_minutes, minutes)
        }
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (remainingMinutes == 0) {
            context.getString(com.rubcut.gis2smartspacer.R.string.eta_hours, hours)
        } else {
            context.getString(
                com.rubcut.gis2smartspacer.R.string.eta_hours_minutes,
                hours,
                remainingMinutes
            )
        }
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
        return Config(
            label = context.getString(com.rubcut.gis2smartspacer.R.string.complication_label, modeLabel),
            description = context.getString(
                com.rubcut.gis2smartspacer.R.string.complication_description,
                modeLabel.lowercase()
            ),
            icon = AndroidIcon.createWithResource(context, iconRes),
            refreshPeriodMinutes = com.rubcut.gis2smartspacer.Constants.REFRESH_PERIOD_MINUTES,
            // До первого успешного запроса список пуст — обновления всё равно должны приходить.
            refreshIfNotVisible = true,
            // Настройки доступны только через More settings, без показа при добавлении.
            configActivity = Intent(context, SettingsActivity::class.java)
        )
    }

    override fun onProviderRemoved(smartspacerId: String) {
        // Настройки общие для всех трёх Complication, поэтому здесь ничего не чистим
    }
}
