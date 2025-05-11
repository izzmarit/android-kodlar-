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
import java.net.Socket
import java.net.InetSocketAddress
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.InputStreamReader
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

        // Detaylı log bilgisi
        Log.d("ConnectionSetup", "Test bağlantısı başlatılıyor: IP=$ipAddress, Port=$port")

        lifecycleScope.launch {
            try {
                // Bağlantı ayarlarını geçici olarak kaydet
                repository.saveConnectionSettings(ipAddress, port)

                // Bağlantıyı temel soket ile test et
                var isConnected = false
                withContext(Dispatchers.IO) {
                    try {
                        val socket = Socket()
                        socket.soTimeout = 5000
                        socket.connect(InetSocketAddress(ipAddress, port), 5000)
                        isConnected = socket.isConnected

                        Log.d("ConnectionSetup", "Temel soket bağlantısı: $isConnected")

                        if (isConnected) {
                            try {
                                // PrintWriter kullanmak yerine doğrudan OutputStream kullan
                                val outputStream = socket.getOutputStream()

                                // JSON komutu ve satır sonu karakteri ekle (KRITIK NOKTA)
                                val pingCommand = "{\"cmd\":\"ping\"}\n"
                                outputStream.write(pingCommand.toByteArray())
                                outputStream.flush()

                                Log.d("ConnectionSetup", "Komut gönderildi: $pingCommand")

                                // Yanıt bekle - daha uzun süre bekle
                                val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                                var line: String? = null
                                val startTime = System.currentTimeMillis()

                                // Aktif olarak yanıt bekle
                                while (System.currentTimeMillis() - startTime < 7000) { // 7 saniye bekle
                                    if (input.ready()) {
                                        line = input.readLine()
                                        if (line != null && line.isNotEmpty()) {
                                            Log.d("ConnectionSetup", "Sunucu yanıtı: $line")
                                            break
                                        }
                                    }
                                    // Thread.sleep kullan
                                    Thread.sleep(100)
                                }

                                if (line == null) {
                                    Log.d("ConnectionSetup", "Sunucu yanıt vermedi veya okunamadı")
                                }
                            } catch (e: Exception) {
                                Log.e("ConnectionSetup", "Veri gönderme/alma hatası: ${e.message}")
                                e.printStackTrace()
                            }
                        }

                        socket.close()
                    } catch (e: Exception) {
                        Log.e("ConnectionSetup", "Soket bağlantı hatası: ${e.message}")
                        e.printStackTrace()
                        isConnected = false
                    }
                }

                // Repository üzerinden tam bağlantıyı test et
                if (isConnected) {
                    val repoConnected = repository.isDeviceConnected()
                    Log.d("ConnectionSetup", "Repository bağlantısı: $repoConnected")
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
                Log.e("ConnectionSetup", "Bağlantı testi hatası: ${e.message}", e)
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