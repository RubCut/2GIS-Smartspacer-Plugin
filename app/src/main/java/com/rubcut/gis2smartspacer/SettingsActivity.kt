package com.rubcut.gis2smartspacer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import kotlinx.coroutines.launch
import com.rubcut.gis2smartspacer.complications.CarEtaComplication
import com.rubcut.gis2smartspacer.complications.TransitEtaComplication
import com.rubcut.gis2smartspacer.complications.WalkEtaComplication

class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: SettingsRepository

    private lateinit var apiKeyField: EditText
    private lateinit var addressField: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = SettingsRepository(this)

        apiKeyField = findViewById(R.id.apiKeyField)
        addressField = findViewById(R.id.addressField)
        statusText = findViewById(R.id.statusText)

        apiKeyField.setText(settings.apiKey)
        addressField.setText(settings.destAddress)
        updateStatus()

        findViewById<Button>(R.id.requestLocationButton).setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION
            )
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveAndGeocode()
        }
    }

    private fun saveAndGeocode() {
        val apiKey = apiKeyField.text.toString().trim()
        val address = addressField.text.toString().trim()

        if (apiKey.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Заполни ключ API и адрес", Toast.LENGTH_SHORT).show()
            return
        }

        settings.apiKey = apiKey
        settings.destAddress = address
        statusText.text = "Определяю координаты адреса…"

        lifecycleScope.launch {
            val point = TwoGisClient(apiKey).geocode(address)
            if (point == null) {
                statusText.text = "Не удалось найти адрес. Проверь ключ и формулировку адреса."
                return@launch
            }
            settings.destLat = point.lat
            settings.destLon = point.lon
            updateStatus()
            notifyAllComplications()
            Toast.makeText(this@SettingsActivity, "Сохранено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun notifyAllComplications() {
        SmartspacerComplicationProvider.notifyChange(this, CarEtaComplication::class.java)
        SmartspacerComplicationProvider.notifyChange(this, WalkEtaComplication::class.java)
        SmartspacerComplicationProvider.notifyChange(this, TransitEtaComplication::class.java)
    }

    private fun updateStatus() {
        statusText.text = if (settings.hasDestination) {
            "Координаты назначения: ${settings.destLat}, ${settings.destLon}\n" +
                "Обновление раз в ${Constants.REFRESH_PERIOD_MINUTES} мин."
        } else {
            "Адрес ещё не определён — нажми «Сохранить»"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            Toast.makeText(
                this,
                if (granted) "Доступ к геолокации разрешён" else "Без геолокации плагин не сможет определить твоё текущее положение",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private const val REQUEST_LOCATION = 1001
    }
}
