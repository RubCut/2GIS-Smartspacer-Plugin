package com.rubcut.gis2smartspacer

import android.content.Context

/**
 * Short human-readable ETA and interval formatting, shared by the Complication
 * providers, the settings screen and the on-tap updater.
 */
object EtaFormatter {

    /** `23 мин` / `1 ч` / `1 ч 5 м` — kept under the 12-char Complication limit. */
    fun formatMinutes(context: Context, minutes: Int): String = when {
        minutes < 60 -> context.getString(R.string.eta_minutes, minutes)
        minutes % 60 == 0 -> context.getString(R.string.eta_hours, minutes / 60)
        else -> context.getString(R.string.eta_hours_minutes, minutes / 60, minutes % 60)
    }

    /** `15 мин`, `2 ч`, `2 ч 30 мин` — for the settings screen, no length limit. */
    fun formatInterval(context: Context, minutes: Int): String = when {
        minutes < 60 -> context.getString(R.string.eta_minutes, minutes)
        minutes % 60 == 0 -> context.getString(R.string.eta_hours_full, minutes / 60)
        else -> context.getString(
            R.string.eta_hours_minutes_full,
            minutes / 60,
            minutes % 60
        )
    }
}
