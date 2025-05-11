package com.example.kuluckakontrolu

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.kuluckakontrolu.databinding.ActivitySettingsBinding
import com.example.kuluckakontrolu.model.IncubatorSettings
import com.example.kuluckakontrolu.model.LoadingState
import com.example.kuluckakontrolu.utils.SecurityUtils
import com.example.kuluckakontrolu.viewmodel.IncubatorViewModel
import com.example.kuluckakontrolu.viewmodel.IncubatorViewModelFactory
import android.util.Log
import android.widget.Switch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: IncubatorViewModel

    private var targetTemp = 37.5
    private var targetHumidity = 60
    private var motorInterval = 120
    private var animalType = "TAVUK"
    private var currentDay = 0
    private var totalDays = 21

    // PID değişkenleri
    private var pidKp = 10.0
    private var pidKi = 0.1
    private var pidKd = 50.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Demo modu ayarları
        val sharedPreferences = getSharedPreferences("KuluckaSettings", MODE_PRIVATE)
        val switchDemoMode = findViewById<Switch>(R.id.switchDemoMode)
        switchDemoMode.isChecked = sharedPreferences.getBoolean("DemoMode", false)

        // Demo modu durumunu kaydet
        switchDemoMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("DemoMode", isChecked).apply()
            Toast.makeText(this, "Değişikliklerin etkili olması için uygulamayı yeniden başlatın", Toast.LENGTH_LONG).show()
        }

        // Geri butonu ekle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ViewModel oluşturma
        val repository = (application as IncubatorApplication).repository
        val viewModelFactory = IncubatorViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[IncubatorViewModel::class.java]

        // Hayvan türü spinner'ını ayarla
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.animal_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAnimalType.adapter = adapter

        // Mevcut ayarları yükle
        viewModel.dataState.observe(this) { state ->
            if (state is LoadingState.Success) {
                loadSettings(state.data.settings)
            }
        }

        // Giriş butonu
        binding.buttonLogin.setOnClickListener {
            if (SecurityUtils.validatePassword(this, binding.editTextPassword.text.toString())) {
                // Şifre doğru, ayarlar menüsünü göster
                binding.layoutPasswordEntry.visibility = View.GONE
                binding.settingsScrollView.visibility = View.VISIBLE
            } else {
                // Şifre yanlış
                Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show()
            }
        }

        setupControlButtons()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupControlButtons() {
        with(binding) {
            // Sıcaklık ayarı butonları
            buttonTempMinus.setOnClickListener {
                if (targetTemp > 30.0) {
                    targetTemp -= 0.1
                    textViewTempSetting.text = String.format("%.1f", targetTemp)
                }
            }

            buttonTempPlus.setOnClickListener {
                if (targetTemp < 40.0) {
                    targetTemp += 0.1
                    textViewTempSetting.text = String.format("%.1f", targetTemp)
                }
            }

            // Nem ayarı butonları
            buttonHumidityMinus.setOnClickListener {
                if (targetHumidity > 40) {
                    targetHumidity -= 1
                    textViewHumiditySetting.text = targetHumidity.toString()
                }
            }

            buttonHumidityPlus.setOnClickListener {
                if (targetHumidity < 80) {
                    targetHumidity += 1
                    textViewHumiditySetting.text = targetHumidity.toString()
                }
            }

            // Motor ayarı butonları
            buttonMotorMinus.setOnClickListener {
                if (motorInterval > 30) {
                    motorInterval -= 15
                    textViewMotorSetting.text = motorInterval.toString()
                }
            }

            buttonMotorPlus.setOnClickListener {
                if (motorInterval < 240) {
                    motorInterval += 15
                    textViewMotorSetting.text = motorInterval.toString()
                }
            }

            // Gün ayarı butonları
            buttonDaysMinus.setOnClickListener {
                if (currentDay > 0) {
                    currentDay -= 1
                    textViewDaysSetting.text = currentDay.toString()
                }
            }

            buttonDaysPlus.setOnClickListener {
                if (currentDay < totalDays) {
                    currentDay += 1
                    textViewDaysSetting.text = currentDay.toString()
                }
            }

            // PID-Kp ayarı butonları
            buttonKpMinus?.setOnClickListener {
                if (pidKp > 1.0) {
                    pidKp -= 0.5
                    textViewKp.text = String.format("%.1f", pidKp)
                }
            }

            buttonKpPlus?.setOnClickListener {
                if (pidKp < 20.0) {
                    pidKp += 0.5
                    textViewKp.text = String.format("%.1f", pidKp)
                }
            }

            // PID-Ki ayarı butonları
            buttonKiMinus?.setOnClickListener {
                if (pidKi > 0.01) {
                    pidKi = (pidKi - 0.01).coerceAtLeast(0.01)
                    textViewKi.text = String.format("%.2f", pidKi)
                }
            }

            buttonKiPlus?.setOnClickListener {
                if (pidKi < 1.0) {
                    pidKi += 0.01
                    textViewKi.text = String.format("%.2f", pidKi)
                }
            }

            // PID-Kd ayarı butonları
            buttonKdMinus?.setOnClickListener {
                if (pidKd > 1.0) {
                    pidKd -= 1.0
                    textViewKd.text = String.format("%.1f", pidKd)
                }
            }

            buttonKdPlus?.setOnClickListener {
                if (pidKd < 100.0) {
                    pidKd += 1.0
                    textViewKd.text = String.format("%.1f", pidKd)
                }
            }

            // Hayvan türü değiştiğinde
            spinnerAnimalType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    animalType = parent.getItemAtPosition(position).toString()
                    totalDays = when (animalType) {
                        "TAVUK" -> 21
                        "ÖRDEK" -> 28
                        "KAZ" -> 30
                        "HİNDİ" -> 28
                        "SÜLÜN" -> 25
                        "GÜVERCİN" -> 18
                        "KEKLİK" -> 24
                        else -> 17 // Bıldırcın
                    }

                    // Gün maksimum değerini kontrol et
                    if (currentDay > totalDays) {
                        currentDay = totalDays
                        textViewDaysSetting.text = currentDay.toString()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Hiçbir şey
                }
            }

            // Kaydet ve gönder butonu
            buttonSaveSettings.setOnClickListener {
                try {
                    // Virgül yerine nokta kullanarak Double'a çevir
                    val tempText = textViewTempSetting.text.toString().replace(',', '.')
                    val validatedTemp = tempText.toDouble()

                    val validatedHumidity = textViewHumiditySetting.text.toString().toInt()
                    val validatedMotor = textViewMotorSetting.text.toString().toInt()
                    val validatedDay = textViewDaysSetting.text.toString().toInt()

                    if (isValidSettings(validatedTemp, validatedHumidity, validatedMotor, validatedDay)) {
                        saveSettings()
                        Toast.makeText(this@SettingsActivity, R.string.settings_saved, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@SettingsActivity, R.string.invalid_settings, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this@SettingsActivity, R.string.invalid_number_format, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isValidSettings(temp: Double, humidity: Int, motor: Int, day: Int): Boolean {
        return temp in 30.0..40.0 && humidity in 40..80 && motor in 30..240 && day in 0..totalDays
    }

    private fun loadSettings(settings: IncubatorSettings) {
        targetTemp = settings.targetTemp
        targetHumidity = settings.targetHumidity
        motorInterval = settings.motorInterval
        animalType = settings.animalType
        currentDay = settings.currentDay

        // PID parametrelerini yükle
        pidKp = settings.pidKp
        pidKi = settings.pidKi
        pidKd = settings.pidKd

        with(binding) {
            // UI'yi güncelle
            textViewTempSetting.text = String.format("%.1f", targetTemp)
            textViewHumiditySetting.text = targetHumidity.toString()
            textViewMotorSetting.text = motorInterval.toString()
            textViewDaysSetting.text = currentDay.toString()

            // PID ayarlarını güncelle
            textViewKp?.text = String.format("%.1f", pidKp)
            textViewKi?.text = String.format("%.2f", pidKi)
            textViewKd?.text = String.format("%.1f", pidKd)

            // Spinner'ı doğru konuma ayarla
            val animalTypes = resources.getStringArray(R.array.animal_types)
            for (i in animalTypes.indices) {
                if (animalTypes[i] == animalType) {
                    spinnerAnimalType.setSelection(i)
                    break
                }
            }
        }
    }

    private fun saveSettings() {
        try {
            // Double ve Int değerleri güvenli dönüştürme
            val tempValue = runCatching {
                binding.textViewTempSetting.text.toString().replace(',', '.').toDoubleOrNull()
            }.getOrNull() ?: targetTemp

            val humidityValue = runCatching {
                binding.textViewHumiditySetting.text.toString().toIntOrNull()
            }.getOrNull() ?: targetHumidity

            val motorValue = runCatching {
                binding.textViewMotorSetting.text.toString().toIntOrNull()
            }.getOrNull() ?: motorInterval

            // PID değerlerini al
            val kpValue = runCatching {
                binding.textViewKp?.text.toString().replace(',', '.').toDoubleOrNull()
            }.getOrNull() ?: pidKp

            val kiValue = runCatching {
                binding.textViewKi?.text.toString().replace(',', '.').toDoubleOrNull()
            }.getOrNull() ?: pidKi

            val kdValue = runCatching {
                binding.textViewKd?.text.toString().replace(',', '.').toDoubleOrNull()
            }.getOrNull() ?: pidKd

            val dayValue = runCatching {
                binding.textViewDaysSetting.text.toString().toIntOrNull()
            }.getOrNull() ?: currentDay

            // Değerleri ESP32'nin GlobalDefinitions.h'deki limitlerle uyumlu şekilde sınırla
            val finalTemp = tempValue.coerceIn(30.0, 40.0)
            val finalHumidity = humidityValue.coerceIn(40, 80)
            val finalMotor = motorValue.coerceIn(30, 240)
            val finalDay = dayValue.coerceIn(0, totalDays)
            val finalKp = kpValue.coerceIn(1.0, 20.0)
            val finalKi = kiValue.coerceIn(0.01, 1.0)
            val finalKd = kdValue.coerceIn(1.0, 100.0)

            // Ayarları güncelle
            val updatedSettings = IncubatorSettings(
                targetTemp = finalTemp,
                targetHumidity = finalHumidity,
                motorInterval = finalMotor,
                animalType = animalType,
                currentDay = finalDay,
                pidKp = finalKp,
                pidKi = finalKi,
                pidKd = finalKd
            )

            viewModel.updateSettings(updatedSettings)
            Log.d("SettingsActivity", "Ayarlar güncellendi: $updatedSettings")
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Ayarlar kaydedilirken hata: ${e.message}")
            Toast.makeText(this, R.string.invalid_settings, Toast.LENGTH_SHORT).show()
        }
    }
}