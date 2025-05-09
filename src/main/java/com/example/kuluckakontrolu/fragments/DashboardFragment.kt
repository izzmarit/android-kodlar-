package com.example.kuluckakontrolu.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.kuluckakontrolu.R
import com.example.kuluckakontrolu.databinding.FragmentDashboardBinding
import com.example.kuluckakontrolu.model.IncubatorData
import com.example.kuluckakontrolu.model.LoadingState
import com.example.kuluckakontrolu.viewmodel.IncubatorViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.kuluckakontrolu.dialogs.NewCycleDialogFragment
import com.example.kuluckakontrolu.model.IncubatorError
import com.example.kuluckakontrolu.utils.ProfileUtils

class DashboardFragment : Fragment() {
    private val TAG = "DashboardFragment"
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: IncubatorViewModel
    private var buttonRetry: Button? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[IncubatorViewModel::class.java]

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        // Ana veri durumunu gözlemle
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoadingState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.dashboardContent.visibility = View.GONE
                    binding.errorLayout.visibility = View.GONE
                }
                is LoadingState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.dashboardContent.visibility = View.VISIBLE
                    binding.errorLayout.visibility = View.GONE

                    try {
                        // Null kontrolü yap ve güvenli bir şekilde UI'ı güncelle
                        val data = state.data
                        if (data != null) {
                            updateDashboard(data)
                        } else {
                            Log.e(TAG, "Success durumunda veri null")
                            showError(IncubatorError.GENERIC_ERROR("Veri bulunamadı"))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Gösterge paneli güncellenirken hata: ${e.message}")
                        showError(IncubatorError.GENERIC_ERROR("Veri gösterilirken hata: ${e.message}"))
                    }
                }
                is LoadingState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.dashboardContent.visibility = View.GONE
                    binding.errorLayout.visibility = View.VISIBLE

                    // Hata mesajını göster
                    val errorMessage = getErrorMessage(state.error)
                    binding.textError.text = errorMessage

                    // Yeniden deneme butonu ekle
                    if (buttonRetry == null) {
                        val retryButton = Button(context).apply {
                            text = getString(R.string.retry)
                            setOnClickListener {
                                viewModel.retryConnection()
                            }
                        }
                        (binding.errorLayout as? ViewGroup)?.addView(retryButton)
                        buttonRetry = retryButton
                    }
                }
            }
        }

        // Aktif kuluçka döngüsünü gözlemle
        lifecycleScope.launch {
            viewModel.activeCycle.collect { cycle ->
                if (cycle != null) {
                    binding.cardIncubationStatus.visibility = View.VISIBLE
                    binding.textCycleName.text = cycle.name
                    binding.textAnimalType.text = cycle.animalType

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    binding.textStartDate.text = dateFormat.format(Date(cycle.startDate))

                    val currentDay = viewModel.dataState.value?.let {
                        if (it is LoadingState.Success) it.data.state.currentDay else 0
                    } ?: 0

                    val totalDays = getTotalDaysForAnimalType(cycle.animalType)
                    binding.textDayCounter.text = getString(R.string.day_format, currentDay, totalDays)
                    binding.progressDay.max = totalDays
                    binding.progressDay.progress = currentDay
                } else {
                    binding.cardIncubationStatus.visibility = View.GONE
                }
            }
        }

        // Sistem özetini gözlemle
        lifecycleScope.launch {
            viewModel.systemSummary.collect { summary ->
                try {
                    binding.textTotalCycles.text = summary.cycleCount.toString()
                    binding.textTotalRuntime.text = formatRuntime(summary.totalRuntime)
                    binding.textAverageTemp.text = String.format("%.1f°C", summary.avgTemperature)
                    binding.textAverageHumidity.text = String.format("%%%d", summary.avgHumidity)

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    binding.textLastUpdated.text = dateFormat.format(summary.lastUpdated)
                } catch (e: Exception) {
                    Log.e(TAG, "Sistem özeti güncellenirken hata: ${e.message}")
                }
            }
        }

        // OTA güncelleme durumunu gözlemle
        lifecycleScope.launch {
            viewModel.otaUpdateStatus.collect { status ->
                try {
                    if (status.isUpdateAvailable) {
                        binding.textUpdateStatus.text = getString(
                            R.string.update_available,
                            status.currentVersion,
                            status.availableVersion
                        )
                        binding.textUpdateStatus.visibility = View.VISIBLE
                        binding.buttonCheckUpdate.text = getString(R.string.update_now)
                        binding.buttonCheckUpdate.isEnabled = true
                        binding.buttonCheckUpdate.setOnClickListener {
                            startUpdate()
                        }
                    } else if (status.errorMessage.isNotEmpty()) {
                        binding.textUpdateStatus.text = status.errorMessage
                        binding.textUpdateStatus.visibility = View.VISIBLE
                        binding.buttonCheckUpdate.isEnabled = true
                    } else if (status.isUpdating) {
                        binding.updateProgressBar.visibility = View.VISIBLE
                        binding.updateProgressBar.progress = status.updateProgress
                        binding.textUpdateStatus.text = getString(R.string.updating_progress, status.updateProgress)
                        binding.textUpdateStatus.visibility = View.VISIBLE
                        binding.buttonCheckUpdate.isEnabled = false
                    } else if (status.updateProgress >= 100) {
                        binding.updateProgressBar.visibility = View.GONE
                        binding.textUpdateStatus.text = getString(R.string.update_complete)
                        binding.textUpdateStatus.visibility = View.VISIBLE
                        binding.buttonCheckUpdate.text = getString(R.string.check_updates)
                        binding.buttonCheckUpdate.isEnabled = true
                    } else {
                        binding.updateProgressBar.visibility = View.GONE
                        binding.textUpdateStatus.visibility = View.GONE
                        binding.buttonCheckUpdate.text = getString(R.string.check_updates)
                        binding.buttonCheckUpdate.isEnabled = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "OTA güncelleme durumu işlenirken hata: ${e.message}")
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonCheckUpdate.setOnClickListener {
            checkForUpdates()
        }

        binding.buttonStartCycle.setOnClickListener {
            // Yeni kuluçka döngüsü başlatma diyaloğunu göster
            try {
                NewCycleDialogFragment().show(parentFragmentManager, "new_cycle_dialog")
            } catch (e: Exception) {
                Log.e(TAG, "Yeni döngü diyaloğu gösterilirken hata: ${e.message}")
                Snackbar.make(
                    binding.root,
                    "Yeni döngü başlatılamadı: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateDashboard(data: IncubatorData) {
        try {
            // Sıcaklık kartı
            with(binding) {
                // NULL kontrolü ile güvenli atama
                textCurrentTemp.text = String.format("%.1f°C", data.state.currentTemp)
                textTargetTemp.text = getString(R.string.target_format, String.format("%.1f°C", data.settings.targetTemp))
                textHeaterStatus.text = getString(
                    R.string.heater_status,
                    if (data.state.heaterStatus) getString(R.string.active) else getString(R.string.inactive)
                )

                // İleri seviye sıcaklık bilgileri - sensör değerleri için null kontrolü
                textSensor1Temp.text = String.format("%.1f°C", data.state.temp1)
                textSensor2Temp.text = String.format("%.1f°C", data.state.temp2)
                textPidOutput.text = String.format("%.1f", data.state.pidOutput)

                // Nem kartı - null güvenlik kontrolü
                textCurrentHumidity.text = getString(R.string.humidity_format, data.state.currentHumidity)
                textTargetHumidity.text = getString(R.string.target_format,
                    getString(R.string.humidity_format, data.settings.targetHumidity))

                // Motor durumu - ESP32 kodlarıyla uyumlu olmalı
                val motorStatus = if (data.state.motorStatus) getString(R.string.running) else getString(R.string.waiting)
                textMotorStatus.text = getString(R.string.motor_status, motorStatus)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Dashboard güncelleme hatası: ${e.message}")
            Snackbar.make(binding.root, getString(R.string.generic_error, e.message), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showError(error: IncubatorError) {
        binding.progressBar.visibility = View.GONE
        binding.dashboardContent.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
        binding.textError.text = getErrorMessage(error)
    }

    private fun checkForUpdates() {
        try {
            binding.buttonCheckUpdate.isEnabled = false
            binding.textUpdateStatus.text = getString(R.string.checking_updates)
            binding.textUpdateStatus.visibility = View.VISIBLE

            viewModel.checkForFirmwareUpdates()
        } catch (e: Exception) {
            Log.e(TAG, "Güncelleme kontrolü başlatılamadı: ${e.message}")
            binding.textUpdateStatus.text = "Güncelleme kontrolü başlatılamadı: ${e.message}"
            binding.buttonCheckUpdate.isEnabled = true
        }
    }

    private fun startUpdate() {
        try {
            viewModel.startFirmwareUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Güncelleme başlatılamadı: ${e.message}")
            binding.textUpdateStatus.text = "Güncelleme başlatılamadı: ${e.message}"
            binding.buttonCheckUpdate.isEnabled = true
        }
    }

    private fun getErrorMessage(error: IncubatorError): String {
        return when(error) {
            is IncubatorError.CONNECTION_ERROR -> getString(R.string.connection_error_text)
            is IncubatorError.DATA_PARSING_ERROR -> getString(R.string.data_parsing_error)
            is IncubatorError.TIMEOUT_ERROR -> getString(R.string.timeout_error)
            is IncubatorError.GENERIC_ERROR -> getString(R.string.generic_error, error.message)
            else -> getString(R.string.generic_error, "Bilinmeyen hata")
        }
    }

    private fun formatRuntime(milliseconds: Long): String {
        val days = milliseconds / (24 * 60 * 60 * 1000)
        val hours = (milliseconds % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)

        return if (days > 0) {
            getString(R.string.days_hours_format, days, hours)
        } else {
            getString(R.string.hours_format, hours)
        }
    }

    private fun getTotalDaysForAnimalType(animalType: String): Int {
        return when (animalType) {
            "TAVUK" -> 21
            "ÖRDEK" -> 28
            "BILDIRCIN" -> 17
            "KAZ" -> 30
            "HİNDİ" -> 28
            "SÜLÜN" -> 25
            "GÜVERCİN" -> 18
            "KEKLİK" -> 24
            else -> 21
        }
    }
}