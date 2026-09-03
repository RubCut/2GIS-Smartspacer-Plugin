package com.rubcut.gis2smartspacer

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import kotlinx.coroutines.launch

/**
 * Invisible trampoline activity behind the "Update ETA" tap action: it refreshes
 * the ETA of the tapped Complication instance, toasts the result and finishes.
 * It never shows any UI, so the user stays in the Smartspace.
 */
class UpdateEtaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val smartspacerId = intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)
            ?: Constants.FALLBACK_COMPLICATION_ID
        val authority = intent.getStringExtra(SmartspacerConstants.EXTRA_AUTHORITY)
        // The authority tells which provider was tapped; without it (a stale
        // tap action from an older plugin version) refresh all three modes.
        val modes = Constants.modeForAuthority(authority)?.let { setOf(it) }
            ?: TravelMode.entries.toSet()

        // The activity stays translucent-and-empty while the coroutine runs;
        // lifecycleScope is cancelled only in onDestroy, i.e. at finish() below.
        lifecycleScope.launch {
            val settings = SettingsRepository(this@UpdateEtaActivity)
                .forComplication(smartspacerId)
            if (!settings.isConfigured) {
                toast(R.string.toast_update_needs_setup)
                finish()
                return@launch
            }

            val origin = LocationHelper.getCurrentLocation(this@UpdateEtaActivity)
                ?: LocationHelper.getLastKnownLocation(this@UpdateEtaActivity)
            if (origin == null) {
                toast(R.string.toast_no_location)
                finish()
                return@launch
            }

            Toast.makeText(this@UpdateEtaActivity, R.string.toast_updating, Toast.LENGTH_SHORT)
                .show()
            val updated = EtaUpdater.refreshFrom(origin, settings, modes)
            ComplicationNotifier.notifyChange(this@UpdateEtaActivity, authority, smartspacerId)

            val minutes = modes.firstNotNullOfOrNull { settings.getEtaMinutes(it) }
            if (updated && minutes != null) {
                val eta = EtaFormatter.formatMinutes(this@UpdateEtaActivity, minutes)
                toast(getString(R.string.toast_updated, eta))
            } else {
                toast(R.string.toast_update_failed)
            }
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun toast(resId: Int) = toast(getString(resId))

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}
