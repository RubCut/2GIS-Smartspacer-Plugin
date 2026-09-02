package com.rubcut.gis2smartspacer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.rubcut.gis2smartspacer.complications.CarEtaComplication
import com.rubcut.gis2smartspacer.complications.TransitEtaComplication
import com.rubcut.gis2smartspacer.complications.WalkEtaComplication
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var complicationSettings: ComplicationSettings
    private lateinit var smartspacerId: String
    private var complicationAuthority: String? = null
    private lateinit var apiKeyField: TextInputEditText
    private lateinit var addressField: TextInputEditText
    private lateinit var apiKeyLayout: TextInputLayout
    private lateinit var addressLayout: TextInputLayout
    private lateinit var latitudeLayout: TextInputLayout
    private lateinit var longitudeLayout: TextInputLayout
    private lateinit var latitudeField: TextInputEditText
    private lateinit var longitudeField: TextInputEditText
    private lateinit var coordinatesContainer: LinearLayout
    private lateinit var destinationModeGroup: MaterialButtonToggleGroup
    private lateinit var statusCard: MaterialCardView
    private lateinit var statusTitle: TextView
    private lateinit var statusText: TextView
    private lateinit var statusIcon: ImageView
    private lateinit var progress: ProgressBar
    private lateinit var saveButton: MaterialButton
    private lateinit var locationButton: MaterialButton

    private val foregroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        Toast.makeText(
            this,
            if (granted) R.string.location_granted else R.string.location_denied,
            Toast.LENGTH_LONG
        ).show()
        updateLocationButton()
    }

    private val backgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { updateLocationButton() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        setContentView(R.layout.activity_main)

        settingsRepository = SettingsRepository(this)
        // Smartspacer passes the instance being configured; fall back to a
        // shared pseudo-instance when opened without the extras.
        smartspacerId = intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)
            ?: Constants.FALLBACK_COMPLICATION_ID
        complicationAuthority = intent.getStringExtra(SmartspacerConstants.EXTRA_AUTHORITY)
        // Settings belong to this exact Complication instance.
        complicationSettings = settingsRepository.forComplication(smartspacerId)
        if (complicationSettings.isConfigured) setResult(Activity.RESULT_OK)
        bindViews()
        applySystemBarInsets()
        // A Complication without its own key starts from the synced default.
        apiKeyField.setText(complicationSettings.apiKey.ifBlank { settingsRepository.defaultApiKey })
        addressField.setText(complicationSettings.destAddress)
        if (complicationSettings.hasDestination) {
            latitudeField.setText(complicationSettings.destLat.toString())
            longitudeField.setText(complicationSettings.destLon.toString())
        }
        val initialMode = if (complicationSettings.usesManualCoordinates) {
            R.id.coordinatesModeButton
        } else {
            R.id.addressModeButton
        }
        destinationModeGroup.check(initialMode)
        showDestinationMode(initialMode)
        showStoredStatus()
        updateLocationButton()

        destinationModeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) showDestinationMode(checkedId)
        }
        findViewById<MaterialButton>(R.id.getKeyButton).setOnClickListener {
            openUrl("https://platform.2gis.ru/")
        }
        findViewById<MaterialButton>(R.id.apiDocsButton).setOnClickListener {
            openUrl("https://docs.2gis.com/api/navigation/routing/overview")
        }
        findViewById<MaterialButton>(R.id.open2GisButton).setOnClickListener {
            openUrl("https://2gis.ru/")
        }
        saveButton.setOnClickListener { saveAndGeocode() }
        locationButton.setOnClickListener { requestRequiredLocationPermission() }
        addressField.setOnEditorActionListener { _, _, _ ->
            saveAndGeocode()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (::locationButton.isInitialized) updateLocationButton()
    }

    private fun bindViews() {
        apiKeyField = findViewById(R.id.apiKeyField)
        addressField = findViewById(R.id.addressField)
        apiKeyLayout = findViewById(R.id.apiKeyLayout)
        addressLayout = findViewById(R.id.addressLayout)
        latitudeLayout = findViewById(R.id.latitudeLayout)
        longitudeLayout = findViewById(R.id.longitudeLayout)
        latitudeField = findViewById(R.id.latitudeField)
        longitudeField = findViewById(R.id.longitudeField)
        coordinatesContainer = findViewById(R.id.coordinatesContainer)
        destinationModeGroup = findViewById(R.id.destinationModeGroup)
        statusCard = findViewById(R.id.statusCard)
        statusTitle = findViewById(R.id.statusTitle)
        statusText = findViewById(R.id.statusText)
        statusIcon = findViewById(R.id.statusIcon)
        progress = findViewById(R.id.progressIndicator)
        saveButton = findViewById(R.id.saveButton)
        locationButton = findViewById(R.id.requestLocationButton)
    }

    private fun saveAndGeocode() {
        val apiKey = apiKeyField.text?.toString()?.trim().orEmpty()
        val address = addressField.text?.toString()?.trim().orEmpty()
        val manual = destinationModeGroup.checkedButtonId == R.id.coordinatesModeButton
        apiKeyLayout.error = getString(R.string.validation_required).takeIf { apiKey.isEmpty() }
        addressLayout.error = getString(R.string.validation_required).takeIf { !manual && address.isEmpty() }

        val latitude = latitudeField.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
        val longitude = longitudeField.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
        latitudeLayout.error = getString(R.string.invalid_coordinate)
            .takeIf { manual && (latitude == null || latitude !in -90.0..90.0) }
        longitudeLayout.error = getString(R.string.invalid_coordinate)
            .takeIf { manual && (longitude == null || longitude !in -180.0..180.0) }
        if (apiKey.isEmpty() || (!manual && address.isEmpty()) ||
            (manual && (latitude == null || latitude !in -90.0..90.0 ||
                longitude == null || longitude !in -180.0..180.0))
        ) return

        val previousApiKey = complicationSettings.apiKey
        setBusy(true)
        showStatus(R.string.status_geocoding_title, R.string.status_geocoding, loading = true)
        lifecycleScope.launch {
            val point = if (manual) {
                GeoPoint(latitude!!, longitude!!)
            } else {
                TwoGisClient(apiKey).geocode(address)
            }
            if (point == null) {
                setBusy(false)
                showStatus(
                    R.string.status_error_title,
                    R.string.status_geocode_error,
                    isError = true
                )
                return@launch
            }

            val destinationName = if (manual) {
                getString(R.string.manual_destination, point.lat, point.lon)
            } else {
                address
            }
            complicationSettings.saveDestination(
                apiKey,
                destinationName,
                point,
                manualCoordinates = manual
            )
            if (apiKey != previousApiKey) {
                // A new key entered in this Complication becomes the synced
                // default that newly added Complications are pre-filled with.
                settingsRepository.defaultApiKey = apiKey
            }
            setResult(Activity.RESULT_OK)
            notifyComplication()

            if (!LocationHelper.hasForegroundPermission(this@SettingsActivity)) {
                setBusy(false)
                showStatus(
                    R.string.status_location_needed_title,
                    R.string.status_location_needed
                )
                return@launch
            }

            showStatus(R.string.status_refreshing_title, R.string.status_refreshing, loading = true)
            val origin = LocationHelper.getCurrentLocation(this@SettingsActivity)
            val updated = origin != null && EtaUpdater.refreshFrom(origin, complicationSettings)
            setBusy(false)
            if (origin != null && !updated) {
                showStatus(
                    R.string.status_route_error_title,
                    R.string.status_route_error,
                    isError = true
                )
            } else {
                showStoredStatus(updated)
            }
            notifyComplication()
            Toast.makeText(this@SettingsActivity, R.string.saved_message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showStoredStatus(justUpdated: Boolean = false) {
        if (!complicationSettings.hasDestination) {
            showStatus(R.string.status_not_configured_title, R.string.status_not_configured)
            return
        }
        val address = if (complicationSettings.usesManualCoordinates) {
            getString(
                R.string.manual_destination,
                complicationSettings.destLat,
                complicationSettings.destLon
            )
        } else {
            complicationSettings.destAddress
        }
        val lastUpdate = complicationSettings.lastUpdateTimestamp
        if (lastUpdate > 0L || justUpdated) {
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(lastUpdate))
            showStatus(
                getString(R.string.status_ready_title),
                getString(
                    R.string.status_ready,
                    "$address · $time",
                    Constants.REFRESH_PERIOD_MINUTES
                )
            )
        } else {
            showStatus(
                getString(R.string.status_ready_title),
                getString(R.string.status_ready_no_eta, address)
            )
        }
    }

    private fun showStatus(titleRes: Int, bodyRes: Int, loading: Boolean = false, isError: Boolean = false) =
        showStatus(getString(titleRes), getString(bodyRes), loading, isError)

    private fun showStatus(
        title: String,
        body: String,
        loading: Boolean = false,
        isError: Boolean = false
    ) {
        statusTitle.text = title
        statusText.text = body
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        statusIcon.visibility = if (loading) View.GONE else View.VISIBLE
        val backgroundAttr = if (isError) {
            com.google.android.material.R.attr.colorErrorContainer
        } else {
            com.google.android.material.R.attr.colorSecondaryContainer
        }
        val foregroundAttr = if (isError) {
            com.google.android.material.R.attr.colorOnErrorContainer
        } else {
            com.google.android.material.R.attr.colorOnSecondaryContainer
        }
        val background = MaterialColors.getColor(statusCard, backgroundAttr)
        val foreground = MaterialColors.getColor(statusCard, foregroundAttr)
        statusCard.setCardBackgroundColor(background)
        statusTitle.setTextColor(foreground)
        statusText.setTextColor(foreground)
        statusIcon.setColorFilter(foreground)
        progress.indeterminateTintList = android.content.res.ColorStateList.valueOf(foreground)
    }

    private fun setBusy(busy: Boolean) {
        saveButton.isEnabled = !busy
        apiKeyField.isEnabled = !busy
        addressField.isEnabled = !busy
        latitudeField.isEnabled = !busy
        longitudeField.isEnabled = !busy
        destinationModeGroup.isEnabled = !busy
        saveButton.setText(if (busy) R.string.saving_button else R.string.save_button)
    }

    private fun showDestinationMode(checkedId: Int) {
        val manual = checkedId == R.id.coordinatesModeButton
        addressLayout.visibility = if (manual) View.GONE else View.VISIBLE
        coordinatesContainer.visibility = if (manual) View.VISIBLE else View.GONE
        addressLayout.error = null
        latitudeLayout.error = null
        longitudeLayout.error = null
    }

    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.rootScroll)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun requestRequiredLocationPermission() {
        if (!LocationHelper.hasForegroundPermission(this)) {
            foregroundPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !LocationHelper.hasBackgroundPermission(this)
        ) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                Toast.makeText(this, R.string.background_location_hint, Toast.LENGTH_LONG).show()
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    )
                )
            }
        }
    }

    private fun updateLocationButton() {
        val hasForeground = LocationHelper.hasForegroundPermission(this)
        val hasBackground = LocationHelper.hasBackgroundPermission(this)
        locationButton.setText(
            when {
                !hasForeground -> R.string.location_button_grant
                !hasBackground -> R.string.location_button_background
                else -> R.string.location_button_ready
            }
        )
        locationButton.isEnabled = !hasForeground || !hasBackground
    }

    private fun notifyComplication() {
        // Refresh only the instance that was edited; the authority extra tells
        // which provider owns it.
        when (complicationAuthority) {
            Constants.AUTHORITY_CAR -> SmartspacerComplicationProvider.notifyChange(
                this, CarEtaComplication::class.java, smartspacerId
            )
            Constants.AUTHORITY_WALK -> SmartspacerComplicationProvider.notifyChange(
                this, WalkEtaComplication::class.java, smartspacerId
            )
            Constants.AUTHORITY_TRANSIT -> SmartspacerComplicationProvider.notifyChange(
                this, TransitEtaComplication::class.java, smartspacerId
            )
            else -> {
                SmartspacerComplicationProvider.notifyChange(
                    this, CarEtaComplication::class.java, smartspacerId
                )
                SmartspacerComplicationProvider.notifyChange(
                    this, WalkEtaComplication::class.java, smartspacerId
                )
                SmartspacerComplicationProvider.notifyChange(
                    this, TransitEtaComplication::class.java, smartspacerId
                )
            }
        }
    }
}
