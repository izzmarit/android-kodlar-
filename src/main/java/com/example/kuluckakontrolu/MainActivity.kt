package com.example.kuluckakontrolu

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.kuluckakontrolu.databinding.ActivityMainBinding
import com.example.kuluckakontrolu.dialogs.AboutDialogFragment
import com.example.kuluckakontrolu.dialogs.ReconnectionDialogFragment
import com.example.kuluckakontrolu.viewmodel.IncubatorViewModel
import com.example.kuluckakontrolu.viewmodel.IncubatorViewModelFactory
import com.example.kuluckakontrolu.viewmodel.IncubatorViewModel.ConnectionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: IncubatorViewModel
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ekranı sürekli açık tut
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // İlk çalıştırma kontrolü
        val prefs = getSharedPreferences("KuluckaSettings", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("FirstRun", true)

        if (isFirstRun) {
            // İlk çalıştırma ise bağlantı ayarları ekranına yönlendir
            val intent = Intent(this, ConnectionSetupActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewModel oluşturma
        val repository = (application as IncubatorApplication).repository
        val viewModelFactory = IncubatorViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[IncubatorViewModel::class.java]

        // Başlık ayarla - MK V5.0 versiyonunu göster
        title = getString(R.string.app_name)

        // Navigation Controller
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Bottom Navigation
        binding.bottomNavigation.setupWithNavController(navController)

        // Demo mod kontrolü - doğrudan SharedPreferences'tan oku
        val isDemoMode = prefs.getBoolean("DemoMode", false)

        // Intent'ten yeni aktivasyonu kontrol et
        val demoActivated = intent.getBooleanExtra("DEMO_MODE_ACTIVATED", false)

        if (!isDemoMode && !demoActivated) {
            binding.textDemoMode.visibility = View.GONE

            // Bağlantı durumunu göster
            binding.textConnectionStatus.text = getString(R.string.connecting)
            binding.textConnectionStatus.setTextColor(getColor(R.color.amber))
            binding.textConnectionStatus.visibility = View.VISIBLE

            // WiFi kontrolü
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo

            if (networkInfo == null || !networkInfo.isConnected) {
                Toast.makeText(this, "WiFi bağlantınızı kontrol edin", Toast.LENGTH_LONG).show()
            }

            // Bağlantı denemesini kullanıcı etkileşiminden sonra başlat
            binding.textConnectionStatus.setOnClickListener {
                lifecycleScope.launch {
                    viewModel.connectToIncubator()
                }
            }

            // Otomatik bağlantı denemesi
            lifecycleScope.launch {
                delay(1500) // 1.5 saniye bekle
                viewModel.connectToIncubator(5) // 5 deneme yap
            }
        } else {
            // Demo modu etkin uyarısı görünür yap
            binding.textDemoMode.visibility = View.VISIBLE

            // Demo modu ayarını kesin olarak yap
            if (demoActivated && !isDemoMode) {
                prefs.edit().putBoolean("DemoMode", true).apply()
            }

            // Demo modunda viewModel durumunu connected olarak ayarla
            viewModel.setDemoMode(true)

            // Log ekle
            Log.d("MainActivity", "Demo modu etkinleştirildi")
        }

        // Arkaplanda çalışan servisi başlat
        startIncubatorService(isDemoMode)

        // Bağlantı durumunu gözlemle
        viewModel.connectionStatus.observe(this) { status ->
            when (status) {
                ConnectionStatus.CONNECTING -> {
                    binding.textConnectionStatus.text = getString(R.string.connecting)
                    binding.textConnectionStatus.setTextColor(getColor(R.color.amber))
                    binding.textConnectionStatus.visibility = View.VISIBLE
                }
                ConnectionStatus.CONNECTED -> {
                    binding.textConnectionStatus.text = getString(R.string.connected)
                    binding.textConnectionStatus.setTextColor(getColor(R.color.green))
                    binding.textConnectionStatus.visibility = View.VISIBLE
                    // 3 saniye sonra gizle
                    binding.textConnectionStatus.postDelayed({
                        binding.textConnectionStatus.visibility = View.GONE
                    }, 3000)
                }
                ConnectionStatus.FAILED -> {
                    binding.textConnectionStatus.text = getString(R.string.connection_failed)
                    binding.textConnectionStatus.setTextColor(getColor(R.color.red))
                    binding.textConnectionStatus.visibility = View.VISIBLE

                    // Bağlantı hatası durumunda otomatik yeniden bağlanma denemesi yap
                    lifecycleScope.launch {
                        delay(3000) // 3 saniye bekle
                        viewModel.retryConnection()
                    }
                }
                else -> {
                    binding.textConnectionStatus.visibility = View.GONE
                }
            }
        }

        // Intent'ten bildirim türünü kontrol et ve ilgili sekmede aç
        val notificationType = intent.getStringExtra("notification_type")
        if (notificationType != null) {
            // Bildirimleri burada işleyin
            processNotification(notificationType)
        }
    }

    private fun processNotification(notificationType: String) {
        when (notificationType) {
            "TEMPERATURE" -> {
                // Sıcaklık uyarısı için dashboard'a git
                navController.navigate(R.id.dashboardFragment)
            }
            "HUMIDITY" -> {
                // Nem uyarısı için dashboard'a git
                navController.navigate(R.id.dashboardFragment)
            }
            "CONNECTION" -> {
                // Bağlantı uyarısı için ayarlar sayfasına git
                navController.navigate(R.id.settingsFragment)
            }
            "SENSOR_ERROR" -> {
                // Sensör hatası için dashboard'a git
                navController.navigate(R.id.dashboardFragment)
            }
        }
    }

    private fun startIncubatorService(isDemoMode: Boolean) {
        val serviceIntent = Intent(this, IncubatorMonitorService::class.java)

        // Demo mod bilgisini servis intentine ekle
        serviceIntent.putExtra("isDemoMode", isDemoMode)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun showReconnectionDialog() {
        val dialogFragment = ReconnectionDialogFragment.newInstance { retry ->
            if (retry) {
                viewModel.retryConnection()
            }
        }
        dialogFragment.show(supportFragmentManager, "reconnection_dialog")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Demo modunda ise, demo modundan çıkış seçeneğini göster
        val isDemoMode = getSharedPreferences("KuluckaSettings", MODE_PRIVATE).getBoolean("DemoMode", false)
        menu.findItem(R.id.action_exit_demo).isVisible = isDemoMode

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.retryConnection()
                true
            }
            R.id.action_exit_demo -> {
                exitDemoMode()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exitDemoMode() {
        // Demo modu ve ilk çalıştırma bayraklarını sıfırla
        val prefs = getSharedPreferences("KuluckaSettings", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("DemoMode", false)
            .putBoolean("FirstRun", true)
            .apply()

        // Servisi durdur
        stopService(Intent(this, IncubatorMonitorService::class.java))

        // Repository'yi sıfırla
        (application as IncubatorApplication).resetRepository()

        // Bağlantı ayarları ekranına git
        val intent = Intent(this, ConnectionSetupActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showAboutDialog() {
        val dialog = AboutDialogFragment()
        dialog.show(supportFragmentManager, "about_dialog")
    }

    override fun onDestroy() {
        // Temizlik işlemleri
        super.onDestroy()
    }
}