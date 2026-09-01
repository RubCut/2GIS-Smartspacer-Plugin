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
        val settings = SettingsRepository(provideContext())
        if (!settings.isConfigured) return emptyList()

        val minutes = settings.getEtaMinutes(mode) ?: return emptyList()
        val content = formatMinutes(minutes)

        return listOf(
            ComplicationTemplate.Basic(
                id = "eta_${mode.name.lowercase()}_$smartspacerId",
                icon = Icon(AndroidIcon.createWithResource(provideContext(), iconRes), shouldTint = false),
                content = Text(content),
                onClick = TapAction(
                    intent = Intent(provideContext(), SettingsActivity::class.java)
                )
            ).create()
        )
    }

    // Basic-шаблон ограничивает текст 12 символами, поэтому держим формат коротким
    private fun formatMinutes(minutes: Int): String = when {
        minutes < 60 -> "$minutes мин"
        else -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0) "$h ч" else "$h ч $m м"
        }
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = "ETA: ${mode.label}",
            description = "Время в пути (${mode.label.lowercase()}) до заданного адреса, через 2ГИС",
            icon = AndroidIcon.createWithResource(provideContext(), iconRes),
            refreshPeriodMinutes = com.rubcut.gis2smartspacer.Constants.REFRESH_PERIOD_MINUTES,
            refreshIfNotVisible = false,
            configActivity = Intent(provideContext(), SettingsActivity::class.java)
        )
    }

    override fun onProviderRemoved(smartspacerId: String) {
        // Настройки общие для всех трёх Complication, поэтому здесь ничего не чистим
    }
}
