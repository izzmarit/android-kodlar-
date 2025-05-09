package com.example.kuluckakontrolu.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.kuluckakontrolu.R
import com.example.kuluckakontrolu.databinding.DialogFinishCycleBinding
import com.example.kuluckakontrolu.viewmodel.IncubatorViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FinishCycleDialogFragment : DialogFragment() {
    private var _binding: DialogFinishCycleBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: IncubatorViewModel
    private var totalEggs = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFinishCycleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(IncubatorViewModel::class.java)

        // Dialog başlığı ayarla
        dialog?.setTitle(R.string.finish_incubation_cycle)

        // Aktif döngü bilgilerini yükle
        lifecycleScope.launch {
            viewModel.activeCycle.collect { cycle ->
                if (cycle != null) {
                    binding.textCycleName.text = cycle.name
                    binding.textAnimalType.text = cycle.animalType
                    totalEggs = cycle.totalEggs
                    binding.textTotalEggs.text = getString(R.string.total_eggs, totalEggs)

                    // Slider'ı ayarla
                    binding.sliderHatchedEggs.valueTo = totalEggs.toFloat()
                    binding.sliderHatchedEggs.value = (totalEggs * 0.75f) // Varsayılan %75 başarı
                    updateSuccessRate(binding.sliderHatchedEggs.value.toInt())

                    binding.sliderHatchedEggs.addOnChangeListener { _, value, _ ->
                        val hatchedEggs = value.toInt()
                        updateSuccessRate(hatchedEggs)
                    }
                } else {
                    dismiss() // Aktif döngü yoksa diyaloğu kapat
                }
            }
        }

        // İptal butonu
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        // Tamamla butonu
        binding.buttonFinish.setOnClickListener {
            val hatchedEggs = binding.sliderHatchedEggs.value.toInt()
            viewModel.finishCurrentCycle(hatchedEggs)
            dismiss()
        }
    }

    private fun updateSuccessRate(hatchedEggs: Int) {
        binding.textHatchedEggs.text = hatchedEggs.toString()

        val successRate = if (totalEggs > 0) {
            (hatchedEggs * 100) / totalEggs
        } else {
            0
        }

        binding.textSuccessRate.text = getString(R.string.success_rate, successRate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}