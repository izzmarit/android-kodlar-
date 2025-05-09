package com.example.kuluckakontrolu.fragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.kuluckakontrolu.ConnectionSetupActivity
import com.example.kuluckakontrolu.R
import com.example.kuluckakontrolu.databinding.FragmentSettingsBinding
import com.example.kuluckakontrolu.model.IncubatorSettings
import com.example.kuluckakontrolu.model.LoadingState
import com.example.kuluckakontrolu.model.TimeRange
import com.example.kuluckakontrolu.utils.SecurityUtils
import com.example.kuluckakontrolu.viewmodel.IncubatorViewModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import android.util.Log
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: IncubatorViewModel

    private var targetTemp = 37.5
    private var targetHumidity = 60
    private var motorInterval = 120
    private var pidKp = 10.0
    private var pidKi = 0.1
    private var pidKd = 50.0

    private val selectBackupFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            handleSelectedBackupFile(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(IncubatorViewModel::class.java)

        // Mevcut ayarları yükle
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            if (state is LoadingState.Success) {
                updateSettingsUI(state.data.settings)
            }
        }

        setupControlButtons()

        binding.buttonSaveSettings.setOnClickListener {
            saveSettings()
        }

        binding.buttonBackup.setOnClickListener {
            createBackup()
        }

        binding.buttonRestore.setOnClickListener {
            selectBackupFile.launch("application/zip")
        }

        binding.buttonExportData.setOnClickListener {
            exportData()
        }

        binding.buttonWifiSettings.setOnClickListener {
            navigateToWifiSettings()
        }

        binding.switchAdvancedSettings.setOnCheckedChangeListener { _, isChecked ->
            binding.advancedSettingsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Gelişmiş ayarlar başlangıçta gizli
        binding.advancedSettingsLayout.visibility = View.GONE
    }

    private fun setupControlButtons() {
        with(binding) {
            // Sıcaklık ayarı butonları
            buttonTempMinus.setOnClickListener {
                if (targetTemp > 30.0) {
                    targetTemp -= 0.1
                    textViewTemp.text = String.format("%.1f", targetTemp)
                }
            }

            buttonTempPlus.setOnClickListener {
                if (targetTemp < 40.0) {
                    targetTemp += 0.1
                    textViewTemp.text = String.format("%.1f", targetTemp)
                }
            }

            // Nem ayarı butonları
            buttonHumidityMinus.setOnClickListener {
                if (targetHumidity > 40) {
                    targetHumidity -= 1
                    textViewHumidity.text = targetHumidity.toString()
                }
            }

            buttonHumidityPlus.setOnClickListener {
                if (targetHumidity < 80) {
                    targetHumidity += 1
                    textViewHumidity.text = targetHumidity.toString()
                }
            }

            // Motor ayarı butonları
            buttonMotorMinus.setOnClickListener {
                if (motorInterval > 30) {
                    motorInterval -= 15
                    textViewMotor.text = motorInterval.toString()
                }
            }

            buttonMotorPlus.setOnClickListener {
                if (motorInterval < 240) {
                    motorInterval += 15
                    textViewMotor.text = motorInterval.toString()
                }
            }

            // PID Kp ayarı
            buttonKpMinus.setOnClickListener {
                if (pidKp > 1.0) {
                    pidKp -= 0.5
                    textViewKp.text = String.format("%.1f", pidKp)
                }
            }

            buttonKpPlus.setOnClickListener {
                if (pidKp < 20.0) {
                    pidKp += 0.5
                    textViewKp.text = String.format("%.1f", pidKp)
                }
            }

            // PID Ki ayarı
            buttonKiMinus.setOnClickListener {
                if (pidKi > 0.01) {
                    pidKi -= 0.01
                    textViewKi.text = String.format("%.2f", pidKi)
                }
            }

            buttonKiPlus.setOnClickListener {
                if (pidKi < 1.0) {
                    pidKi += 0.01
                    textViewKi.text = String.format("%.2f", pidKi)
                }
            }

            // PID Kd ayarı
            buttonKdMinus.setOnClickListener {
                if (pidKd > 1.0) {
                    pidKd -= 5.0
                    textViewKd.text = String.format("%.1f", pidKd)
                }
            }

            buttonKdPlus.setOnClickListener {
                if (pidKd < 100.0) {
                    pidKd += 5.0
                    textViewKd.text = String.format("%.1f", pidKd)
                }
            }
        }
    }

    private fun updateSettingsUI(settings: IncubatorSettings) {
        targetTemp = settings.targetTemp
        targetHumidity = settings.targetHumidity
        motorInterval = settings.motorInterval
        pidKp = settings.pidKp
        pidKi = settings.pidKi
        pidKd = settings.pidKd

        with(binding) {
            textViewTemp.text = String.format("%.1f", targetTemp)
            textViewHumidity.text = targetHumidity.toString()
            textViewMotor.text = motorInterval.toString()
            textViewKp.text = String.format("%.1f", pidKp)
            textViewKi.text = String.format("%.2f", pidKi)
            textViewKd.text = String.format("%.1f", pidKd)
        }
    }

    private fun saveSettings() {
        // Şifre koruması - önce doğrulama iste
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.password_required)
            .setMessage(R.string.settings_password_prompt)
            .setView(R.layout.dialog_password)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                val passwordEditText = (dialog as AlertDialog).findViewById<TextInputEditText>(R.id.editTextPassword)
                val password = passwordEditText?.text?.toString() ?: ""

                if (SecurityUtils.validatePassword(requireContext(), password)) {
                    // Şifre doğru, ayarları güncelle
                    updateSettings()
                } else {
                    // Şifre yanlış
                    Toast.makeText(context, R.string.wrong_password, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.show()
    }

    private fun updateSettings() {
        try {
            // Mevcut ayarları al
            val currentSettings = (viewModel.dataState.value as? LoadingState.Success)?.data?.settings
            val currentSettingsState = (viewModel.dataState.value as? LoadingState.Success)?.data?.state
            val currentAnimalType = currentSettings?.animalType ?: "TAVUK"
            val currentDay = currentSettingsState?.currentDay ?: 0

            // Yeni değerleri güvenli bir şekilde oluştur
            val settings = IncubatorSettings(
                targetTemp = targetTemp.coerceIn(30.0, 40.0),
                targetHumidity = targetHumidity.coerceIn(40, 80),
                motorInterval = motorInterval.coerceIn(30, 240),
                animalType = currentAnimalType,
                currentDay = currentDay,
                pidKp = pidKp.coerceIn(1.0, 20.0),
                pidKi = pidKi.coerceIn(0.01, 1.0),
                pidKd = pidKd.coerceIn(1.0, 100.0)
            )

            viewModel.updateSettings(settings)

            // Başarı mesajı göster
            Snackbar.make(
                binding.root,
                R.string.settings_saved,
                Snackbar.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Ayarlar güncellenirken hata: ${e.message}")

            // Hata mesajı göster
            Snackbar.make(
                binding.root,
                getString(R.string.invalid_settings) + ": " + e.localizedMessage,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun createBackup() {
        binding.progressBackup.visibility = View.VISIBLE
        binding.buttonBackup.isEnabled = false

        viewModel.backupData().observe(viewLifecycleOwner) { file ->
            binding.progressBackup.visibility = View.GONE
            binding.buttonBackup.isEnabled = true

            if (file != null) {
                // Yedek dosyasını paylaş
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri as Uri)
                    type = "application/zip"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Yedeği Paylaş"))
            } else {
                Toast.makeText(context, R.string.backup_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSelectedBackupFile(uri: Uri) {
        try {
            // URI'dan geçici dosyaya kopyala
            val tempFile = File(requireContext().cacheDir, "temp_backup_${System.currentTimeMillis()}.zip")

            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                // Geri yükleme işlemini başlat
                binding.progressRestore.visibility = View.VISIBLE
                binding.buttonRestore.isEnabled = false

                viewModel.restoreData(tempFile).observe(viewLifecycleOwner) { success ->
                    binding.progressRestore.visibility = View.GONE
                    binding.buttonRestore.isEnabled = true

                    if (success) {
                        Toast.makeText(context, R.string.restore_success, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
                    }

                    // Geçici dosyayı sil
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportData() {
        val timeRanges = arrayOf(
            getString(R.string.last_day),
            getString(R.string.last_week),
            getString(R.string.full_cycle)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_time_range)
            .setItems(timeRanges) { _, which ->
                val selectedRange = when(which) {
                    0 -> TimeRange.LAST_DAY
                    1 -> TimeRange.LAST_WEEK
                    else -> TimeRange.FULL_CYCLE
                }

                exportDataWithTimeRange(selectedRange)
            }
            .show()
    }

    private fun exportDataWithTimeRange(timeRange: TimeRange) {
        binding.progressExport.visibility = View.VISIBLE
        binding.buttonExportData.isEnabled = false

        viewModel.exportDataToCsv(timeRange).observe(viewLifecycleOwner) { file ->
            binding.progressExport.visibility = View.GONE
            binding.buttonExportData.isEnabled = true

            if (file != null) {
                // CSV dosyasını paylaş
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri as Uri)
                    type = "text/csv"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Veriyi Paylaş"))
            } else {
                Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToWifiSettings() {
        // Şifre koruması - önce doğrulama iste
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.password_required)
            .setMessage(R.string.wifi_password_prompt)
            .setView(R.layout.dialog_password)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                val passwordEditText = (dialog as AlertDialog).findViewById<TextInputEditText>(R.id.editTextPassword)
                val password = passwordEditText?.text?.toString() ?: ""

                if (SecurityUtils.validatePassword(requireContext(), password)) {
                    // Şifre doğru, WiFi ayarları ekranına git
                    val intent = Intent(requireActivity(), ConnectionSetupActivity::class.java)
                    startActivity(intent)
                } else {
                    // Şifre yanlış
                    Toast.makeText(context, R.string.wrong_password, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}