package com.example.kuluckakontrolu

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kuluckakontrolu.databinding.ActivityConnectionSetupBinding
import com.example.kuluckakontrolu.repository.ESP32IncubatorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import kotlinx.coroutines.withTimeoutOrNull

class ConnectionSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectionSetupBinding
    private lateinit var repository: ESP32IncubatorRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectionSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = (application as IncubatorApplication).repository as ESP32IncubatorRepository

        setupListeners()
    }

    private fun setupListeners() {
        // Test Connection button
        binding.buttonConnect.setOnClickListener {
            val ipAddress = binding.editTextIpAddress.text.toString().trim()
            val portStr = binding.editTextPort.text.toString().trim()

            if (ipAddress.isEmpty()) {
                binding.editTextIpAddress.error = getString(R.string.required_field)
                return@setOnClickListener
            }

            if (portStr.isEmpty()) {
                binding.editTextPort.error = getString(R.string.required_field)
                return@setOnClickListener
            }

            val port = portStr.toIntOrNull() ?: 80

            testConnection(ipAddress, port)
        }

        // Demo mode button
        binding.buttonStartDemo.setOnClickListener {
            enableDemoMode()
        }

        // Save and continue button
        binding.buttonSave.setOnClickListener {
            val ipAddress = binding.editTextIpAddress.text.toString().trim()
            val portStr = binding.editTextPort.text.toString().trim()
            val port = portStr.toIntOrNull() ?: 80

            // Save connection settings
            repository.saveConnectionSettings(ipAddress, port)

            // Save first run status
            getSharedPreferences("KuluckaSettings", MODE_PRIVATE).edit()
                .putBoolean("FirstRun", false)
                .apply()

            // Navigate to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Demo mode kartı için tıklama dinleyicisi
        binding.cardViewDemo.setOnClickListener {
            Log.d("ConnectionSetupActivity", "Demo mode card clicked")
            enableDemoMode()
        }
    }

    private fun testConnection(ipAddress: String, port: Int) {
        binding.buttonConnect.isEnabled = false
        binding.progressBarConnection.visibility = View.VISIBLE
        binding.textViewStatus.visibility = View.VISIBLE
        binding.textViewStatus.text = getString(R.string.testing_connection)
        binding.textViewStatus.setTextColor(getColor(R.color.text_secondary))

        lifecycleScope.launch {
            try {
                // Bağlantı ayarlarını geçici olarak kaydet
                repository.saveConnectionSettings(ipAddress, port)

                // Bağlantıyı test et
                val isConnected = withContext(Dispatchers.IO) {
                    repository.isDeviceConnected()
                }

                if (isConnected) {
                    binding.textViewStatus.text = getString(R.string.connection_successful)
                    binding.textViewStatus.setTextColor(getColor(R.color.green))
                    binding.buttonSave.isEnabled = true
                } else {
                    binding.textViewStatus.text = getString(R.string.connection_failed)
                    binding.textViewStatus.setTextColor(getColor(R.color.red))
                    binding.buttonSave.isEnabled = false
                }
            } catch (e: Exception) {
                binding.textViewStatus.text = getString(R.string.connection_error, e.message)
                binding.textViewStatus.setTextColor(getColor(R.color.red))
                binding.buttonSave.isEnabled = false
            } finally {
                binding.buttonConnect.isEnabled = true
                binding.progressBarConnection.visibility = View.GONE
            }
        }
    }

    private fun enableDemoMode() {
        Log.d("ConnectionSetupActivity", "enableDemoMode called")

        // Demo mod bayrağını ayarla
        getSharedPreferences("KuluckaSettings", MODE_PRIVATE).edit()
            .putBoolean("DemoMode", true)
            .putBoolean("FirstRun", false)
            .apply()

        binding.textViewStatus.visibility = View.VISIBLE
        binding.textViewStatus.text = getString(R.string.demo_mode_enabled)
        binding.textViewStatus.setTextColor(getColor(R.color.amber))

        Toast.makeText(this, R.string.demo_mode_enabled, Toast.LENGTH_SHORT).show()

        // Ana ekrana geçiş - veri akışı dahil olarak
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("DEMO_MODE_ACTIVATED", true)
            startActivity(intent)
            finish()
        }, 1500)
    }
}